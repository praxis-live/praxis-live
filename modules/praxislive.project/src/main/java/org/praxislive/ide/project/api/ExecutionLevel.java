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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.project.api;

/**
 * The three main levels of project execution.
 */
public enum ExecutionLevel {

    /**
     * The configure level is for hub setup, process start and required
     * configuration.
     */
    CONFIGURE,
    /**
     * The build level is for building of user roots and components.
     */
    BUILD,
    /**
     * The run level is for starting user roots or executing commands related to
     * running the project. Lines and files in this level may be executed
     * multiple times.
     */
    RUN;
}
