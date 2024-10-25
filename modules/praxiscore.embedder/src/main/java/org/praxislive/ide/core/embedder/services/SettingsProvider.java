/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2024 Neil C Smith.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 * 
 * 
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.core.embedder.services;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.openide.util.lookup.ServiceProvider;
import org.praxislive.core.Settings;

@ServiceProvider(service = Settings.Provider.class)
public class SettingsProvider extends Settings.Provider {

    private final static Preferences PREFS = Preferences.userRoot().node("org/praxislive/launcher");
    private final static Logger LOGGER = Logger.getLogger(SettingsProvider.class.getName());
    private final static String SYS_PREFIX = "praxis.";
    private final Map<String, String> map = new ConcurrentHashMap<>();

    public SettingsProvider() {
        initPersisted();
        initSystemProperties();
    }

    private void initPersisted() {
        try {
            for (String key : PREFS.keys()) {
                String val = PREFS.get(key, null);
                map.put(key, val);
                LOGGER.log(Level.FINE, "Persisted setting, key : {0}, value : {1}", new Object[]{key, val});
            }
        } catch (BackingStoreException ex) {
            LOGGER.log(Level.WARNING, "Couldn't access persisted preferences.", ex);
        }
    }

    private void initSystemProperties() {
        try {
            Properties sys = System.getProperties();
            for (String key : sys.stringPropertyNames()) {
                if (key.startsWith(SYS_PREFIX)) {
                    String val = sys.getProperty(key);
                    if (val == null) {
                        continue;
                    }
                    key = key.substring(SYS_PREFIX.length());
                    if (key.isEmpty()) {
                        LOGGER.log(Level.FINE, "Found key equal to prefix - ignoring");
                        continue;
                    }
                    String old = map.put(key, val);
                    if (old == null) {
                        LOGGER.log(Level.FINE, "Runtime setting, key : {0}, value : {1}", new Object[]{key, val});
                    } else {
                        LOGGER.log(Level.FINE, "Runtime override, key : {0}, old value : {1}, new value : {2}", new Object[]{key, old, val});
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Couldn't access system properties.", ex);
        }

    }

    @Override
    public String get(String key) {
        return map.get(key);
    }

    @Override
    public void put(String key, String value, boolean persistent) {
        if (key == null) {
            throw new NullPointerException();
        }
        if (persistent) {
            try {
                if (value == null) {
                    PREFS.remove(key);
                } else {
                    PREFS.put(key, value);
                }
                PREFS.flush();
            } catch (BackingStoreException ex) {
                LOGGER.log(Level.WARNING, "Couldn't persist setting.", ex);
            }
        }
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
        
    }

    @Override
    public boolean isPersistent(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
