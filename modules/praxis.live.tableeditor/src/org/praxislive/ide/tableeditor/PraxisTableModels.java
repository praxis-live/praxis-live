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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.tableeditor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import org.openide.util.ChangeSupport;

/**
 *
 */
class PraxisTableModels implements Iterable<PraxisTableModel> {
    
    private final List<PraxisTableModel> data;
    private final ChangeSupport cs;
    private final ModelListener listener;

    PraxisTableModels() {
        this.data = new ArrayList<>();
        this.cs = new ChangeSupport(this);
        this.listener = new ModelListener();
    }
    
    public void addChangeListener(ChangeListener listener) {
        cs.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        cs.removeChangeListener(listener);
    }

    public void add(PraxisTableModel model) {
        data.add(model);
        model.addTableModelListener(listener);
        cs.fireChange();
    }
    
    public PraxisTableModel remove(int index) {
        PraxisTableModel r = data.remove(index);
        if (r != null) {
            r.removeTableModelListener(listener);
            cs.fireChange();
        }
        return r;
    }
    
    public int size() {
        return data.size();
    }

    public PraxisTableModel get(int i) {
        return data.get(i);
    }

    @Override
    public Iterator<PraxisTableModel> iterator() {
        return data.iterator();
    }
    
    private class ModelListener implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent e) {
            cs.fireChange();
        }
        
    }
    
}
