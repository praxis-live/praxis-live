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

package net.neilcsmith.praxis.live.project;

import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.project.api.ExecutionLevel;
import net.neilcsmith.praxis.live.project.api.FileHandler;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import net.neilcsmith.praxis.live.project.api.PraxisProjectProperties;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class FileHandlerIterator implements Cancellable {
    
    private final static FileObject[] EMPTY_FILES = new FileObject[0];

    private FileObject[] buildFiles = EMPTY_FILES;
    private FileObject[] runFiles = EMPTY_FILES;
    private ProgressHandle progress = null;
    private int index = -1;
    private FileHandler.Provider[] handlers = new FileHandler.Provider[0];
    private PraxisProject project;
    private Callback callback;

    private FileHandlerIterator(PraxisProject project, boolean build, boolean run) {
        this.project = project;
        PraxisProjectProperties props = project.getLookup().lookup(PraxisProjectProperties.class);
        if (props != null) {
            if (build) {
                buildFiles = props.getProjectFiles(ExecutionLevel.BUILD);
            }
            if (run) {
                runFiles = props.getProjectFiles(ExecutionLevel.RUN);
            }
        }
        handlers = Lookup.getDefault().lookupAll(FileHandler.Provider.class).toArray(handlers);
//        callback = new Callback();
        callback = new CallbackImpl();
    }

    public void start() {
        int totalFiles = buildFiles.length + runFiles.length;
        if (totalFiles == 0) {
            return;
        }
        progress = ProgressHandleFactory.createHandle("Executing...", this);
        progress.start(totalFiles);
        next();
    }

    @Override
    public boolean cancel() {
        return false;
    }

    private void next() {
        index++;
        if (index >= (buildFiles.length + runFiles.length)) {
            done();
            return;
        }
        FileObject file;
        ExecutionLevel level;
        if (index < buildFiles.length) {
            file = buildFiles[index];
            level = ExecutionLevel.BUILD;
        } else {
            file = runFiles[index - buildFiles.length];
            level = ExecutionLevel.RUN;
        }
        FileHandler handler = findHandler(level, file);
        String msg = FileUtil.getRelativePath(project.getProjectDirectory(), file) + " [" + level + "]";
        progress.progress(msg, index);
        try {
            handler.process(callback);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            fail();
        }
    }

    private void fail() {
        progress.finish();
    }

    private void done() {
        progress.finish();
    }

    private FileHandler findHandler(ExecutionLevel level, FileObject file) {
        FileHandler handler = null;
        for (FileHandler.Provider provider : handlers) {
            try {
            handler = provider.getHandler(project, level, file);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
            if (handler != null) {
                break;
            }
        }
        if (handler == null) {
            handler = new DefaultFileHandler(project, file);
        }
        return handler;
    }

//    private class Callback implements FileHandler.Callback {
//
//        @Override
//        public void onSuccess() {
//            next();
//        }
//
//        @Override
//        public void onFailure() {
//            fail();
//        }
//
//    }

    private class CallbackImpl implements Callback {

        @Override
        public void onReturn(CallArguments args) {
            next();
        }

        @Override
        public void onError(CallArguments args) {
            fail();
        }

    }

    public static FileHandlerIterator createBuildIterator(PraxisProject project) {
        return new FileHandlerIterator(project, true, false);
    }

    public static FileHandlerIterator createRunIterator(PraxisProject project) {
        return new FileHandlerIterator(project, false, true);
    }

    public static FileHandlerIterator createBuildAndRunIterator(PraxisProject project) {
        return new FileHandlerIterator(project, true, true);
    }



}
