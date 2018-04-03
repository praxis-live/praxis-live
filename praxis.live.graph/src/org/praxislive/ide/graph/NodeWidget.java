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
 * Please visit http://neilcsmith.net if you need additional information or
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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.model.StateModel;
import org.netbeans.api.visual.widget.ImageWidget;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 * This class represents a node widget.
 */
public class NodeWidget extends Widget implements StateModel.Listener, MinimizeAbility {

    private final Widget header;
    private final ImageWidget minimizeWidget;
    private final ImageWidget imageWidget;
    private final LabelWidget nameWidget;
    private final GlyphSetWidget glyphSetWidget;

    private final StateModel stateModel;
    private final LAFScheme scheme;
    private final PraxisGraphScene scene;
    private final SceneListenerImpl sceneListener;
    private final CommentWidget commentWidget;
    
    private LAFScheme.Colors schemeColors;

    /**
     * Creates a node widget with a specific color scheme.
     *
     * @param scene the scene
     */
    public NodeWidget(PraxisGraphScene<?> scene) {
        super(scene);
        this.scene = scene;
        this.sceneListener = new SceneListenerImpl();
        this.scheme = scene.getLookAndFeel();

        stateModel = new StateModel();

        setLayout(LayoutFactory.createVerticalFlowLayout());
        setMinimumSize(new Dimension(100, 10));

        header = new Widget(scene);
        header.setLayout(LayoutFactory.createHorizontalFlowLayout(LayoutFactory.SerialAlignment.CENTER, 8));
        addChild(header);

        minimizeWidget = new ImageWidget(scene, scheme.getMinimizeWidgetImage(this));
        minimizeWidget.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        minimizeWidget.getActions().addAction(new ToggleMinimizedAction());

        header.addChild(minimizeWidget);

        imageWidget = new ImageWidget(scene);
        header.addChild(imageWidget);

        nameWidget = new LabelWidget(scene);
        nameWidget.setFont(scene.getDefaultFont().deriveFont(Font.BOLD));
        nameWidget.setForeground(Color.BLACK);
        header.addChild(nameWidget);

        glyphSetWidget = new GlyphSetWidget(scene);
        glyphSetWidget.setMinimumSize(new Dimension(16, 16));
        header.addChild(glyphSetWidget);

        Widget topLayer = new Widget(scene);
        addChild(topLayer);

        stateModel.addListener(this);

        commentWidget = new CommentWidget(scene);
//        commentWidget.setVisible(false);

        scheme.installUI(this);

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
     * Called to check whether a particular widget is minimizable. By default it
     * returns true. The result have to be the same for whole life-time of the
     * widget. If not, then the revalidation has to be invoked manually. An
     * anchor (created by <code>NodeWidget.createPinAnchor</code> is not
     * affected by this method.
     *
     * @param widget the widget
     * @return true, if the widget is minimizable; false, if the widget is not
     * minimizable
     */
    protected boolean isMinimizableWidget(Widget widget) {
        return true;
    }

    /**
     * Check the minimized state.
     *
     * @return true, if minimized
     */
    public boolean isMinimized() {
        return stateModel.getBooleanState();
    }

    /**
     * Set the minimized state. This method will show/hide child widgets of this
     * Widget and switches anchors between node and pin widgets.
     *
     * @param minimized if true, then the widget is going to be minimized
     */
    public void setMinimized(boolean minimized) {
        stateModel.setBooleanState(minimized);
    }

    /**
     * Toggles the minimized state. This method will show/hide child widgets of
     * this Widget and switches anchors between node and pin widgets.
     */
    public void toggleMinimized() {
        stateModel.toggleBooleanState();
    }

    /**
     * Called when a minimized state is changed. This method will show/hide
     * child widgets of this Widget and switches anchors between node and pin
     * widgets.
     */
    @Override
    public void stateChanged() {
        boolean minimized = stateModel.getBooleanState();
        Rectangle rectangle = minimized ? new Rectangle() : null;
        for (Widget widget : getChildren()) {
            if (widget != header) {
                getScene().getSceneAnimator().animatePreferredBounds(widget, minimized && isMinimizableWidget(widget) ? rectangle : null);
            }
        }
        minimizeWidget.setImage(scheme.getMinimizeWidgetImage(this));
    }

    /**
     * Called to notify about the change of the widget state.
     *
     * @param previousState the previous state
     * @param state the new state
     */
    @Override
    protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
        if ((!previousState.isSelected() && state.isSelected()) ||
                (!previousState.isHovered() && state.isHovered())) {
            bringToFront();
            commentWidget.bringToFront();
        }
        scheme.updateUI(this);
    }

    /**
     * Sets a node image.
     *
     * @param image the image
     */
    public void setNodeImage(Image image) {
        imageWidget.setImage(image);
        revalidate();
    }

    /**
     * Returns a node name.
     *
     * @return the node name
     */
    public String getNodeName() {
        return nameWidget.getLabel();
    }

    /**
     * Sets a node name.
     *
     * @param nodeName the node name
     */
    public void setNodeName(String nodeName) {
        nameWidget.setLabel(nodeName);
    }

    /**
     * Attaches a pin widget to the node widget.
     *
     * @param widget the pin widget
     */
    public void attachPinWidget(Widget widget) {
        widget.setCheckClipping(true);
        addChild(widget);
        if (stateModel.getBooleanState() && isMinimizableWidget(widget)) {
            widget.setPreferredBounds(new Rectangle());
        }
    }

    /**
     * Sets node glyphs.
     *
     * @param glyphs the list of images
     */
    public void setGlyphs(List<Image> glyphs) {
        glyphSetWidget.setGlyphs(glyphs);
    }

    /**
     * Returns a node name widget.
     *
     * @return the node name widget
     */
    public LabelWidget getNodeNameWidget() {
        return nameWidget;
    }

    /**
     * Collapses the widget.
     */
    @Override
    public void collapseWidget() {
        stateModel.setBooleanState(true);
    }

    /**
     * Expands the widget.
     */
    @Override
    public void expandWidget() {
        stateModel.setBooleanState(false);
    }

    /**
     * Returns a header widget.
     *
     * @return the header widget
     */
    public Widget getHeader() {
        return header;
    }

    /**
     * Returns a minimize button widget.
     *
     * @return the minimize button widget
     */
    public Widget getMinimizeButton() {
        return minimizeWidget;
    }

    public void setComment(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            // remove comment
            commentWidget.setText("");
            commentWidget.setVisible(false);
            commentWidget.removeFromParent();
        } else {
            // add comment
            if (commentWidget.getParentWidget() == null) {
                getParentWidget().addChild(commentWidget);
            }
            commentWidget.setText(comment);
            commentWidget.setVisible(true);       
        }
        scheme.updateUI(this);
    }

    public String getComment() {
        return commentWidget.getText();
    }
    
    private void positionComment() {
        if (!commentWidget.isVisible()) {
            return;
        }
        Point loc = getLocation();
        Rectangle bounds = getBounds();
        Rectangle commentBounds = commentWidget.getBounds();
        if (loc == null || bounds == null|| commentBounds == null) {
            return;
        }
        int offset = commentWidget.getBorder().getInsets().left;
        commentWidget.setPreferredLocation(new Point(loc.x + offset, loc.y - commentBounds.height - 4));
        commentWidget.setMinimumSize(new Dimension(bounds.width, 15));
    }

    @Override
    protected void paintChildren() {
        if (isBelowLODThreshold()) {
            return;
        }
        super.paintChildren();
    }

    public boolean isBelowLODThreshold() {
        return scene.isBelowLODThreshold();
    }

    public Widget getCommentWidget() {
        return commentWidget;
    }
    
    public void setSchemeColors(LAFScheme.Colors colors) {
        this.schemeColors = colors;
        revalidate();
    }
    
    public LAFScheme.Colors getSchemeColors() {
        return schemeColors == null ? scene.getSchemeColors() : schemeColors;
    }

    private class SceneListenerImpl implements Scene.SceneListener {

        @Override
        public void sceneRepaint() {
            // no op
        }

        @Override
        public void sceneValidating() {
            scheme.updateUI(NodeWidget.this);
        }

        @Override
        public void sceneValidated() {
            positionComment();
        }

    }

    private class ToggleMinimizedAction extends WidgetAction.Adapter {

        @Override
        public State mousePressed(Widget widget, WidgetMouseEvent event) {
            if (event.getButton() == MouseEvent.BUTTON1 || event.getButton() == MouseEvent.BUTTON2) {
                stateModel.toggleBooleanState();
//                return State.CONSUMED; // temporary fix - minimized state saved on de-selection
            }
            return State.REJECTED;
        }
    }

}
