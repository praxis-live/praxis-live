/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2015 Neil C Smith.
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
package net.neilcsmith.praxis.live.core;

import com.sun.jna.Platform;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
public class HubSettings {

    private final static Logger LOG = Logger.getLogger(HubSettings.class.getName());

    private final static HubSettings DEFAULT = new HubSettings();
    private final static Preferences PREFS = NbPreferences.forModule(HubSettings.class);
    private final static String KEY_DISTRIBUTED = "hub.distributed";
    private final static String KEY_SLAVE_INFO = "hub.slave-info";
//    private final static String KEY_LOCAL_SLAVE_CONFIG = "hub.local-slave-config";
    private final static String KEY_LOCAL_SLAVE_LAUNCHER = "hub.local-slave-launcher";
    private final static String DEFAULT_SLAVE_CONFIG = "localhost 13178 * audio true";

    private final File defaultSlaveLocation;

    private HubSettings() {
        defaultSlaveLocation = findDefaultSlaveLocation();
    }

    public void setDistributedHub(boolean distributed) {
        PREFS.putBoolean(KEY_DISTRIBUTED, distributed);
    }

    public boolean isDistributedHub() {
        return PREFS.getBoolean(KEY_DISTRIBUTED, false);
    }

    public void setLocalSlaveLauncher(File launcher) {
        if (launcher == null || launcher.equals(defaultSlaveLocation)) {
            LOG.fine("Setting to default slave launcher");
            PREFS.remove(KEY_LOCAL_SLAVE_LAUNCHER);
        } else {
            String path = FileUtil.normalizeFile(launcher).toString();
            LOG.log(Level.FINE, "Setting to custom slave launcher : {0}", path);
            PREFS.put(KEY_LOCAL_SLAVE_LAUNCHER, path);
        }
    }

    public File getLocalSlaveLauncher() {
        String custom = PREFS.get(KEY_LOCAL_SLAVE_LAUNCHER, null);
        return custom == null ? defaultSlaveLocation : new File(custom);
    }

    public void setSlaveInfo(List<HubSlaveInfo> info) {
        if (info == null || info.isEmpty()) {
            PREFS.remove(KEY_SLAVE_INFO);
        } else {
            StringBuilder sb = new StringBuilder();
            for (HubSlaveInfo sl : info) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(sl.toString());
            }
            PREFS.put(KEY_SLAVE_INFO, sb.toString());
        }
    }

    public List<HubSlaveInfo> getSlaveInfo() {
        String info = PREFS.get(KEY_SLAVE_INFO, DEFAULT_SLAVE_CONFIG);
        if (info == null || info.isEmpty()) {
            return Collections.emptyList();
        } else {
            try {
                String[] lines = info.split("[\\r?\\n]+");
                List<HubSlaveInfo> list = new ArrayList<>(lines.length);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    list.add(HubSlaveInfo.fromString(line));
                }
                return list;
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Unable to parse slave info.", ex);
                return Collections.emptyList();
            }
        }
    }

//    public void setLocalSlaves(List<LocalSlaveConfig> config) {
//        if (config == null || config.isEmpty()) {
//            PREFS.remove(KEY_LOCAL_SLAVE_CONFIG);
//        } else {
//            StringBuilder sb = new StringBuilder();
//            for (LocalSlaveConfig sl : config) {
//                if (sb.length() > 0) {
//                    sb.append('\n');
//                }
//                sb.append(sl.toString());
//            }
//            PREFS.put(KEY_LOCAL_SLAVE_CONFIG, sb.toString());
//        }
//    }
//
//    public List<LocalSlaveConfig> getLocalSlaves() {
//        String info = PREFS.get(KEY_LOCAL_SLAVE_CONFIG, null);
//        if (info == null || info.isEmpty()) {
//            return Collections.emptyList();
//        } else {
//            try {
//                String[] lines = info.split("[\\r?\\n]+");
//                List<LocalSlaveConfig> list = new ArrayList<>(lines.length);
//                for (String line : lines) {
//                    line = line.trim();
//                    if (line.isEmpty()) {
//                        continue;
//                    }
//                    list.add(LocalSlaveConfig.fromString(line));
//                }
//                return list;
//            } catch (Exception ex) {
//                LOG.log(Level.WARNING, "Unable to parse slave info.", ex);
//                return Collections.emptyList();
//            }
//        }
//    }
    
    public static HubSettings getDefault() {
        return DEFAULT;
    }

    private static File findDefaultSlaveLocation() {
        File modDir = InstalledFileLocator.getDefault()
                .locate("modules", "net.neilcsmith.praxis.live.core", false);
        if (modDir == null) {
            LOG.warning("No Praxis LIVE module directory found");
            return null;
        }
        File installDir = modDir.getParentFile().getParentFile();
        File binDir = new File(installDir, "bin");
        if (!binDir.exists()) {
            LOG.warning("/bin directory not found");
            return null;
        }
        File launcher;
        if (Platform.isWindows()) {
            if (Platform.is64Bit()) {
               launcher = new File(binDir, "praxis64.exe"); 
            } else {
               launcher = new File(binDir, "praxis.exe"); 
            }
        } else {
            launcher = new File(binDir, "praxis");
        }
        if (launcher.exists()) {
            LOG.log(Level.FINE, "Found launcher at {0}", launcher);
            return launcher;
        } else {
            LOG.warning("No launcher found in /bin");
            return null;
        }

    }

}
