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
package org.praxislive.ide.pxr.graph.scene;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Objects;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Widget;

class CommentWidget extends Widget {

    private final PraxisGraphScene<?> scene;
    
    private String text;

    public CommentWidget(PraxisGraphScene<?> scene) {

        super(scene);
        this.scene = scene;
        
        setLayout(LayoutFactory.createVerticalFlowLayout());
        setMinimumSize(new Dimension(100, 10));
        text = "";
        
    }

    public final void setText(String text) {
        if (this.text.equals(text)) {
            return;
        }
        this.text = Objects.requireNonNull(text);
        String[] lines = text.split("\n");
        removeChildren();
        for (String line : lines) {
            LabelWidget lw = new LabelWidget(getScene(), line);
            lw.setOpaque(false);
            lw.setForeground(Color.BLACK); // how to set this?
            addChild(lw);
        }
    }

    public final String getText() {
        return text;
    }

    @Override
    protected void paintChildren() {
        if (scene.isBelowLODThreshold()) {
            return;
        }
        super.paintChildren();
    }

}
