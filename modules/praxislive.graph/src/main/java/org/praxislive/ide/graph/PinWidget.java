/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Linking this work statically or dynamically with other modules is making a
 * combined work based on this work. Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this work give you permission
 * to link this work with independent modules to produce an executable,
 * regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that
 * you also meet, for each linked independent module, the terms and conditions of
 * the license of that module. An independent module is a module which is not
 * derived from or based on this work. If you modify this work, you may extend
 * this exception to your version of the work, but you are not obligated to do so.
 * If you do not wish to do so, delete this exception statement from your version.
 *
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 *
 *
 * This class is derived from code in NetBeans Visual Library.
 * Original copyright notice follows.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.praxislive.ide.graph;

import java.awt.Point;
import java.awt.Rectangle;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Widget;

import org.netbeans.api.visual.anchor.Anchor;
import org.netbeans.api.visual.widget.Scene;

/**
 * This class represents a pin widget in the VMD visualization style.
 * The pin widget consists of a name and a glyph set.
 *
 * @author David Kaspar
 */
public class PinWidget extends Widget {

    public final static String DEFAULT_CATEGORY = "";
    
    private final PraxisGraphScene scene;
    private final SceneListenerImpl sceneListener;
    private final NodeWidget node;
    private final LabelWidget nameWidget;
    
    private LAFScheme scheme;
    private LAFScheme.Colors schemeColors;
    private Alignment alignment;
    private String category;

    /**
     * Creates a pin widget with a specific name.
     * @param scene the scene
     * @param name pin ID
     */
    public PinWidget(PraxisGraphScene scene, NodeWidget node, String name) {
        super(scene);
        this.scene = scene;
        this.sceneListener = new SceneListenerImpl();
        this.node = node;
        this.scheme = scene.getLookAndFeel();
        this.schemeColors = node.getSchemeColors();
        this.alignment = Alignment.Center;
        this.category = DEFAULT_CATEGORY;
//        setLayout(LayoutFactory.createOverlayLayout());
        setLayout(LayoutFactory.createVerticalFlowLayout());
        addChild(nameWidget = new LabelWidget(scene));
//        nameWidget.setForeground(Color.BLACK);
        nameWidget.setLabel(name);
        nameWidget.setAlignment(LabelWidget.Alignment.CENTER);
        scheme.installUI(this);
    }

    /**
     * Called to notify about the change of the widget state.
     * @param previousState the previous state
     * @param state the new state
     */
    protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
        scheme.updateUI(this);
    }

    @Override
    protected void notifyAdded() {
        scene.addSceneListener(sceneListener);
    }

    @Override
    protected void notifyRemoved() {
        scene.removeSceneListener(sceneListener);
    }
    
    /**
     * Returns a pin name widget.
     * @return the pin name widget
     */
    public Widget getPinNameWidget () {
        return nameWidget;
    }

    /**
     * Returns a pin name.
     * @return the pin name
     */
    public String getName() {
        return nameWidget.getLabel();
    }

    public void setSchemeColors(LAFScheme.Colors colors) {
        this.schemeColors = colors;
        revalidate();
    }
    
    public LAFScheme.Colors getSchemeColors() {
        return schemeColors == null ? node.getSchemeColors() : schemeColors;
    }

    public void setAlignment(Alignment alignment) {
        if (alignment == null) {
            throw new NullPointerException();
        }
        nameWidget.setAlignment(getLabelAlignment(alignment));
        this.alignment = alignment;
        scheme.updateUI(this);
    }

    public Alignment getAlignment() {
        return this.alignment;
    }

    private LabelWidget.Alignment getLabelAlignment(Alignment alignment) {
        switch (alignment) {
            case Left:
                return LabelWidget.Alignment.LEFT;
            case Right:
                return LabelWidget.Alignment.RIGHT;
            default:
                return LabelWidget.Alignment.CENTER;
        }
    }

    public Anchor createAnchor() {
        return new AlignedAnchor();
    }

    @Override
    public boolean isHitAt(Point localLocation) {
        if (scene.isBelowLODThreshold()) {
            return false;
        } else {
            return super.isHitAt(localLocation);
        }   
    }

    private class AlignedAnchor extends Anchor {

        private AlignedAnchor() {
            super(PinWidget.this);
        }

        @Override
        public Result compute(Entry entry) {
            Rectangle bounds = convertLocalToScene(getBounds());
            int centerY = bounds.y + bounds.height / 2;
            int gap = scheme.getAnchorGap();
            boolean right;
            switch (alignment) {
                case Left:
                    right = false;
                    break;
                case Right:
                    right = true;
                    break;
                default:
                    Point opposite = getOppositeSceneLocation(entry);
                    if (opposite.x > bounds.x) {
                        right = true;
                    } else {
                        right = false;
                    }

            }
            if (right) {
                return new Anchor.Result(
                        new Point(bounds.x + bounds.width + gap, centerY),
                        Direction.RIGHT);
            } else {
                return new Anchor.Result(
                        new Point(bounds.x - gap, centerY),
                        Direction.LEFT);
            }
        }
    }
    
    private class SceneListenerImpl implements Scene.SceneListener {

        @Override
        public void sceneRepaint() {
        }

        @Override
        public void sceneValidating() {
            scheme.updateUI(PinWidget.this);
        }

        @Override
        public void sceneValidated() {
        }
        
    }
    
}
