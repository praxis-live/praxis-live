/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.live.pxr.PXRParser.ComponentElement;
import net.neilcsmith.praxis.live.pxr.PXRParser.ConnectionElement;
import net.neilcsmith.praxis.live.pxr.PXRParser.RootElement;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
import net.neilcsmith.praxis.live.pxr.api.EditorUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 *
 * @author Neil C Smith
 */
class ImportRenameSupport {
    
    
    
    static boolean prepareForPaste(ContainerProxy container, RootElement fakeRoot) {
        return prepare(container, fakeRoot, true);
    }
    
    static boolean prepareForImport(ContainerProxy container, RootElement fakeRoot) {
        return prepare(container, fakeRoot, false);
    }
    
    private static boolean prepare(ContainerProxy container, RootElement fakeRoot, boolean paste) {
        ComponentAddress ctxt = container.getAddress();
        if (!ctxt.equals(fakeRoot.address)) {
            // assert?
            throw new IllegalArgumentException();
        }
        Set<String> existing = new LinkedHashSet<String>(Arrays.asList(container.getChildIDs()));
        ComponentElement[] cmps = fakeRoot.children;
        List<String> names = new ArrayList<String>(cmps.length);
        for (ComponentElement cmp : cmps) {
            String name = EditorUtils.findFreeID(existing, cmp.address.getID(), false);
            existing.add(name);
            names.add(name);
        }
        
        existing.clear();
        existing.addAll(Arrays.asList(container.getChildIDs()));
        RenameTableModel model = new RenameTableModel(existing, Arrays.asList(cmps), names);
        
        NotifyDescriptor dlg = constructDialog(paste ? "Paste as ..." : "Import as ...", model);
        if (DialogDisplayer.getDefault().notify(dlg) != NotifyDescriptor.OK_OPTION) {
            return false;
        }
        
        for (int i=0; i<cmps.length; i++) {
            ComponentAddress ad = cmps[i].address;
            String oldID = ad.getID();
            String newID = names.get(i);
            if (!oldID.equals(newID)) {
                ComponentAddress newAd = ComponentAddress.create(ad.getParentAddress(), newID);
                cmps[i].address = newAd;
                checkChildren(cmps[i], ad, newAd);
                checkConnections(fakeRoot, oldID, newID);
            }
        }
        
        return true;
        
    }
    
    private static NotifyDescriptor constructDialog(String title, RenameTableModel model) {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable(model);
//        Dimension d = table.getPreferredSize();
//        table.setPreferredScrollableViewportSize(new Dimension(
//                Math.min(300, d.width),
//                table.getRowHeight() * (table.getRowCount() + 1)));
        table.requestFocusInWindow();
        panel.add(new JScrollPane(table));
        panel.setPreferredSize(new Dimension(350,table.getRowHeight() * (table.getRowCount() + 2)));
        NotifyDescriptor dlg = new NotifyDescriptor.Confirmation(
                panel,
                title,
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.PLAIN_MESSAGE);
        return dlg;
    }
    
    private static void checkConnections(ComponentElement cmp, String oldID, String newID) {
        for (ConnectionElement con : cmp.connections) {
            if (con.component1.equals(oldID)) {
                con.component1 = newID;
            }
            if (con.component2.equals(oldID)) {
                con.component2 = newID;
            }
        }
    }
    
    private static void checkChildren(ComponentElement cmp,
            ComponentAddress oldAd, ComponentAddress newAd) {
        for (ComponentElement child : cmp.children) {
            String ad = child.address.toString();
            if (ad.startsWith(oldAd.toString())) {
                ad = ad.replace(oldAd.toString(), newAd.toString());
                child.address = ComponentAddress.create(ad);
            }
            checkChildren(child, oldAd, newAd);
        }
    }
    
    
    private static class RenameTableModel extends AbstractTableModel {
        
        private final Set<String> existing;
        private final List<ComponentElement> children;
        private final List<String> names;
        
        
        private RenameTableModel(Set<String> existing, List<ComponentElement> children, List<String> names) {
            this.existing = existing;
            this.children = children;
            this.names = names;
        }
        

        @Override
        public int getRowCount() {
            return children.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return "Existing ID";
            } else {
                return "New ID";
            }
        }
        
        

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return children.get(rowIndex).address.getID();
            } else {
                return names.get(rowIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            String val = aValue.toString();
            if (columnIndex == 1 && ComponentAddress.isValidID(val)) {
                if (existing.contains(val)) {
                    return;
                }
                int idx = names.indexOf(val);
                if (idx > -1 && idx != rowIndex) {
                    return;
                }
                names.set(rowIndex, val);
            }
        }
        
        
        
    }
    
}
