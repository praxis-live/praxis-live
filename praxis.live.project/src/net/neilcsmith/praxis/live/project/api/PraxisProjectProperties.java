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

package net.neilcsmith.praxis.live.project.api;

import java.beans.PropertyChangeListener;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public abstract class PraxisProjectProperties {
    
    public final static String PROP_FILES_CHANGED = "filesChanged";


    public abstract FileObject[] getProjectFiles(ExecutionLevel level);

    public abstract void setProjectFiles(ExecutionLevel level, FileObject[] files);

    public abstract void addPropertyChangeListener(PropertyChangeListener listener);

    public abstract void removePropertyChangeListener(PropertyChangeListener listener);

}
