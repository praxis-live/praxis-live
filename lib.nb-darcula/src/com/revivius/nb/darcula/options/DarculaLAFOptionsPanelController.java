/*
 * Copyright 2016 markiewb.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.revivius.nb.darcula.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.LifecycleManager;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

@OptionsPanelController.SubRegistration(
        location = "Appearance",
        displayName = "#AdvancedOption_DisplayName_DarculaLAF",
        keywords = "#AdvancedOption_Keywords_DarculaLAF",
        keywordsCategory = "Appearance/DarculaLAF"
)
@org.openide.util.NbBundle.Messages({
    "AdvancedOption_DisplayName_DarculaLAF=Darcula Look and Feel",
    "AdvancedOption_Keywords_DarculaLAF=darcula laf, dark, theme, font, laf"
})
public final class DarculaLAFOptionsPanelController extends OptionsPanelController {

    
    public static final String FONT_STRING = "font";
    public static final String OVERRIDE_FONT_BOOLEAN = "overrideFont";
    public static final String INVERT_ICONS_BOOLEAN = "invertIcons";
    public static final String STRETCHED_TABS_BOOLEAN = "stretchedTabs";

    private static final PreferenceChangeListener PREF_LISTENER = new PreferenceChangeListener() {
        @Override
        public void preferenceChange(PreferenceChangeEvent evt) {
            NotificationDisplayer.getDefault().notify(
                    "Restart IDE",
                    ImageUtilities.loadImageIcon("com/revivius/nb/darcula/options/restart.png", true),
                    "Click here to restart IDE and apply new settings.",
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            LifecycleManager.getDefault().markForRestart();
                            LifecycleManager.getDefault().exit();
                        }
                    }
            );
        }
    };
    
    static {
        NbPreferences.forModule(DarculaLAFPanel.class).addPreferenceChangeListener(PREF_LISTENER);
    }
    
    private DarculaLAFPanel panel;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean changed;

    @Override
    public void update() {
        getPanel().load();
        changed = false;
    }

    @Override
    public void applyChanges() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                getPanel().store();
                changed = false;
            }
        });
    }

    @Override
    public void cancel() {
        // need not do anything special, if no changes have been persisted yet
    }

    @Override
    public boolean isValid() {
        return getPanel().valid();
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null; // new HelpCtx("...ID") if you have a help set
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    private DarculaLAFPanel getPanel() {
        if (panel == null) {
            panel = new DarculaLAFPanel(this);
        }
        return panel;
    }

    void changed() {
        if (!changed) {
            changed = true;
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }

}
