/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2017 Neil C Smith.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.netbeans.api.visual.action.CycleFocusProvider;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 */
class PraxisCycleFocusProvider implements CycleFocusProvider {

    @Override
    public boolean switchPreviousFocus(Widget widget) {
        Scene scene = widget.getScene();
        return scene instanceof PraxisGraphScene && switchFocus((PraxisGraphScene<?>) scene, false);
    }

    @Override
    public boolean switchNextFocus(Widget widget) {
        Scene scene = widget.getScene();
        return scene instanceof PraxisGraphScene && switchFocus((PraxisGraphScene<?>) scene, true);
    }
    
    private boolean switchFocus(PraxisGraphScene<?> scene, boolean forward) {
        boolean changeSelection = false;
        Object focused = scene.getFocusedObject();
        List<Object> nodes = scene.getSelectedObjects().stream()
                .filter(scene::isNode).collect(Collectors.toCollection(ArrayList::new));
        if (nodes.size() < 2) {
            nodes = scene.getNodes().stream().collect(Collectors.toCollection(ArrayList::new));
            changeSelection = true;
        }
        if (nodes.isEmpty()) {
            return false;
        }
        nodes.sort(new LayoutComparator(scene));    
        int curIdx = nodes.indexOf(focused);
        if (forward) {
            curIdx++;
            if (curIdx >= nodes.size()) {
                focused = nodes.get(0);
            } else {    
                focused = nodes.get(curIdx);
            }
        } else {
            curIdx--;
            if (curIdx < 0) {
                focused = nodes.get(nodes.size() - 1);
            } else {
                focused = nodes.get(curIdx);
            }
        }
        scene.setFocusedObject(focused);
        if (changeSelection) {
            scene.setSelectedObjects(Collections.singleton(focused));
        }
        return true;
    } 
    
    
    static class LayoutComparator implements Comparator<Object> {

        private final static int TOLERANCE = 100;
        
        private final PraxisGraphScene<?> scene;

        LayoutComparator(PraxisGraphScene<?> scene) {
            this.scene = scene;
        }

        @Override
        public int compare(Object o1, Object o2) {
            if (o1 == o2) {
                return 0;
            }
            Widget w1 = scene.findWidget(o1);
            Widget w2 = scene.findWidget(o2);
            
            Point w1p = w1.getLocation();
            Point w2p = w2.getLocation();
            
            if (Math.abs(w1p.y - w2p.y) < TOLERANCE) {
                return (w1p.x < w2p.x ? -1 : 1);
            } else {
                return w1p.y < w2p.y ? -1 : 1;
            }
            
            
        }
        
    }
    
    
}
