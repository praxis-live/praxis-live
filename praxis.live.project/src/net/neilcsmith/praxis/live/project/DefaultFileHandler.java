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
import net.neilcsmith.praxis.live.project.api.FileHandler;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class DefaultFileHandler extends FileHandler {

    private PraxisProject project;
    private FileObject file;

    DefaultFileHandler(PraxisProject project, FileObject file) {
        this.project = project;
        this.file = file;
    }

    @Override
    public void process(Callback callback) throws Exception {
        String script = file.asText();
        script = "set _PWD " + project.getProjectDirectory().getURL().toURI() + "\n" + script;
//        ProjectHelper.executeScript(script, new CallbackWrapper(callback));]
        ProjectHelper.getDefault().executeScript(script, callback);
    }

//    private class CallbackWrapper implements net.neilcsmith.praxis.live.core.api.Callback {
//
//        private Callback callback;
//
//        private CallbackWrapper(Callback callback) {
//            this.callback = callback;
//
//        }
//
//        @Override
//        public void onReturn(CallArguments args) {
//            callback.onSuccess();
//        }
//
//        @Override
//        public void onError(CallArguments args) {
//            callback.onFailure();
//        }
//    }
}
