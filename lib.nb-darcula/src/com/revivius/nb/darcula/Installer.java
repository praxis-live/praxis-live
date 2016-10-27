package com.revivius.nb.darcula;

import com.bulenkov.darcula.DarculaLaf;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.UIManager;
import org.openide.modules.ModuleInstall;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 * Makes Darcula LAF available in preferred LAF combo, installs
 * DarculaLFCustoms, set Preferences and switch the editor color profile to
 * Darcula theme.
 *
 * @author Revivius
 */
public class Installer extends ModuleInstall {

    private static final String COLOR_MODEL_CLASS_NAME = "org.netbeans.modules.options.colors.ColorModel";

    private static boolean SWITCH_EDITOR_COLORS = false;

    @Override
    public void validate() throws IllegalStateException {
        Preferences prefs = NbPreferences.root().node("laf");
        if (!prefs.getBoolean("darcula.installed", false)) {
            prefs.put("laf", DarculaLaf.class.getName());
            SWITCH_EDITOR_COLORS = true;
        }
        prefs.putBoolean("darcula.installed", true);

        // to make LAF available in Tools > Options > Appearance > Look and Feel
        UIManager.installLookAndFeel(new UIManager.LookAndFeelInfo(DarculaLaf.NAME, DarculaLaf.class.getName()));
        UIManager.put("Nb.DarculaLFCustoms", new DarculaLFCustoms());
    }

    @Override
    public void restored() {
        if (SWITCH_EDITOR_COLORS) {
            WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
                @Override
                public void run() {
                    switchEditorColorsProfile();
                }
            });
        };
    }

    /**
     * Returns if possible to change color profile. Use reflection to
     * instantiate ColorModel (private package) class and get the current
     * profile.
     *
     * @return {@code true} if current profile not equals this theme profile
     * name or {@code false} otherwise.
     */
    private boolean isChangeEditorColorsPossible() {
        ClassLoader loader = Lookup.getDefault().lookup(ClassLoader.class);
        if (loader == null) {
            loader = Installer.class.getClassLoader();
        }
        try {
            Class claszz = loader.loadClass(COLOR_MODEL_CLASS_NAME);
            Object colorModel = claszz.newInstance();
            Method method = claszz.getDeclaredMethod("getCurrentProfile", new Class[0]);
            Object invokeResult = method.invoke(colorModel, new Object[0]);
            return invokeResult != null && !DarculaLaf.NAME.equals(invokeResult);
        } catch (Exception ex) {
            //ignore
            Logger.getLogger(Installer.class.getName()).log(Level.INFO, "Cannot get the current editor colors profile.", ex);
        }
        return false;
    }

    /**
     * Switch the editor color profile if possible. Use reflection to
     * instantiate ColorModel (private package) class and set the current
     * profile
     */
    private void switchEditorColorsProfile() {
        if (!isChangeEditorColorsPossible()) {
            return;
        }

        ClassLoader loader = Lookup.getDefault().lookup(ClassLoader.class);
        if (loader == null) {
            loader = Installer.class.getClassLoader();
        }
        try {
            Class classz = loader.loadClass(COLOR_MODEL_CLASS_NAME);
            Object colorModel = classz.newInstance();
            Method method = classz.getDeclaredMethod("setCurrentProfile", String.class);
            method.invoke(colorModel, DarculaLaf.NAME);

            // method call above changes the token colors but not annotation
            // colors. these two seems to solve the problem
            method = classz.getDeclaredMethod("getAnnotations", String.class);
            Object acs = method.invoke(colorModel, DarculaLaf.NAME);

            method = classz.getDeclaredMethod("setAnnotations", String.class, Collection.class);
            method.invoke(colorModel, DarculaLaf.NAME, acs);
        } catch (Exception ex) {
            //ignore
            Logger.getLogger(Installer.class.getName()).log(Level.INFO, "Cannot change editors colors profile.", ex);
        }
    }
}
