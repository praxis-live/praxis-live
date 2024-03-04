/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2020 Neil C Smith.
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

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.spi.FileHandler;
import org.praxislive.ide.project.api.PraxisProject;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;
import org.praxislive.core.types.PError;
import org.praxislive.ide.project.api.ExecutionElement;

/**
 *
 */
public class AutostartFileHandler implements FileHandler {
    
    private final static Logger LOG = Logger.getLogger(AutostartFileHandler.class.getName());
    
    private final static String AUTOSTART_SUFFIX = "_autostart";

    private final static RequestProcessor RP = new RequestProcessor();
    private PraxisProject project;
    private FileObject source;
    private String rootID;

    public AutostartFileHandler(PraxisProject project, FileObject source, String rootID) {
        if (project == null || source == null) {
            throw new NullPointerException();
        }
        this.project = project;
        this.source = source;
        this.rootID = rootID;
    }

    @Override
    public void process(final Callback callback) throws Exception {
        if (callback == null) {
            throw new NullPointerException();
        }
        RP.execute(() -> {
            try {
                String script = source.asText().trim();
                String expected = "/" + rootID + ".start";
                if (!expected.equals(script)) {
                    LOG.log(Level.WARNING, "Unexpected contents in Autostart file\nFile : {0}\nContents : {1}",
                            new Object[]{source.toURI(), script});
                    // @TODO mark file for fixing somehow?
                }
                SwingUtilities.invokeLater(() -> {
                    // @TODO check root file is owned by project?
                    PXRRootProxy root = project.getLookup().lookup(PXRRootRegistry.class).getRootByID(rootID);
                    Project owner = root != null ? FileOwnerQuery.getOwner(root.getSourceFile()) : null;
                    if (root != null && owner != null &&
                            project.getProjectDirectory().equals(owner.getProjectDirectory())) {
                        try {
                            root.send("start", List.of(), callback);
                            return;
                        } catch (Exception ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                    callback.onError(List.of());
                });
                
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
                SwingUtilities.invokeLater(new Runnable() {
                    
                    @Override
                    public void run() {
                        callback.onError(List.of(PError.of(ex)));
                    }
                });
            }
        });
    }



    @ServiceProvider(service=FileHandler.Provider.class)
    public static class Provider implements FileHandler.Provider {

        @Override
        public Optional<FileHandler> createHandler(PraxisProject project,
                ExecutionLevel level, ExecutionElement.File element) {
            var file = element.file();
            var name = file.getName();
            if (name.endsWith(AUTOSTART_SUFFIX)) {
                FileObject parent = file.getParent();
                if ("config".equals(parent.getName()) &&
                        project.getProjectDirectory().equals(parent.getParent())) {
                    String rootID = name.substring(0, name.lastIndexOf(AUTOSTART_SUFFIX));
                    return Optional.of(new AutostartFileHandler(project, file, rootID));
                }
            }
            return Optional.empty();

        }
    }
}
