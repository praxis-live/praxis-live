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
package org.praxislive.ide.pxr.spi;

import java.util.function.UnaryOperator;
import org.praxislive.project.GraphModel;

/**
 * A set of interfaces by which root editors can participate in copying, pasting
 * or exporting by transforming the graph model before it is applied. Instances
 * of these interfaces should be added to the lookup of the root editor.
 */
public sealed interface ModelTransform extends UnaryOperator<GraphModel> {

    /**
     * Transform the graph model in a copy operation. The transform will be
     * applied when the model has been created from the serialization data,
     * before generating the script written to the clipboard.
     */
    public non-sealed interface Copy extends ModelTransform {

    }

    /**
     * Transform the graph model in a paste operation. The transform will be
     * applied to a model created from the clipboard data, and a script for
     * evaluation generated from the transformed model.
     */
    public non-sealed interface Paste extends ModelTransform {

    }

    /**
     * Transform the graph model in a subgraph export operation. The transform
     * will be applied when the model has been created from the serialization
     * data, before generating the script written to the file.
     */
    public non-sealed interface Export extends ModelTransform {

    }

    /**
     * Transform the graph model in a subgraph import operation. The transform
     * will be applied when the model has been created from the file data,
     * before generating the script to be evaluated.
     */
    public non-sealed interface Import extends ModelTransform {

    }

}
