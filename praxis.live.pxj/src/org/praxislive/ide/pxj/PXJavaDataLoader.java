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
package org.praxislive.ide.pxj;

import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.ExtensionList;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.UniFileLoader;

/**
 *
 */
@DataObject.Registration(
        mimeType = "text/x-java",
        iconBase = "org/praxislive/ide/pxj/resources/pxj16.png",
        position = 0)
public class PXJavaDataLoader extends UniFileLoader {
    
    public PXJavaDataLoader() {
        super("org.praxislive.ide.pxj.PXJavaDataObject");
        ExtensionList exts = new ExtensionList();
        exts.addMimeType("text/x-java");
        setExtensions(exts);
    }

    @Override
    protected String actionsContext() {
        return "Loaders/text/x-java/Actions";
    }

    @Override
    protected FileObject findPrimaryFile(FileObject fo) {
        if (fo.getAttribute(PXJDataObject.PXJ_DOB_KEY) instanceof PXJDataObject) {
            return fo;
        } else {
            return null;
        }
    }
    
    

    @Override
    protected MultiDataObject createMultiObject(FileObject primaryFile) throws DataObjectExistsException, IOException {
        return new PXJavaDataObject(primaryFile, this);
    }
    
}
