/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package net.neilcsmith.praxis.live.pxr;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Set;
import javax.swing.SwingUtilities;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.pxr.api.ComponentProxy;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Neil C Smith
 */
public class ActionBridge {

    private final static RequestProcessor RP = new RequestProcessor(ActionBridge.class);

    private ActionBridge() {
        // non instantiable
    }

    public static void copyToClipboard(ContainerProxy container, Set<String> children) {
        StringBuilder sb = new StringBuilder();
        try {
            PXRWriter.writeSubGraph(container, children, sb);
            SubGraphTransferable tf = new SubGraphTransferable(sb.toString());
            getClipboard().setContents(tf, tf);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static boolean pasteFromClipboard(ContainerProxy container, Callback callback) {
        Clipboard c = getClipboard();
        if (c.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
            try {
                String script = (String) c.getData(DataFlavor.stringFlavor);
                PXRParser.RootElement fakeRoot = PXRParser.parseInContext(container.getAddress(), script);
                if (ImportRenameSupport.prepareForPaste(container, fakeRoot)) {
                    PXRBuilder builder = PXRBuilder.getBuilder(findRootProxy(container), fakeRoot);
                    builder.process(callback);
                    return true;
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return false;
    }

    public static boolean importSubgraph(final ContainerProxy container, final FileObject file, final Callback callback) {
        if (!file.hasExt("pxg")) {
            return false;
        }
        final ComponentAddress context = container.getAddress();
        RP.execute(new Runnable() {
            @Override
            public void run() {
                PXRParser.RootElement r = null;
                try {
                    r = PXRParser.parseInContext(context, file.asText());
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
                final PXRParser.RootElement root = r;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (root != null) {
                            if (ImportRenameSupport.prepareForImport(container, root)) {
                                PXRBuilder builder = PXRBuilder.getBuilder(findRootProxy(container), root);
                                builder.process(callback);
                            } else {
                                callback.onReturn(CallArguments.EMPTY);
                            }
                        } else {
                            callback.onError(CallArguments.EMPTY);
                        }
                    }
                });
            }
        });
        return true;
    }

    private static Clipboard getClipboard() {
        Clipboard c = Lookup.getDefault().lookup(Clipboard.class);
        if (c == null) {
            c = Toolkit.getDefaultToolkit().getSystemClipboard();
        }
        return c;
    }

    private static PXRRootProxy findRootProxy(ContainerProxy container) {
        while (container != null) {
            if (container instanceof PXRRootProxy) {
                return (PXRRootProxy) container;
            }
            container = container.getParent();
        }
        throw new IllegalStateException("No root proxy found");
    }

    private static class SubGraphTransferable implements Transferable, ClipboardOwner {

        private String data;

        private SubGraphTransferable(String data) {
            this.data = data;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{
                        DataFlavor.stringFlavor
                    };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            if (flavor.equals(DataFlavor.stringFlavor)) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (isDataFlavorSupported(flavor)) {
                return data;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            // no op
        }
    }
}
