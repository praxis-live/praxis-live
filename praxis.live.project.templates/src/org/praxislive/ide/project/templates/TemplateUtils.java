/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2021 Neil C Smith.
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
package org.praxislive.ide.project.templates;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Objects;
import java.util.prefs.Preferences;
import java.util.stream.Stream;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.XMLFileSystem;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.praxislive.ide.core.api.IDE;
import org.praxislive.ide.core.api.DynamicFileSystem;

/**
 *
 */
class TemplateUtils {

    static final RequestProcessor RP = new RequestProcessor("Templates");

    static final String KEY_EXAMPLES_LINK = "project-templates-link";
    static final String KEY_EXAMPLES_LINK_INSTALLED = "project-templates-link-installed";

    static final String LAYER_FILE_NAME = "layer.xml";
    static final String EXAMPLE_FILE_NAME = "templates.zip";
    static final String CONFIG_PATH = "Templates";
    static final String CONFIG_LAYER_PATH = CONFIG_PATH + "/" + LAYER_FILE_NAME;
    static final String CONFIG_ZIP_PATH = CONFIG_PATH + "/" + EXAMPLE_FILE_NAME;
    static final String ZIP_PATH_ATTRIBUTE = "zipPath";

    private static final Preferences info = IDE.getPreferences();

    private static XMLFileSystem layer;

    private TemplateUtils() {
    }

    static boolean isInstalled() {
        return ((!info.get(KEY_EXAMPLES_LINK_INSTALLED, "").isEmpty())
                && FileUtil.getConfigFile(CONFIG_ZIP_PATH) != null);
    }

    static boolean isLatest() {
        return Objects.equals(info.get(KEY_EXAMPLES_LINK, ""),
                info.get(KEY_EXAMPLES_LINK_INSTALLED, ""));
    }

    static boolean canInstall() {
        return !info.get(KEY_EXAMPLES_LINK, "").isEmpty();
    }

    static void install() throws IOException {

        FileObject layerFile = FileUtil.getConfigFile(CONFIG_LAYER_PATH);
        if (layerFile != null) {
            layerFile.delete();
        }

        if (layer != null) {
            DynamicFileSystem.getDefault().unmount(layer);
            layer = null;
        }

        if (!isInstalled() || !isLatest()) {
            downloadZip();
        }

        mountLayer();

        info.put(KEY_EXAMPLES_LINK_INSTALLED, info.get(KEY_EXAMPLES_LINK, ""));

    }

    static FileObject downloadZip() throws IOException {
        FileObject zip = FileUtil.getConfigFile(CONFIG_ZIP_PATH);
        if (zip != null) {
            zip.delete();
        }

        FileObject folder = FileUtil.createFolder(FileUtil.getConfigRoot(), CONFIG_PATH);
        zip = FileUtil.createData(folder, EXAMPLE_FILE_NAME);

        try (InputStream in = new URL(info.get(KEY_EXAMPLES_LINK, "")).openStream();
                OutputStream out = zip.getOutputStream()) {
            FileUtil.copy(in, out);
        } catch (IOException ex) {
            info.remove(KEY_EXAMPLES_LINK_INSTALLED);
            zip.delete();
            throw ex;
        }

        return zip;

    }

    static void mountLayer() {
        DynamicFileSystem dfs = DynamicFileSystem.getDefault();
        if (layer != null) {
            dfs.unmount(layer);
            layer = null;
        }

        FileObject layerFile = findLayerFile();

        if (layerFile != null) {
            try {
                layer = new XMLFileSystem(layerFile.toURL());
                dfs.mount(layer);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }

    }

    static FileObject findLayerFile() {

        FileObject layerFile = FileUtil.getConfigFile(CONFIG_LAYER_PATH);

        if (layerFile != null) {
            return layerFile;
        }

        FileObject examplesZip = FileUtil.getConfigFile(CONFIG_ZIP_PATH);

        if (examplesZip != null) {
            return writeLayerFile(examplesZip);
        }

        return null;
    }

    private static FileObject writeLayerFile(FileObject examplesZip) {

        try {
            FileObject exampleRoot = FileUtil.getArchiveRoot(examplesZip);
            FileObject[] children = exampleRoot.getChildren();
            if (children.length == 0) {
                return null;
            } else if (children.length == 1) {
                if (children[0].isFolder()) {
                    exampleRoot = children[0];
                } else {
                    return null;
                }
            }
            StringBuilder sb = new StringBuilder();
            writeLayerPrefix(sb);
            writeFolderProjects(sb, exampleRoot, 1000);
            writeLayerSuffix(sb);
            FileObject layerFile
                    = FileUtil.createData(FileUtil.getConfigFile(CONFIG_PATH), "layer.xml");
            try (OutputStreamWriter out = new OutputStreamWriter(layerFile.getOutputStream())) {
                out.append(sb);
            }
            return layerFile;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }

    private static void writeFolderProjects(StringBuilder sb, FileObject folder, int position) {
        String[] children = Stream.of(folder.getChildren())
                .map(FileObject::getNameExt)
                .sorted()
                .toArray(String[]::new);
        for (String child : children) {
            FileObject file = folder.getFileObject(child);
            if (file.isFolder()) {
                if (file.getFileObject("project.pxp") != null) {
                    writeTemplateFile(sb, file);
                } else {
                    var folderName = file.getName();
                    sb.append("<folder name=\"").append(folderName).append("\">");
                    if (!"PraxisCORE".equals(folderName)) {
                        sb.append("<attr name=\"position\" intvalue=\"").append(position).append("\"/>\n");
                        position += 100;
                    }
                    writeFolderProjects(sb, file, 0);
                    sb.append("</folder>");
                }
            }
        }
    }

    private static void writeLayerPrefix(StringBuilder sb) {
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE filesystem PUBLIC \"-//NetBeans//DTD Filesystem 1.2//EN\" \"http://www.netbeans.org/dtds/filesystem-1_2.dtd\">\n");
        sb.append("<filesystem>\n");
        sb.append("<folder name=\"Templates\">\n");
        sb.append("<folder name=\"Project\">\n");
    }

    private static void writeTemplateFile(StringBuilder sb, FileObject project) {
        String path = project.getPath();
        sb.append("<file name=\"").append(project.getName()).append("\">\n");
        sb.append("<attr name=\"instantiatingIterator\" newvalue=\"org.praxislive.ide.project.templates.TemplateProjectWizardIterator\"/>\n");
        sb.append("<attr name=\"template\" boolvalue=\"true\"/>\n");
        sb.append("<attr name=\"SystemFileSystem.icon\" urlvalue=\"nbresloc:/org/praxislive/ide/project/resources/pxp16.png\"/>\n");
        sb.append("<attr name=\"zipPath\" stringvalue=\"").append(path).append("\"/>\n");
        sb.append("</file>\n");
    }

    private static void writeLayerSuffix(StringBuilder sb) {
        sb.append("</folder>\n"); // Project
        sb.append("</folder>\n"); // Templates
        sb.append("</filesystem>");
    }

}
