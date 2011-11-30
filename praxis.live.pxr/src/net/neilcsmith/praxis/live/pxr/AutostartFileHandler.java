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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.project.api.ExecutionLevel;
import net.neilcsmith.praxis.live.project.api.FileHandler;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import net.neilcsmith.praxis.live.pxr.api.ProxyException;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import net.neilcsmith.praxis.live.pxr.api.RootRegistry;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
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
public class AutostartFileHandler extends FileHandler {
    
    private final static Logger LOG = Logger.getLogger(AutostartFileHandler.class.getName());
    
    private final static String AUTOSTART_SUFFIX = "_autostart";

    private final static RequestProcessor RP = new RequestProcessor();
    private PraxisProject project;
    private FileObject source;
    private String rootID;
    private Callback callback;

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
        this.callback = callback;
        RP.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    String script = source.asText().trim();
                    String expected = "/" + rootID + ".start";
                    if (!expected.equals(script)) {
                        LOG.log(Level.WARNING, "Unexpected contents in Autostart file\nFile : {0}\nContents : {1}",
                                new Object[]{source.getURL(), script});
                        // @TODO mark file for fixing somehow?
                    }
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            // @TODO check root file is owned by project?
                            RootProxy root = RootRegistry.getDefault().getRootByID(rootID);
                            Project owner = root != null ? FileOwnerQuery.getOwner(root.getSourceFile()) : null;
                            if (root != null && owner != null &&
                                    project.getProjectDirectory().equals(owner.getProjectDirectory())) {
                                try {
                                    root.call("start", CallArguments.EMPTY, new Callback() {

                                        @Override
                                        public void onReturn(CallArguments args) {
                                            callback.onReturn(CallArguments.EMPTY);
                                        }

                                        @Override
                                        public void onError(CallArguments args) {
                                            callback.onReturn(CallArguments.EMPTY);
                                        }
                                    });
                                    return;
                                } catch (ProxyException ex) {
                                    Exceptions.printStackTrace(ex);
                                }
                            }
                            callback.onError(CallArguments.EMPTY);
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



    @ServiceProvider(service=FileHandler.Provider.class)
    public static class Provider implements FileHandler.Provider {

        @Override
        public FileHandler getHandler(PraxisProject project, ExecutionLevel level, FileObject file) {
            String name = file.getName();
            if (name.endsWith(AUTOSTART_SUFFIX)) {
                FileObject parent = file.getParent();
                if ("config".equals(parent.getName()) &&
                        project.getProjectDirectory().equals(parent.getParent())) {
                    String rootID = name.substring(0, name.lastIndexOf(AUTOSTART_SUFFIX));
                    return new AutostartFileHandler(project, file, rootID);
                }
            }
            return null;

        }
    }
}
