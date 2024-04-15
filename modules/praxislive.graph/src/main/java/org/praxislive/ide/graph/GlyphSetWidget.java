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
 *
 *
 * This file incorporates code from Apache NetBeans Visual Library, covered by
 * the following terms :
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.praxislive.ide.graph;

import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.ImageWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This widget represents a list of glyphs rendered horizontally one after another. A glyph is a small image - usually 16x16px.
 */
public class GlyphSetWidget extends Widget {

    /**
     * Creates a glyph set widget.
     * @param scene the scene
     */
    public GlyphSetWidget (Scene scene) {
        super (scene);
        setLayout (LayoutFactory.createHorizontalFlowLayout ());
    }

    /**
     * Sets glyphs as a list of images.
     * @param glyphs the list of images used as glyphs
     */
    public void setGlyphs (List<Image> glyphs) {
        List<Widget> children = new ArrayList<> (getChildren ());
        for (Widget widget : children)
            removeChild (widget);
        if (glyphs != null)
            for (Image glyph : glyphs) {
                ImageWidget imageWidget = new ImageWidget (getScene ());
                imageWidget.setImage (glyph);
                addChild (imageWidget);
            }
    }

}
