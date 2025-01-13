/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2025 Neil C Smith.
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
package org.praxislive.ide.pxr.graph;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.Action;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.EditProvider;
import org.netbeans.api.visual.action.InplaceEditorProvider;
import org.netbeans.api.visual.action.SelectProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.border.Border;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.explorer.propertysheet.PropertyPanel;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.Value;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PBoolean;
import org.praxislive.core.types.PBytes;
import org.praxislive.core.types.PNumber;
import org.praxislive.core.types.PResource;
import org.praxislive.ide.core.api.Syncable;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.pxr.graph.scene.LAFScheme;

/**
 *
 */
class ExposedControls extends Widget {

    private static final Object SYNC_KEY = new Object();

    private static final Border LABEL_BORDER = BorderFactory.createEmptyBorder(1);

    private final Map<String, ControlWidget> controlWidgets;
    private final Node node;
    private final Syncable sync;
    private final Listener listener;

    public ExposedControls(Scene scene, ComponentProxy cmp, List<String> controls) {
        super(scene);
        controlWidgets = new LinkedHashMap<>();
        node = cmp.getNodeDelegate();
        sync = node.getLookup().lookup(Syncable.class);
        listener = new Listener();
        setOpaque(true);
        setForeground(null);
        setBorder(BorderFactory.createRoundedBorder(4, 4, 2, 2, LAFScheme.BACKGROUND, null));
        setLayout(new TableLayout());
        ComponentInfo info = cmp.getInfo();
        getActions().addAction(new MouseCapture());
        for (String control : controls) {
            ControlInfo controlInfo = info.controlInfo(control);
            if (controlInfo == null) {
                continue;
            }
            ControlWidget controlWidget = createControl(scene, node, control, controlInfo);
            if (controlWidget != null) {
                addChild(createLabel(scene, control));
                controlWidgets.put(control, controlWidget);
                addChild(controlWidget);
            }
        }

    }

    @Override
    protected void notifyAdded() {
        super.notifyAdded();
        controlWidgets.values().forEach(cw -> cw.updateText());
        node.addPropertyChangeListener(listener);
        if (sync != null) {
            sync.addKey(SYNC_KEY);
        }
    }

    @Override
    protected void notifyRemoved() {
        super.notifyRemoved();
        node.removePropertyChangeListener(listener);
        if (sync != null) {
            sync.removeKey(SYNC_KEY);
        }
    }

    private ControlWidget createControl(Scene scene, Node node,
            String controlID, ControlInfo controlInfo) {
        if (controlInfo.controlType() == ControlInfo.Type.Action) {
            Action action = findAction(node, controlID);
            if (action != null) {
                return new ControlWidget(scene, controlInfo, node, action);
            }
        } else {
            Node.Property<?> property = findProperty(node, controlID);
            if (property != null) {
                return new ControlWidget(scene, controlInfo, node, property);
            }
        }
        return null;
    }

    private Widget createLabel(Scene scene, String id) {
        LabelWidget label = new LabelWidget(scene, id + " :");
        label.setForeground(null);
        label.setAlignment(LabelWidget.Alignment.RIGHT);
        label.setBorder(LABEL_BORDER);
        return label;
    }

    private Action findAction(Node node, String id) {
        for (Action action : node.getActions(false)) {
            if (action != null && id.equals(action.getValue(Action.NAME))) {
                return action;
            }
        }
        return null;
    }

    private Node.Property<?> findProperty(Node node, String id) {
        for (Node.PropertySet propSet : node.getPropertySets()) {
            for (Node.Property<?> prop : propSet.getProperties()) {
                if (id.equals(prop.getName())) {
                    if (prop.isHidden()) {
                        return null;
                    } else {
                        return prop;
                    }
                }
            }
        }
        return null;
    }

    private class Listener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String control = evt.getPropertyName();
            if (control == null) {
                controlWidgets.values().forEach(cw -> cw.updateText());
            } else {
                ControlWidget cw = controlWidgets.get(control);
                if (cw != null) {
                    cw.updateText();
                }
            }
        }

    }

    private static class ControlWidget extends LabelWidget {

        private static final DecimalFormat FORMATTER = new DecimalFormat("####0.0####");

        private final Scene scene;
        private final ControlInfo info;
        private final Node node;
        private final Node.Property<Object> property;
        private final Action action;
        private final Value.Type<?> valueType;

        private ControlWidget(Scene scene, ControlInfo info, Node node, Action action) {
            this(scene, info, node, null, action);
        }

        private ControlWidget(Scene scene, ControlInfo info, Node node, Node.Property<?> property) {
            this(scene, info, node, property, null);
        }

        @SuppressWarnings("unchecked")
        private ControlWidget(Scene scene, ControlInfo info, Node node, Node.Property<?> property, Action action) {
            super(scene);
            this.scene = scene;
            this.info = Objects.requireNonNull(info);
            this.node = node;
            this.property = (Node.Property<Object>) property;
            this.action = action;
            setBorder(new BorderImpl());
            if (action != null && action.getValue(Action.NAME) instanceof String id) {
                setLabel(id);
            }
            List<ArgumentInfo> args = info.outputs();
            if (args.size() != 1) {
                valueType = Value.Type.of(Value.class);
            } else {
                valueType = Value.Type.fromName(args.get(0).argumentType())
                        .orElse(Value.Type.of(Value.class));
            }
            getActions().addAction(scene.createWidgetHoverAction());
            if (action != null) {
                initActionTrigger();
            } else if (info.controlType() == ControlInfo.Type.Function) {
                initFunctionEdit();
            } else {
                initPropertyEdit();
            }
            if (info.controlType() != ControlInfo.Type.ReadOnlyProperty) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }

        private void initActionTrigger() {
            getActions().addAction(ActionFactory.createSelectAction(new SelectProvider() {
                @Override
                public boolean isAimingAllowed(Widget widget, Point point, boolean bln) {
                    return true;
                }

                @Override
                public boolean isSelectionAllowed(Widget widget, Point point, boolean bln) {
                    return true;
                }

                @Override
                public void select(Widget widget, Point point, boolean bln) {
                    action.actionPerformed(new ActionEvent(widget, ActionEvent.ACTION_PERFORMED, "trigger"));
                }

            }, false));

        }

        private void initFunctionEdit() {
            WidgetAction inplaceEditorAction = ActionFactory.createInplaceEditorAction(
                    new SuggestFieldInplaceEditorProvider(new InplaceEditor(List.of())));
            InplaceEditorProvider.EditorController inplaceEditorController
                    = ActionFactory.getInplaceEditorController(inplaceEditorAction);
            getActions().addAction(new WidgetAction.Adapter() {
                @Override
                public WidgetAction.State mouseClicked(Widget widget, WidgetAction.WidgetMouseEvent event) {
                    if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 1) {
                        inplaceEditorController.openEditor(widget);
                        return State.createLocked(widget, inplaceEditorAction);
                    } else {
                        return State.REJECTED;
                    }
                }
            });
            getActions().addAction(inplaceEditorAction);
        }

        private void initPropertyEdit() {
            boolean readOnly = info.controlType() != ControlInfo.Type.Property;
            Class<? extends Value> type = valueType.asClass();
            List<String> suggested = List.of();
            boolean inplace = false;
            if (!readOnly) {
                if (PNumber.class == type) {
                    inplace = true;
                } else if (!info.inputs().isEmpty()) {
                    Value allowed = info.inputs().get(0).properties().get(ArgumentInfo.KEY_ALLOWED_VALUES);
                    if (allowed != null) {
                        suggested = PArray.from(allowed)
                                .map(a -> a.stream().map(Value::toString).toList())
                                .orElse(List.of());
                        inplace = !suggested.isEmpty();
                    }
                }

            }
            if (inplace) {
                WidgetAction inplaceEditorAction = ActionFactory.createInplaceEditorAction(
                        new SuggestFieldInplaceEditorProvider(new InplaceEditor(suggested)));
                InplaceEditorProvider.EditorController inplaceEditorController
                        = ActionFactory.getInplaceEditorController(inplaceEditorAction);
                getActions().addAction(new WidgetAction.Adapter() {
                    @Override
                    public WidgetAction.State mouseClicked(Widget widget, WidgetAction.WidgetMouseEvent event) {
                        if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 1) {
                            inplaceEditorController.openEditor(widget);
                            return State.createLocked(widget, inplaceEditorAction);
                        } else {
                            return State.REJECTED;
                        }
                    }
                });
                getActions().addAction(inplaceEditorAction);
            } else {
                EditProvider editHandler;
                if (PBoolean.class == type) {
                    editHandler = new BooleanEditHandler();
                } else {
                    editHandler = new PropertyEditHandler();
                }
                getActions().addAction(new WidgetAction.Adapter() {
                    @Override
                    public WidgetAction.State mouseClicked(Widget widget, WidgetAction.WidgetMouseEvent event) {
                        if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 1) {
                            editHandler.edit(widget);
                            return State.CONSUMED;
                        } else {
                            return State.REJECTED;
                        }
                    }
                });
            }
        }

        private void updateText() {
            if (property == null) {
                return;
            }
            try {
                Object value = property.getValue();
                if (valueType.asClass() == PNumber.class) {
                    PNumber num = PNumber.from((Value) value).orElse(PNumber.ZERO);
                    if (num.isInteger()) {
                        setLabel(num.toString());
                    } else {
                        setLabel(FORMATTER.format(num.value()));
                    }
                } else if (valueType.asClass() == PBytes.class) {
                    setLabel("<bytes>");
                } else if (valueType.asClass() == PResource.class) {
                    String txt = value.toString();
                    int lastSlash = txt.lastIndexOf("/");
                    if (lastSlash > 0) {
                        txt = txt.substring(lastSlash);
                    }
                    setLabel(txt);
                } else {
                    PropertyEditor ed = property.getPropertyEditor();
                    ed.setValue(property.getValue());
                    String text = ed.getAsText();
                    if (text != null) {
                        setLabel(text);
                    } else {
                        setLabel(String.valueOf(ed.getValue()));
                    }
                }

            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
                setLabel("");
            }
            scene.validate();
        }

        @Override
        protected Rectangle calculateClientArea() {
            Rectangle r = super.calculateClientArea();
            r.width = Math.min(120, Math.max(80, r.width));
            return r;
        }

        @Override
        protected void paintWidget() {
            if (info.controlType() == ControlInfo.Type.Function) {
                PropertyEditor ed = property.getPropertyEditor();
                if (ed.isPaintable()) {
                    ed.paintValue(getGraphics(), getClientArea());
                    return;
                }
            }
            super.paintWidget();
        }

        @Override
        protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
            if (previousState.isHovered() != state.isHovered()
                    || previousState.isWidgetAimed() != state.isWidgetAimed()) {
                repaint();
            }
        }

        private class InplaceEditor implements SuggestFieldInplaceEditor {

            private final List<String> suggested;

            private InplaceEditor(List<String> suggested) {
                this.suggested = suggested;
            }

            @Override
            public List<String> getSuggestedValues(Widget widget) {
                return suggested;
            }

            @Override
            public boolean isEnabled(Widget widget) {
                return true;
            }

            @Override
            public String getText(Widget widget) {
                try {
                    PropertyEditor ed = property.getPropertyEditor();
                    ed.setValue(property.getValue());
                    String text = ed.getAsText();
                    if (text != null) {
                        return text;
                    } else {
                        return "";
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                    return "";
                }
            }

            @Override
            public void setText(Widget widget, String string) {
                try {
                    PropertyEditor ed = property.getPropertyEditor();
                    ed.setAsText(string);
                    property.setValue(ed.getValue());
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

        }

        private class PropertyEditHandler implements EditProvider {

            @Override
            public void edit(Widget widget) {
                PropertyEditor ed = property.getPropertyEditor();
                PropertyPanel panel;
                if (ed.supportsCustomEditor()) {
                    panel = new PropertyPanel(property, PropertyPanel.PREF_CUSTOM_EDITOR);
                } else {
                    panel = new PropertyPanel(property);
                }
                panel.setChangeImmediate(false);
                DialogDescriptor descriptor = new DialogDescriptor(
                        panel,
                        property.getDisplayName(),
                        true,
                        (e) -> {
                            if (e.getSource() == DialogDescriptor.OK_OPTION) {
                                panel.updateValue();
                            }
                        });
                DialogDisplayer.getDefault().notifyLater(descriptor);
            }

        }

        private class BooleanEditHandler implements EditProvider {

            @Override
            public void edit(Widget widget) {
                try {
                    boolean isBoolean = ((Class<?>) property.getValueType()) == Boolean.class;
                    if (isBoolean) {
                        boolean value = (boolean) property.getValue();
                        value = !value;
                        property.setValue(value);
                    } else {
                        PropertyEditor ed = property.getPropertyEditor();
                        boolean value = "true".equalsIgnoreCase(ed.getAsText());
                        value = !value;
                        ed.setAsText("" + value);
                        property.setValue(ed.getValue());
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

        }

        private class BorderImpl implements Border {

            @Override
            public Insets getInsets() {
                return LABEL_BORDER.getInsets();
            }

            @Override
            public void paint(Graphics2D g, Rectangle bounds) {
                if (getState().isHovered() && info.controlType() != ControlInfo.Type.ReadOnlyProperty) {
                    g.setColor(getParentWidget().getForeground());
                    int y = bounds.y + bounds.height - 1;
                    g.drawLine(bounds.x, y, bounds.x + bounds.width, y);
                }
            }

            @Override
            public boolean isOpaque() {
                return false;
            }

        }
    }

    private static class TableLayout implements Layout {

        private final int HORIZONTAL_GAP = 4;
        private final int VERTICAL_GAP = 4;

        @Override
        public void layout(Widget widget) {
            List<Widget> children = widget.getChildren();

            int y = 0;
            int labelWidth = 0;
            for (int i = 0; i < children.size(); i += 2) {
                Widget label = children.get(i);
                Rectangle labelBounds = label.getPreferredBounds();
                labelWidth = Math.max(labelWidth, labelBounds.width);
            }

            for (int i = 0; i < children.size(); i += 2) {
                Widget label = children.get(i);
                Widget property = children.get(i + 1);
                Rectangle labelBounds = label.getPreferredBounds();
                Rectangle propBounds = property.getPreferredBounds();
                int height = Math.max(labelBounds.height, propBounds.height);
                labelBounds.width = labelWidth;
                labelBounds.height = height;
                label.resolveBounds(new Point(0, y), labelBounds);
                propBounds.height = height;
                property.resolveBounds(
                        new Point(labelBounds.width + HORIZONTAL_GAP, y),
                        propBounds);

                y += height + VERTICAL_GAP;

            }

        }

        @Override
        public boolean requiresJustification(Widget widget) {
            return true;
        }

        @Override
        public void justify(Widget widget) {
            Rectangle bounds = widget.getClientArea();
            int x = bounds.x;
            int y = bounds.y;

            List<Widget> children = widget.getChildren();

            for (int i = 0; i < children.size(); i += 2) {
                Widget label = children.get(i);
                Widget property = children.get(i + 1);
                Rectangle labelBounds = label.getBounds();
                Rectangle propBounds = property.getBounds();
                int height = Math.max(labelBounds.height, propBounds.height);
                labelBounds.height = height;
                label.resolveBounds(new Point(x - labelBounds.x, y - labelBounds.y), labelBounds);
                propBounds.height = height;
                propBounds.width = bounds.width - HORIZONTAL_GAP - labelBounds.width;
                property.resolveBounds(
                        new Point(x - propBounds.x + labelBounds.width + HORIZONTAL_GAP,
                                y - propBounds.y),
                        propBounds);

                y += height + VERTICAL_GAP;

            }
        }

    }

    private static class MouseCapture extends WidgetAction.Adapter {

        @Override
        public State mousePressed(Widget widget, WidgetMouseEvent event) {
            return State.CONSUMED;
        }

        @Override
        public State mouseClicked(Widget widget, WidgetMouseEvent event) {
            return State.CONSUMED;
        }

        @Override
        public State mouseDragged(Widget widget, WidgetMouseEvent event) {
            return State.CONSUMED;
        }

//        @Override
//        public State mouseMoved(Widget widget, WidgetMouseEvent event) {
//            return State.CONSUMED;
//        }
    }

}
