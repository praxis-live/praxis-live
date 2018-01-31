/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2016 Neil C Smith.
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
package org.praxislive.ide.core;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.prefs.Preferences;
import org.praxislive.ide.core.api.CoreInfo;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@NbBundle.Messages({
    "LINK_ReleaseProperties=http://www.praxislive.org/release.properties"
})
public class Core extends CoreInfo {

    public final static String KEY_LATEST_BUILD = "latest-build";
    
    private static final Core INSTANCE = new Core();

    private final static RequestProcessor RP = new RequestProcessor(Core.class);

    private final static Preferences GLOBALS = NbPreferences.root().node("/org/praxislive");
    private final static Preferences INTERNAL = NbPreferences.forModule(Core.class);

    private String version;
    private String build;

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getBuildVersion() {
        return build;
    }

    public String getLatestBuild() {
        return GLOBALS.get(KEY_LATEST_BUILD, getBuildVersion());
    }
    
    @Override
    public Preferences getPreferences() {
        return GLOBALS;
    }
    
    public Preferences getInternalPreferences() {
        return INTERNAL;
    }
    
    void setVersion(String version) {
        this.version = version;
    }

    void setBuildVersion(String build) {
        this.build = build;
    }

    void checkForUpdates() {
        if (INTERNAL.getBoolean("check-for-updates", true)
                && !Boolean.getBoolean("praxislive.start.suppresscheck")) {
            RP.post(new UpdateCheck());
        }
    }

    public static Core getInstance() {
        return INSTANCE;
    }

    private static class UpdateCheck implements Runnable {

        @Override
        public void run() {
            Properties releaseProperties = new Properties();
            try (InputStreamReader reader = new InputStreamReader(new URL(Bundle.LINK_ReleaseProperties()).openStream())) {
                releaseProperties.load(reader);
                for (String key : releaseProperties.stringPropertyNames()) {
                    String value = releaseProperties.getProperty(key);
                    if (value == null || value.isEmpty()) {
                        GLOBALS.remove(key);
                    } else {
                        GLOBALS.put(key, value);
                    }
                }
            } catch (MalformedURLException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

    }
}
