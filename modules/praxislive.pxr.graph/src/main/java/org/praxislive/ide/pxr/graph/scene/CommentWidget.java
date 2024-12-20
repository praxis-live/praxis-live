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

import java.awt.Dimension;
import java.awt.Point;
import java.util.Objects;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.EditProvider;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Widget;

final class CommentWidget extends Widget {

    private final PraxisGraphScene<?> scene;

    private String text;
    private EditProvider editProvider;

    CommentWidget(PraxisGraphScene<?> scene) {

        super(scene);
        this.scene = scene;
        setOpaque(true);
        setBackground(scene.getBackground());
        setLayout(LayoutFactory.createVerticalFlowLayout());
        setMinimumSize(new Dimension(100, 10));
        setBorder(BorderFactory.createOpaqueBorder(4, 4, 4, 4));
        text = "";
        getActions().addAction(ActionFactory.createEditAction((widget) -> {
            if (editProvider != null) {
                editProvider.edit(widget);
            }
        }));
    }

    final void setText(String text) {
        if (this.text.equals(text)) {
            return;
        }
        this.text = Objects.requireNonNull(text);
        String[] lines = text.split("\n");
        removeChildren();
        for (String line : lines) {
            LabelWidget lw = new LabelWidget(getScene(), line);
            lw.setForeground(null);
            addChild(lw);
        }
    }

    final String getText() {
        return text;
    }

    final void setEditProvider(EditProvider editProvider) {
        this.editProvider = editProvider;
    }

    @Override
    protected void paintChildren() {
        if (scene.isBelowLODThreshold()) {
            return;
        }
        super.paintChildren();
    }

    @Override
    public boolean isHitAt(Point localLocation) {
        if (scene.isBelowLODThreshold()) {
            return false;
        } else {
            return super.isHitAt(localLocation);
        }
    }

}
