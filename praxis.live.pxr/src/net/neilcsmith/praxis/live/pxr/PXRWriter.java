/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Neil C Smith.
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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.live.pxr.api.Connection;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import net.neilcsmith.praxis.live.pxr.api.PraxisPropertyEditor;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PXRWriter {

    private final static Logger LOG = Logger.getLogger(PXRWriter.class.getName());

    final static String INDENT = "  ";
    final static String AT = "@";
    final static String CONNECT = "~";
    
    private final static RequestProcessor RP = new RequestProcessor();
    private PXRDataObject dob;
    private PXRRootProxy root;

    private PXRWriter(PXRDataObject dob, PXRRootProxy root) {
        this.dob = dob;
        this.root = root;
    }

    private void doWrite() {
        final StringBuilder sb = new StringBuilder();
        writeComponent(sb, root, 0);
        RP.execute(new Runnable() {

            @Override
            public void run() {
                Writer writer = null;
                try {
                    FileObject file = dob.getPrimaryFile();
                    writer = new OutputStreamWriter(file.getOutputStream());
                    writer.append(sb);

                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException ex) {
                            //Exceptions.printStackTrace(ex);
                        }
                    }
                }
            }
        });
    }

    private void writeComponent(StringBuilder sb, PXRComponentProxy cmp, int level) {
        LOG.finest("Writing component " + cmp.getAddress());
        writeIndent(sb, level);
        sb.append(AT).append(' ');
        writeComponentID(sb, cmp);
        sb.append(' ').append(cmp.getType()).append(" {\n");
        writeAttributes(sb, cmp, level + 1);
        writeProperties(sb, cmp, level + 1);
        if (cmp instanceof PXRContainerProxy) {
            writeChildren(sb, (PXRContainerProxy) cmp, level + 1);
            writeConnections(sb, (PXRContainerProxy) cmp, level + 1);
        }
        writeIndent(sb, level);
        sb.append("}\n");
    }

    private void writeComponentID(StringBuilder sb, PXRComponentProxy cmp) {
        if (cmp == root) {
            sb.append(cmp.getAddress());
        } else {
            ComponentAddress ad = cmp.getAddress();
            String id = ad.getComponentID(ad.getDepth() - 1);
            sb.append("./");
            sb.append(id);
        }
    }

    private void writeAttributes(StringBuilder sb, PXRComponentProxy cmp, int level) {
        String[] keys = cmp.getAttributeKeys();
        for (String key : keys) {
            writeIndent(sb, level);
            writeAttribute(sb, key, cmp.getAttribute(key));
        }
    }

    private void writeAttribute(StringBuilder sb, String key, String value) {
        LOG.finest("Writing attribute " + key + " : " + value);
        sb.append("#%").append(key).append(' ');
        sb.append(SyntaxUtils.escape(value)).append('\n');
    }

    private void writeProperties(StringBuilder sb, PXRComponentProxy cmp, int level) {
        String[] propIDs = cmp.getPropertyIDs();
        for (String id : propIDs) {
            try {
                LOG.finest("Checking property " + id);
                PraxisProperty prop = cmp.getProperty(id);
                if (!prop.canWrite()) {
                    continue;
                }
                if (prop.supportsDefaultValue() && prop.isDefaultValue()) {
                    continue;
                }
                LOG.finest("Writing property " + id);
                PraxisPropertyEditor editor = prop.getPropertyEditor();
                String code = editor.getPraxisInitializationString();
                if (code == null || code.isEmpty()) {
                    LOG.finest("No code returned from editor for " + id);
                    continue;
                }
                writeIndent(sb, level);
                writeProperty(sb, id, code);
            } catch (Exception e) {
                // continue ???
                LOG.log(Level.INFO, "Error writing property.", e);
            }
        }
    }

    private void writeProperty(StringBuilder sb, String id, String code) {
        sb.append('.').append(id).append(' ').append(code).append('\n');
    }

    private void writeChildren(StringBuilder sb, PXRContainerProxy container, int level) {
        String[] childIDs = container.getChildIDs();
        for (String id : childIDs) {
            writeComponent(sb, container.getChild(id), level);
        }
    }

    private void writeConnections(StringBuilder sb, PXRContainerProxy container, int level) {
        Connection[] connections = container.getConnections();
        for (Connection connection : connections) {
//            ComponentAddress ad1 = connection.getPort1().getComponentAddress();
//            ComponentAddress ad2 = connection.getPort2().getComponentAddress();
//            String c1 = ad1.getComponentID(ad1.getDepth() - 1);
//            String c2 = ad2.getComponentID(ad2.getDepth() - 1);
//            String p1 = connection.getPort1().getID();
//            String p2 = connection.getPort2().getID();
//            writeIndent(sb, level);
//            writeConnection(sb, c1, p1, c2, p2);

            String c1 = connection.getChild1();
            String c2 = connection.getChild2();
            String p1 = connection.getPort1();
            String p2 = connection.getPort2();
            writeIndent(sb, level);
            writeConnection(sb, c1, p1, c2, p2);
        }
    }

    private void writeConnection(StringBuilder sb, String c1, String p1,
            String c2, String p2) {
        sb.append(CONNECT).append(' ');
        sb.append("./").append(c1).append('!').append(p1).append(' ');
        sb.append("./").append(c2).append('!').append(p2).append('\n');
    }

    private void writeIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
    }

    static void write(PXRDataObject dob, PXRRootProxy root) {
        new PXRWriter(dob, root).doWrite();
    }
}
