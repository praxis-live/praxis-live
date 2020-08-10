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
package org.praxislive.ide.laf;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.prefs.Preferences;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import org.openide.modules.ModuleInstall;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 */
public class Installer extends ModuleInstall {

    private static final String COLOR_MODEL_CLASS_NAME =
            "org.netbeans.modules.options.colors.ColorModel"; //NOI18N

    private static boolean switchColors = false;

    @Override
    public void validate() throws IllegalStateException {
        Preferences prefs = NbPreferences.root().node("laf");
        if (prefs.get("laf", "").isBlank()) {
            prefs.put("laf", "com.formdev.flatlaf.FlatDarkLaf");
            switchColors = true;
        }

        System.setProperty("netbeans.ps.hideSingleExpansion", "true");
        System.setProperty("ps.quickSearch.disabled.global", "true");
    }

    @Override
    public void restored() {
        WindowManager.getDefault().invokeWhenUIReady(this::configureUI);

    }

    private void configureUI() {
        var defs = UIManager.getDefaults();
        var font = defs.getFont("Label.font");
        if (font != null) {
            int size = font.getSize();
            defs.put("netbeans.ps.rowheight", size * 2);
        }

        if (switchColors) {
            switchEditorColorsProfile();
        }

        WindowManager wm = WindowManager.getDefault();
        TopComponent tc = wm.findTopComponent("projectTabLogical_tc");
        tc.setIcon(ImageUtilities.loadImage("org/netbeans/modules/project/ui/resources/projectTab.png", true));
        tc = wm.findTopComponent("CommonPalette");
        tc.setIcon(ImageUtilities.loadImage("org/netbeans/modules/palette/resources/palette.png", true));
    }

    private boolean isChangeEditorColorsPossible() {
        String preferredProfile = getPreferredColorProfile();
        ClassLoader loader = Lookup.getDefault().lookup(ClassLoader.class);
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        try {
            Class clazz = loader.loadClass(COLOR_MODEL_CLASS_NAME);
            Object colorModel = clazz.getDeclaredConstructor().newInstance();
            Method method = clazz.getDeclaredMethod("getCurrentProfile", new Class[0]);
            Object invokeResult = method.invoke(colorModel, new Object[0]);
            return invokeResult != null && !preferredProfile.equals(invokeResult);
        } catch (Exception ex) {
            //ignore
            System.getLogger(Installer.class.getName()).log(System.Logger.Level.INFO,
                    "Cannot get the current editor colors profile.", ex);
        }
        return false;
    }

    private void switchEditorColorsProfile() {
        if (!isChangeEditorColorsPossible()) {
            return;
        }

        String preferredProfile = getPreferredColorProfile();

        ClassLoader loader = Lookup.getDefault().lookup(ClassLoader.class);
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        try {
            Class clazz = loader.loadClass(COLOR_MODEL_CLASS_NAME);
            Object colorModel = clazz.getDeclaredConstructor().newInstance();
            Method method = clazz.getDeclaredMethod("setCurrentProfile", String.class);
            method.invoke(colorModel, preferredProfile);

            // method call above changes the token colors but not annotation
            // colors. these two seems to solve the problem
            method = clazz.getDeclaredMethod("getAnnotations", String.class);
            Object acs = method.invoke(colorModel, preferredProfile);

            method = clazz.getDeclaredMethod("setAnnotations", String.class, Collection.class);
            method.invoke(colorModel, preferredProfile, acs);
        } catch (Exception ex) {
            //ignore
            System.getLogger(Installer.class.getName()).log(System.Logger.Level.INFO,
                    "Cannot change editors colors profile.", ex);
        }
    }

    private String getPreferredColorProfile() {
        String className = NbPreferences.root().node("laf").get("laf", null);
        if (null == className) {
            return null;
        }

        ClassLoader loader = Lookup.getDefault().lookup(ClassLoader.class);
        if (null == loader) {
            loader = ClassLoader.getSystemClassLoader();
        }

        try {
            Class clazz = loader.loadClass(className);
            LookAndFeel laf = (LookAndFeel) clazz.getDeclaredConstructor().newInstance();
            return laf.getDefaults().getString("nb.preferred.color.profile"); //NOI18N
        } catch (Exception e) {
            //ignore
        }
        return null;
    }

}
