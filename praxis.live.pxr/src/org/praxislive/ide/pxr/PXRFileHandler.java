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
package org.praxislive.ide.pxr;

import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.netbeans.api.actions.Openable;
import org.praxislive.core.CallArguments;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.spi.FileHandler;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.model.RootProxy;
import org.praxislive.ide.pxr.api.RootRegistry;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PXRFileHandler extends FileHandler {

    private static final RequestProcessor RP = new RequestProcessor();
    private PraxisProject project;
    private PXRDataObject source;
    private Callback callback;
    private List<String> warnings;

    public PXRFileHandler(PraxisProject project, PXRDataObject source) {
        if (project == null || source == null) {
            throw new NullPointerException();
        }
        this.project = project;
        this.source = source;
        warnings = new ArrayList<>();
    }

    @Override
    public void process(final Callback callback) throws Exception {
        if (callback == null) {
            throw new NullPointerException();
        }
        this.callback = callback;
        
        RootProxy root = RootRegistry.getDefault().findRootForFile(source.getPrimaryFile());
        if (root != null) {
            callback.onReturn(CallArguments.EMPTY);
            return;
        }
             
        RP.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    String script = source.getPrimaryFile().asText();
                    final PXRParser.RootElement root = PXRParser.parse(script);
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            build(root);
                        }
                    });
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            callback.onError(CallArguments.EMPTY);
                        }
                    });
                }
            }
        });
    }

    @Override
    public List<String> getWarnings() {
        return warnings;
    }
    

    private void build(PXRParser.RootElement root) {
        PXRBuilder builder = new PXRBuilder(project, source, root, warnings);
        builder.process(new Callback() {

            @Override
            public void onReturn(CallArguments args) {
                Openable open = source.getLookup().lookup(Openable.class);
                if (open != null) {
                    open.open();
                }
                callback.onReturn(args);
            }

            @Override
            public void onError(CallArguments args) {
                callback.onError(args);
            
            }
        });
    }

    @ServiceProvider(service = FileHandler.Provider.class)
    public static class Provider implements FileHandler.Provider {

        @Override
        public FileHandler createHandler(PraxisProject project, ExecutionLevel level, FileObject file) {
            if (level == ExecutionLevel.BUILD && file.hasExt("pxr")) {
                try {
                    DataObject dob = DataObject.find(file);
                    if (dob instanceof PXRDataObject) {
                        return new PXRFileHandler(project, (PXRDataObject) dob);
                    }
                } catch (DataObjectNotFoundException ex) {
                    // fall through
                }

            }
            return null;

        }
    }
}
