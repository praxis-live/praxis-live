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
package org.praxislive.ide.pxr.palette;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.XMLFileSystem;
import org.openide.util.RequestProcessor;
import org.praxislive.ide.core.api.IDE;

/**
 *
 */
class Utils {

    static final RequestProcessor RP = new RequestProcessor("PXG");

    static final String KEY_PXG_LINK = "pxg-components-link";
    static final String KEY_PXG_LINK_INSTALLED = "pxg-components-link-installed";

    static final String PXG_FILE_NAME = "pxg.zip";
    static final String PXR_DOWNLOAD_FOLDER = "PXR/Downloads";
    static final String PXG_FILE_PATH = PXR_DOWNLOAD_FOLDER + "/" + PXG_FILE_NAME;
    static final String PALETTE_PATH = ComponentPalette.FOLDER;
    
    private final static Map<String, String> knownFolders = new HashMap<>(6);
    static {
        knownFolders.put("audio", "audio_custom");
        knownFolders.put("core", "core_custom");
        knownFolders.put("data", "data_custom");
        knownFolders.put("tinkerforge", "tinkerforge_custom");
        knownFolders.put("video", "video_custom");
        knownFolders.put("videogl", "video_gl_custom");
    }

    private static XMLFileSystem layer;

    private static Preferences info = IDE.getPreferences();

    private Utils() {
    }
    
    static boolean isInstalled() {
        return ((!info.get(KEY_PXG_LINK_INSTALLED, "").isEmpty())
                && FileUtil.getConfigFile(PXG_FILE_PATH) != null);
    }

    static boolean isLatest() {
        return Objects.equals(info.get(KEY_PXG_LINK, ""),
                info.get(KEY_PXG_LINK_INSTALLED, ""));
    }

    static boolean canInstall() {
        return !info.get(KEY_PXG_LINK, "").isEmpty();
    }

    static void install() throws IOException {

        if (!isInstalled() || !isLatest()) {
            downloadZip();
        }
        
        try {
            installToPalette();
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }

        info.put(KEY_PXG_LINK_INSTALLED, info.get(KEY_PXG_LINK, ""));

    }

    static FileObject downloadZip() throws IOException {
        FileObject zip = FileUtil.getConfigFile(PXG_FILE_PATH);
        if (zip != null) {
            zip.delete();
        }

        FileObject folder = FileUtil.createFolder(FileUtil.getConfigRoot(), PXR_DOWNLOAD_FOLDER);
        zip = FileUtil.createData(folder, PXG_FILE_NAME);

        try (InputStream in = new URL(info.get(KEY_PXG_LINK, "")).openStream();
                OutputStream out = zip.getOutputStream()) {
            FileUtil.copy(in, out);
        } catch (IOException ex) {
            info.remove(KEY_PXG_LINK_INSTALLED);
            zip.delete();
            throw ex;
        }

        return zip;

    }

    static void installToPalette() throws Exception {
        
        FileObject zip = FileUtil.getConfigFile(PXG_FILE_PATH);
        FileObject root = FileUtil.getArchiveRoot(zip);
        FileObject[] children = root.getChildren();
        if (children.length == 1 && children[0].isFolder()) {
            root = children[0];
            children = root.getChildren();
        }
        for (FileObject folder : children) {
            if (!folder.isFolder()) {
                continue;
            }
            String targetFolder = knownFolders.get(folder.getNameExt().toLowerCase());
            if (targetFolder == null) {
                continue;
            }
            FileObject dest = FileUtil.createFolder(
                    FileUtil.getConfigRoot(), PALETTE_PATH + "/" + targetFolder);
            for (FileObject file : folder.getChildren()) {
                FileObject existing = dest.getFileObject(file.getNameExt());
                if (existing != null) {
                    existing.delete();
                }
                FileUtil.copyFile(file, dest, file.getName());
            }
            
        }

    }

}
