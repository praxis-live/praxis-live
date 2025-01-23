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
package org.praxislive.ide.pxr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.nodes.Node;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.util.actions.Presenter;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.Watch;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PMap;
import org.praxislive.core.types.PString;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.pxr.api.Attributes;

/**
 *
 */
@ActionID(category = ActionBridge.CATEGORY, id = ExposeControlsAction.ID)
@ActionRegistration(
        displayName = "#CTL_ExposeAction",
        lazy = false
)
@NbBundle.Messages({
    "CTL_ExposeAction=Expose",
    "CTL_ResetExposeAction=Reset"
})
public class ExposeControlsAction extends AbstractAction
        implements ContextAwareAction, Presenter.Popup, Presenter.Menu {

    public static final String ID = "org.praxislive.ide.pxr.ExposeControlsAction";

    private static final String EXPOSE_KEY = "expose";

    private final Lookup.Result<ComponentProxy> result;
    private final LookupListener listener;
    private final DynMenu menu;

    public ExposeControlsAction() {
        this(Utilities.actionsGlobalContext());
    }

    private ExposeControlsAction(Lookup context) {
        super(Bundle.CTL_ExposeAction());
        this.menu = new DynMenu();
        this.result = context.lookupResult(ComponentProxy.class);
        this.listener = this::resultChanged;
        this.result.addLookupListener(
                WeakListeners.create(LookupListener.class, this.listener, this.result));
        setEnabled(false);
        resultChanged(null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // no op?
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp) {
        return new ExposeControlsAction(lkp);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return menu;
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return menu;
    }

    private void resultChanged(LookupEvent ev) {
        Collection<? extends ComponentProxy> components = result.allInstances();
        if (components.size() == 1) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

    private class DynMenu extends JMenuItem implements DynamicMenuContent {

        private DynMenu() {
            super(ExposeControlsAction.this);
        }

        @Override
        public JComponent[] getMenuPresenters() {
            Collection<? extends ComponentProxy> components = result.allInstances();
            if (components.size() != 1) {
                return new JComponent[]{};
            }
            ComponentProxy component = components.iterator().next();
            List<String> exposed = currentlyExposed(component);
            List<JCheckBoxMenuItem> items = buildExposeItems(component, exposed);
            if (items.isEmpty()) {
                return new JComponent[]{};
            }
            JMenu menu = new JMenu(Bundle.CTL_ExposeAction());
            for (JCheckBoxMenuItem cbi : items) {
                cbi.addActionListener(e -> {
                    List<String> selection = extractSelection(menu);
                    if (!Objects.equals(exposed, selection)) {
                        updateExposed(component, selection);
                    }
                });
                menu.add(cbi);
            }
            menu.addSeparator();
            JMenuItem reset = new JMenuItem(Bundle.CTL_ResetExposeAction());
            reset.addActionListener(e -> Attributes.clear(component, EXPOSE_KEY));
            menu.add(reset);
            return new JComponent[]{menu};
        }

        @Override
        public JComponent[] synchMenuPresenters(JComponent[] jcs) {
            return getMenuPresenters();
        }

        private List<JCheckBoxMenuItem> buildExposeItems(ComponentProxy component,
                List<String> exposed) {
            ComponentInfo info = component.getInfo();
            Node node = component.getNodeDelegate();

            List<JCheckBoxMenuItem> items = new ArrayList<>();
            for (String control : info.controls()) {
                ControlInfo controlInfo = info.controlInfo(control);
                if (controlInfo == null) {
                    continue;
                }
                if (isExposable(control, controlInfo, node)) {
                    JCheckBoxMenuItem cbi = new JCheckBoxMenuItem(
                            findLabel(control, controlInfo),
                            exposed.contains(control)
                    );
                    cbi.putClientProperty("control", control);
                    items.add(cbi);
                }
            }
            return items;
        }

        private List<String> currentlyExposed(ComponentProxy component) {
            PArray exposed = Attributes.get(component, PArray.class, EXPOSE_KEY, null);
            if (exposed == null) {
                exposed = Optional.ofNullable(component.getInfo().properties().get(EXPOSE_KEY))
                        .flatMap(PArray::from)
                        .orElse(PArray.EMPTY);
            }
            return exposed.asListOf(String.class);
        }

        private boolean isExposable(String control, ControlInfo controlInfo, Node node) {
            if (Watch.isWatch(controlInfo)) {
                return true;
            } else if (controlInfo.controlType() == ControlInfo.Type.Action) {
                for (Action a : node.getActions(false)) {
                    if (a != null && control.equals(a.getValue(Action.NAME))) {
                        return true;
                    }
                }
                return false;
            } else {
                for (Node.PropertySet propSet : node.getPropertySets()) {
                    for (Node.Property<?> prop : propSet.getProperties()) {
                        if (control.equals(prop.getName())) {
                            if (prop.isHidden()) {
                                return false;
                            } else {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        }

        private String findLabel(String id, ControlInfo controlInfo) {
            if (Watch.isWatch(controlInfo)) {
                PMap watchInfo = Optional.ofNullable(
                        controlInfo.properties().get(Watch.WATCH_KEY))
                        .flatMap(PMap::from)
                        .orElse(PMap.EMPTY);
                return watchInfo.getString(Watch.RELATED_PORT_KEY, id) + " (watch)";
            } else {
                return id;
            }
        }

        private List<String> extractSelection(JMenu menu) {
            List<String> selected = new ArrayList<>();
            for (Component c : menu.getMenuComponents()) {
                if (c instanceof JCheckBoxMenuItem cbi) {
                    if (cbi.isSelected()) {
                        Object control = cbi.getClientProperty("control");
                        if (control instanceof String id) {
                            selected.add(id);
                        }
                    }
                }
            }
            return selected;
        }

        private void updateExposed(ComponentProxy component, List<String> selection) {
            if (selection.isEmpty()) {
                if (component.getInfo().properties().get(EXPOSE_KEY) != null) {
                    Attributes.set(component, EXPOSE_KEY, "<none>");
                } else {
                    Attributes.clear(component, EXPOSE_KEY);
                }
            } else {
                Attributes.set(component, EXPOSE_KEY, selection.stream()
                        .map(PString::of).collect(PArray.collector()));
            }
        }

    }

}
