/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith.
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
package org.praxislive.ide.pxr.graph;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.Value;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PBoolean;
import org.praxislive.core.types.PBytes;
import org.praxislive.core.types.PNumber;
import org.praxislive.core.types.PResource;
import org.praxislive.ide.core.api.Syncable;
import java.awt.Font;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.EditProvider;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.explorer.propertysheet.PropertyPanel;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

/**
 *
 */
class PropertyWidget extends LabelWidget {

    private static final Object SYNC_KEY = new Object();
    private static final DecimalFormat FORMATTER = new DecimalFormat("####0.0####");

    private final Scene scene;
    private final ControlInfo info;
    private final Node node;
    private final Node.Property<Object> property;
    private final Listener listener;
    private final Syncable sync;
    private final Value.Type<?> valueType;

    public PropertyWidget(Scene scene, ControlInfo info, Node node, Node.Property<?> property) {
        super(scene);
        this.scene = scene;
        this.info = Objects.requireNonNull(info);
        this.node = Objects.requireNonNull(node);
        this.property = Objects.requireNonNull((Node.Property<Object>) property);
        listener = new Listener();
        Font f = scene.getDefaultFont();
        setFont(f.deriveFont(f.getSize2D() * 0.9f));
        sync = node.getLookup().lookup(Syncable.class);
        List<ArgumentInfo> args = info.outputs();
        if (args.size() != 1) {
            valueType = Value.Type.of(Value.class);
        } else {
            valueType = args.get(0).argumentType();
        }
        initEditAction();
    }

    @Override
    protected void notifyAdded() {
        super.notifyAdded();
        node.addPropertyChangeListener(listener);
        updateText();
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

    private void initEditAction() {
        if (info.controlType() != ControlInfo.Type.Property) {
            return;
        }
        List<ArgumentInfo> args = info.outputs();
        if (args.size() != 1) {
            return;
        }
        Class<? extends Value> type = args.get(0).argumentType().asClass();
        
        if (PBytes.class == type) {
            return;
        }
        
        List<String> suggested = Collections.emptyList();
        boolean inplace = false;
        if (PNumber.class == type) {
            inplace = true;
        } else if (PBoolean.class == type) {
            suggested = Arrays.asList("true", "false");
            inplace = true;
        } else {
            Value allowed = args.get(0).properties().get(ArgumentInfo.KEY_ALLOWED_VALUES);
            if (allowed != null) {
                suggested = PArray.from(allowed).map(a
                        -> a.stream().map(Value::toString).collect(Collectors.toList())
                ).orElse(Collections.emptyList());
                inplace = true;
            }
        }
        if (inplace) {
            getActions().addAction(ActionFactory.createInplaceEditorAction(
                    new SuggestFieldInplaceEditorProvider(new InplaceEditor(suggested))));
        } else {
            getActions().addAction(ActionFactory.createEditAction(new PropertyEditHandler()));
        }
    }

    private void updateText() {
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
                    setLabel(ed.getValue().toString());
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
        r.width = Math.min(100, r.width);
        return r;
    }

    private class Listener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(property.getName())) {
                updateText();
            }
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
            if (!ed.supportsCustomEditor()) {
                return;
            }
            PropertyPanel panel = new PropertyPanel(property, PropertyPanel.PREF_CUSTOM_EDITOR);
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

}
