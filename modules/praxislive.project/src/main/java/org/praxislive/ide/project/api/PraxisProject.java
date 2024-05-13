/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.project.api;

import org.netbeans.api.project.Project;

/**
 * Subtype of Project for all PraxisCORE projects.
 */
public interface PraxisProject extends Project {

    /**
     * The project type.
     */
    public final static String TYPE = "org-praxislive-ide-project";

    /**
     * The layer path for the project type.
     */
    public final static String PROJECT_LAYER_PATH = "Projects/" + TYPE;

    /**
     * The lookup path within the layer for registering features.
     */
    public final static String LOOKUP_PATH = PROJECT_LAYER_PATH + "/Lookup";

}
