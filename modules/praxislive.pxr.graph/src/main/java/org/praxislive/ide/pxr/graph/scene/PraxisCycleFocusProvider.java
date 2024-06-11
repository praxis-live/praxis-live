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
package org.praxislive.ide.pxr.graph.scene;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.netbeans.api.visual.action.CycleFocusProvider;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

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
