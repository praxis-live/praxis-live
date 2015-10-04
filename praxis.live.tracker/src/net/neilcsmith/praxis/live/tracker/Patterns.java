/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Neil C Smith.
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
package net.neilcsmith.praxis.live.tracker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import org.openide.util.ChangeSupport;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class Patterns implements Iterable<Pattern> {
    
    private final List<Pattern> data;
    private final ChangeSupport cs;
    private final PatternListener listener;

    Patterns() {
        this.data = new ArrayList<>();
        this.cs = new ChangeSupport(this);
        this.listener = new PatternListener();
    }
    
    public void addChangeListener(ChangeListener listener) {
        cs.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        cs.removeChangeListener(listener);
    }

    public void addPattern(Pattern pattern) {
        data.add(pattern);
        pattern.addTableModelListener(listener);
        cs.fireChange();
    }
    
    public Pattern removePattern(int index) {
        Pattern r = data.remove(index);
        if (r != null) {
            r.removeTableModelListener(listener);
            cs.fireChange();
        }
        return r;
    }
    
    public int size() {
        return data.size();
    }

    public Pattern getPattern(int i) {
        return data.get(i);
    }

    @Override
    public Iterator<Pattern> iterator() {
        return data.iterator();
    }
    
    private class PatternListener implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent e) {
            cs.fireChange();
        }
        
    }
    
}
