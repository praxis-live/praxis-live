/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeListener;
import net.miginfocom.swing.MigLayout;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.pxr.api.ComponentProxy;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
import net.neilcsmith.praxis.live.pxr.api.ProxyException;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.ChangeSupport;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class EditLayer extends JComponent {

    private final static Logger LOG = Logger.getLogger(EditLayer.class.getName());
    private final static Color hoverColor = new Color(143, 171, 196);
    private final static Color selectedColor = hoverColor.brighter();
    private GuiEditor editor;
    private RootProxy root;
    private JPanel rootPanel;
    private MouseController mouse;
    private JComponent hovered;
    private JComponent selected;
    private ChangeSupport cs;
    private SelectedListener selectedListener;

    EditLayer(GuiEditor editor, JPanel rootPanel) {
        this.editor = editor;
        this.rootPanel = rootPanel;
        root = editor.getRoot();
        cs = new ChangeSupport(this);
        mouse = new MouseController();
        selectedListener = new SelectedListener();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setDropTarget(new DropTarget(this, new DnDController()));

        super.setVisible(false);

    }
    
    void addChangeListener(ChangeListener listener) {
        cs.addChangeListener(listener);
    }

    void removeChangeListener(ChangeListener listener) {
        cs.removeChangeListener(listener);
    }

    void performLayoutAction(LayoutAction.Type type) {
        if (selected == null || selected == rootPanel) {
            LOG.fine("performLayoutAction() called when shouldn't be enabled");
            return;
        }
        switch (type) {
            case MoveUp:
                Utils.move(root, selected, 0, -1);
                break;
            case MoveDown:
                Utils.move(root, selected, 0, 1);
                break;
            case MoveLeft:
                Utils.move(root, selected, -1, 0);
                break;
            case MoveRight:
                Utils.move(root, selected, 1, 0);
                break;
            case IncreaseSpanX:
                Utils.resize(root, selected, 1, 0);
                break;
            case DecreaseSpanX:
                Utils.resize(root, selected, -1, 0);
                break;
            case IncreaseSpanY:
                Utils.resize(root, selected, 0, 1);
                break;
            case DecreaseSpanY:
                Utils.resize(root, selected, 0, -1);
                break;
        }
    }

    boolean isLayoutActionEnabled(LayoutAction.Type type) {
        return selected != null && selected != rootPanel;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        ComponentProxy pxy;
        if (visible && selected != null && selected != rootPanel) {
            pxy = Utils.findComponentProxy(root, selected);
        } else {
            pxy = null;
        }
        try {
            if (pxy == null) {
                editor.setSelected(new Node[]{root.getNodeDelegate()});
            } else {
                editor.setSelected(new Node[]{pxy.getNodeDelegate()});
            }
        } catch (Exception exception) {
        }
        cs.fireChange();
    }

    @Override
    protected void paintComponent(Graphics g) {
        paintHovered(g);
        paintSelected(g);
    }

    private void paintHovered(Graphics g) {
        if (hovered == null) {
            return;
        }
        g.setColor(hoverColor);
        Point loc = hovered.getLocation();
        loc = SwingUtilities.convertPoint(hovered.getParent(), loc, rootPanel);
        g.drawRect(loc.x, loc.y, hovered.getWidth() - 1, hovered.getHeight() - 1);
    }

    private void paintSelected(Graphics g) {
        if (selected == null || selected == rootPanel) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g;
        Point loc = selected.getLocation();
        loc = loc = SwingUtilities.convertPoint(selected.getParent(), loc, rootPanel);
        g2d.setColor(selectedColor);
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.2f));
        g2d.fillRect(loc.x, loc.y, selected.getWidth(), selected.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.drawRect(loc.x, loc.y, selected.getWidth() - 1, selected.getHeight() - 1);
    }

    private void clearHoveredComponent() {
        if (hovered != null) {
            hovered = null;
            repaint();
        }
    }

    private void updateHoveredComponent(int x, int y) {
        JComponent cmp = findComponentAtPoint(x, y);
        updateLabel(cmp, x, y);
        if (cmp == rootPanel) {
            cmp = null;
        }
        if (cmp != hovered) {
            hovered = cmp;
            repaint();
        }
    }

    private void updateLabel(JComponent cmp, int x, int y) {
        JComponent cnt = Utils.findContainerComponent(editor.getRoot(), cmp);
        if (cnt != rootPanel) {
            Point loc = SwingUtilities.convertPoint(rootPanel, x, y, cnt);
            x = loc.x;
            y = loc.y;
        }
        int[] position = null;
        if (cnt != null && cnt.getLayout() instanceof MigLayout) {
            if (cnt != cmp) {
                position = Utils.getGridPosition(cnt, cmp);
            } else {
                position = Utils.getGridPosition(cnt, x, y);
            }
        }
        String text = "";
        if (position == null) {
            text = "Unknown";
        } else if (position.length > 3) {
            text = "X : " + position[0] + " Y : " + position[1] + " Span X : " + position[2] + " Span Y : " + position[3];
        } else if (position.length > 1) {
            text = "X : " + position[0] + " Y : " + position[1];
        }
        setToolTipText(text);
    }

    private void updateSelectedComponent(int x, int y) {
        JComponent cmp = findComponentAtPoint(x, y);
        if (cmp == selected) {
            return;
        }
        try {
            if (selected != null) {
                selected.removeAncestorListener(selectedListener);
            }
            selected = cmp;
            selected.addAncestorListener(selectedListener);
            ComponentProxy pxy = Utils.findComponentProxy(root, cmp);
            if (pxy == null) {
                editor.setSelected(new Node[]{root.getNodeDelegate()});
            } else {
                editor.setSelected(new Node[]{pxy.getNodeDelegate()});
            }

        } catch (Exception ex) {
            LOG.log(Level.WARNING, null, ex);
            selected.removeAncestorListener(selectedListener);
            selected = null;
        }
        cs.fireChange();
        repaint();
    }

    private void addComponent(ComponentType type, int x, int y) throws Exception {
        JComponent dropOver = findComponentAtPoint(x, y);
        final JComponent container = Utils.findContainerComponent(root, dropOver);
        if (container != rootPanel) {
            Point loc = SwingUtilities.convertPoint(rootPanel, x, y, container);
            x = loc.x;
            y = loc.y;
        }
        final ContainerProxy pxy = (ContainerProxy) Utils.findComponentProxy(root, container);
        int[] pos;
        if (container == dropOver) {
            if (container.getComponentCount() == 0 || !(container.getLayout() instanceof MigLayout)) {
                pos = new int[]{0, 0};
            } else {
                pos = Utils.getGridPosition(container, x, y);
            }
        } else {
            pos = Utils.getGridPosition(container, dropOver);
        }
        Utils.ensureSpace(container, pos[0], pos[1], 1, 1, Collections.<JComponent>emptySet(), true);
        final PString layout = PString.valueOf("cell " + pos[0] + " " + pos[1]);
        final String id = getFreeID(pxy, type);
        pxy.addChild(id, type, new Callback() {

            @Override
            public void onReturn(CallArguments args) {
                try {
                    pxy.getChild(id).call("layout", CallArguments.create(layout), new Callback() {

                        @Override
                        public void onReturn(CallArguments args) {
                            Utils.compactGrid(container);
                        }

                        @Override
                        public void onError(CallArguments args) {
                        }
                    });
                } catch (ProxyException ex) {
                    Logger.getLogger(EditLayer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void onError(CallArguments args) {
                DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Error creating component", NotifyDescriptor.ERROR_MESSAGE));
            }
        });

    }

    private String getFreeID(ContainerProxy container, ComponentType type) {
        String base = type.toString();
        base = base.substring(base.lastIndexOf(":") + 1);
        for (int i = 1; i < 100; i++) {
            if (container.getChild(base + i) == null) {
                return base + i;
            }
        }
        return "";
    }

    private JComponent findComponentAtPoint(int x, int y) {
        Component cmp = SwingUtilities.getDeepestComponentAt(rootPanel, x, y);
        return Utils.findAddressedComponent(cmp);
    }
    
    private class SelectedListener implements AncestorListener {

        @Override
        public void ancestorAdded(AncestorEvent ae) {}

        @Override
        public void ancestorRemoved(AncestorEvent ae) {
            updateSelectedComponent(0, 0);
        }

        @Override
        public void ancestorMoved(AncestorEvent ae) {}
        
    }

    private class MouseController extends MouseAdapter {

        @Override
        public void mouseMoved(MouseEvent me) {
            updateHoveredComponent(me.getX(), me.getY());
        }

        @Override
        public void mouseExited(MouseEvent me) {
            clearHoveredComponent();
        }

        @Override
        public void mousePressed(MouseEvent me) {
            requestFocusInWindow();
            if (!me.isPopupTrigger()) {
                updateSelectedComponent(me.getX(), me.getY());
            }
        }

        @Override
        public void mouseClicked(MouseEvent me) {
            if (me.getClickCount() == 2) {
                updateSelectedComponent(me.getX(), me.getY());
                editor.performPreferredAction();
            }
        }

        
    }

    private class DnDController extends DropTargetAdapter {

        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
            if (extractType(dtde.getTransferable()) == null) {
                dtde.rejectDrag();
            }
        }

        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            updateHoveredComponent(dtde.getLocation().x, dtde.getLocation().y);
        }

        @Override
        public void drop(DropTargetDropEvent dtde) {
            ComponentType type = extractType(dtde.getTransferable());
            if (type != null) {
                try {
                    addComponent(type, dtde.getLocation().x, dtde.getLocation().y);
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    return;
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, null, ex);
                }

            }
            dtde.rejectDrop();

        }

        private ComponentType extractType(Transferable transferable) {
            Node n = NodeTransfer.node(transferable, NodeTransfer.DND_COPY);
            if (n != null) {
                ComponentType t = n.getLookup().lookup(ComponentType.class);
                if (t != null) {
                    return t;
                }
            }
            return null;
        }
    }
}
