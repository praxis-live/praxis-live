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

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.swing.Action;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.Mode;
import org.openide.windows.ModeSelector;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.praxislive.ide.core.api.IDE;

/**
 *
 */
public final class EditorModes {

    public enum Configuration {
        Default, Always, Never
    };

    private static final String KEY_CONFIGURATION = "pxr-editor-tab-grouping";
    private static final String SPLIT_ON_OPEN = "split-on-open";
    private static final EditorModes INSTANCE = new EditorModes();
    private static final int DEFAULT_SPLIT_WIDTH = 1600;

    private Configuration activeConfiguration;

    private EditorModes() {
        TopComponent.getRegistry().addPropertyChangeListener(this::registryPropertyChange);
        activeConfiguration = Configuration.Default;
        String config = IDE.getPreferences().get(KEY_CONFIGURATION, null);
        if (config != null) {
            try {
                activeConfiguration = Configuration.valueOf(config);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    public Configuration getActiveConfiguration() {
        return activeConfiguration;
    }

    public void setActiveConfiguration(Configuration configuration) {
        activeConfiguration = Objects.requireNonNull(configuration);
        if (configuration == Configuration.Default) {
            IDE.getPreferences().remove(KEY_CONFIGURATION);
        } else {
            IDE.getPreferences().put(KEY_CONFIGURATION, configuration.name());
        }
    }

    private Mode selectModeImpl(TopComponent tc, Mode mode) {
        if (!WindowManager.getDefault().isEditorMode(mode) || !isFileEditor(tc)) {
            return mode;
        }
        boolean rootEditor = isRootEditor(tc);
        if (rootEditor && !containsNoneRootEditors(mode)) {
            return mode;
        } else if (!rootEditor && !containsOnlyRootEditors(mode)) {
            return mode;
        }
        List<Mode> editorModes = findEditorModes();
        if (!editorModes.contains(mode)) {
            return mode;
        }
        if (editorModes.size() == 1) {
            TopComponent active = mode.getSelectedTopComponent();
            tc.putClientProperty(SPLIT_ON_OPEN, active);
            return mode;
        } else if (rootEditor) {
            return editorModes.stream()
                    .filter(this::containsOnlyRootEditors)
                    .findFirst().orElse(mode);
        } else {
            return editorModes.stream()
                    .filter(Predicate.not(this::containsOnlyRootEditors))
                    .findFirst().orElse(mode);
        }
    }

    private boolean isFileEditor(TopComponent tc) {
        return tc.getLookup().lookup(DataObject.class) != null;
    }

    private List<Mode> findEditorModes() {
        return WindowManager.getDefault().getModes().stream()
                .filter(WindowManager.getDefault()::isEditorMode)
                .map(Mode.class::cast)
                .toList();
    }

    private boolean isRootEditor(TopComponent tc) {
        return tc instanceof RootEditorTopComponent;
    }

    private boolean containsNoneRootEditors(Mode mode) {
        return Stream.of(mode.getTopComponents())
                .filter(this::isFileEditor)
                .anyMatch(Predicate.not(this::isRootEditor));
    }

    private boolean containsOnlyRootEditors(Mode mode) {
        return Stream.of(mode.getTopComponents())
                .filter(this::isFileEditor)
                .allMatch(this::isRootEditor);
    }

    private void registryPropertyChange(PropertyChangeEvent evt) {
        if (TopComponent.Registry.PROP_ACTIVATED.equals(evt.getPropertyName())) {
            TopComponent activated = TopComponent.getRegistry().getActivated();
            if (activated.getClientProperty(SPLIT_ON_OPEN) instanceof TopComponent previous) {
                activated.putClientProperty(SPLIT_ON_OPEN, null);
                if (findEditorModes().size() > 1) {
                    return;
                }
                if (activeConfiguration == Configuration.Never) {
                    return;
                }
                if (activeConfiguration == Configuration.Default) {
                    Frame main = WindowManager.getDefault().getMainWindow();
                    if (main.getSize().width < DEFAULT_SPLIT_WIDTH) {
                        return;
                    }
                }
                for (Action a : activated.getActions()) {
                    if (a == null) {
                        continue;
                    }
                    if ("NewTabGroupAction".equals(a.getClass().getSimpleName())) {
                        a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
                        previous.requestVisible();
                        break;
                    }
                }
            }
        }
    }

    public static EditorModes getDefault() {
        return INSTANCE;
    }

    @ServiceProvider(service = ModeSelector.class)
    public static final class Selector implements ModeSelector {

        @Override
        public Mode selectModeForOpen(TopComponent tc, Mode mode) {
            return EditorModes.getDefault().selectModeImpl(tc, mode);
        }

    }

}
