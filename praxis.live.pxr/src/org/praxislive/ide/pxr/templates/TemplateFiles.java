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
package org.praxislive.ide.pxr.templates;

import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.praxislive.core.ComponentType;
import org.praxislive.ide.core.api.DynamicFileSystem;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.XMLFileSystem;
import org.openide.util.Exceptions;
import org.openide.xml.XMLUtil;
import org.xml.sax.SAXException;

/**
 *
 */
class TemplateFiles {

    private final static Logger LOG = Logger.getLogger(TemplateFiles.class.getName());
    private final static Map<String, String> display = new LinkedHashMap<>();

    static {
        display.put("root:audio", "Audio Patch");
        display.put("root:video", "Video Patch");
        display.put("root:midi", "MIDI Bindings");
        display.put("root:gui", "Control Panel (GUI)");
        display.put("root:tinkerforge", "TinkerForge Patch");
        display.put("root:osc", "OSC Bindings");
        display.put("root:data", "Generic Data Patch");
    }
    private final static String templateFolder = "Templates/Praxis/";
    private final static TemplateFiles INSTANCE = new TemplateFiles();
    private final FileSystem memoryFS;
    private final FileSystem layer;
    private final List<String> privileged;

    private TemplateFiles() {
        memoryFS = FileUtil.createMemoryFileSystem();
        privileged = new ArrayList<>();
        layer = init();
        if (layer != null) {
            DynamicFileSystem.getDefault().mount(layer);
        }
    }

    private FileSystem init() {
        StringBuilder sb = new StringBuilder();
        buildLayerPrefix(sb);
        for (String typeString : display.keySet()) {
            var type = ComponentType.of(typeString);
            String filename = fileName(type);
            buildTemplateFile(sb, type, filename);
            privileged.add(templateFolder + filename);
        }
        buildLayerSuffix(sb);
        LOG.log(Level.FINE, "Created Templates dynamic layer\n{0}", sb);
        FileObject memLayer = writeLayer(sb);
        if (memLayer != null) {
            try {
                return new XMLFileSystem(memLayer.toURL());
            } catch (SAXException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        LOG.warning("Unable to create dynamic templates");
        return null;
    }

    private FileObject writeLayer(StringBuilder sb) {
        OutputStreamWriter writer = null;
        FileObject file = null;
        try {
            file = memoryFS.getRoot().createData("layer.xml");
            writer = new OutputStreamWriter(file.getOutputStream(), "UTF-8");
            writer.append(sb);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        return file;
    }

    private void buildLayerPrefix(StringBuilder sb) {
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE filesystem PUBLIC \"-//NetBeans//DTD Filesystem 1.2//EN\" \"http://www.netbeans.org/dtds/filesystem-1_2.dtd\">\n");
        sb.append("<filesystem>\n");
        sb.append("<folder name=\"Templates\">\n");
        sb.append("<folder name=\"Praxis\">\n");
    }

    private void buildTemplateFile(StringBuilder sb, ComponentType type, String filename) {
        sb.append("<file name=\"").append(filename).append("\">\n");
        sb.append("<attr name=\"displayName\" stringvalue=\"")
                .append(displayName(type)).append("\"/>\n");
        sb.append("<attr name=\"instantiatingIterator\" newvalue=\"org.praxislive.ide.pxr.wizard.PXRWizardIterator\"/>\n");
        sb.append("<attr name=\"template\" boolvalue=\"true\"/>\n");
        sb.append("<attr name=\"templateWizardURL\" urlvalue=\"nbresloc:/org/praxislive/ide/pxr/resources/PXRinfo.html\"/>\n");
        sb.append("<attr name=\"rootType\" stringvalue=\"").append(type).append("\"/>\n");
        sb.append("</file>\n");
    }

    private void buildLayerSuffix(StringBuilder sb) {
        sb.append("</folder>\n"); // Templates
        sb.append("</folder>\n"); // Praxis
        sb.append("</filesystem>");
    }

    private String fileName(ComponentType type) {
        String str = type.toString();
        int idx = str.lastIndexOf(':');
        if (idx > -1) {
            str = str.substring(idx + 1);
        }
        return str + ".pxr";
    }

    private String displayName(ComponentType type) {
        String str = display.get(type.toString());
        if (str != null) {
            try {
                return XMLUtil.toAttributeValue(str);
            } catch (CharConversionException ex) {
                Exceptions.printStackTrace(ex);

            }
        }
        return type.toString();
    }

    PrivilegedTemplates getPrivilegedTemplates() {
        return new Privileged(privileged.toArray(new String[privileged.size()]));
    }

    private static class Privileged implements PrivilegedTemplates {

        private String[] templates;

        private Privileged(String[] templates) {
            this.templates = templates;
        }

        @Override
        public String[] getPrivilegedTemplates() {
            return templates;
        }
    }

    static TemplateFiles getDefault() {
        return INSTANCE;
    }
}
