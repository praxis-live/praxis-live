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
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.praxislive.core.services.ComponentFactory;
import org.praxislive.core.ComponentType;
import org.praxislive.ide.components.api.Components;
import org.netbeans.spi.palette.PaletteController;
import org.netbeans.spi.palette.PaletteFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.MultiFileSystem;
import org.openide.filesystems.XMLFileSystem;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.praxislive.ide.project.api.PraxisProject;
import org.xml.sax.SAXException;

/**
 *
 */
public class ComponentPalette {

    private final static Logger LOG = Logger.getLogger(ComponentPalette.class.getName());

    private final static WeakHashMap<PraxisProject, ComponentPalette> CACHE
            = new WeakHashMap<>();

    final static String FOLDER = "PXR/Palette/";

    private final PraxisProject project;
    private final Components components;
    private final FileSystem paletteFS;

    private ComponentPalette(PraxisProject project) {
        this.project = project;
        var cmp = project.getLookup().lookup(Components.class);
        if (cmp != null) {
            this.components = cmp;
        } else {
            this.components = new FallbackComponents();
        }
        var layer = init();
        FileSystem fs;
        try {
            fs = new MultiFileSystem(FileUtil.getSystemConfigRoot().getFileSystem(), layer);
        } catch (FileStateInvalidException ex) {
            Exceptions.printStackTrace(ex);
            fs = new MultiFileSystem(layer);
        }
        paletteFS = fs;
    }

    private FileSystem init() {
        Map<String, Map<ComponentType, Lookup>> core
                = new TreeMap<>();
        Map<String, Map<ComponentType, Lookup>> others
                = new TreeMap<>();
        buildMaps(core, others);

        StringBuilder sb = new StringBuilder();
        buildLayerPrefix(sb);
        writeMap(sb, core, -100000);
        writeMap(sb, others, 100000);
        buildLayerSuffix(sb);

        LOG.log(Level.FINE, "Created ComponentPalette dynamic layer\n{0}", sb);

        FileObject memLayer = writeLayer(sb);
        if (memLayer != null) {
            try {
                return new XMLFileSystem(memLayer.toURL());
            } catch (SAXException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        LOG.warning("Unable to create dynamic component palette");
        return null;
    }

    private void buildMaps(
            Map<String, Map<ComponentType, Lookup>> core,
            Map<String, Map<ComponentType, Lookup>> others) {
        var types = components.componentTypes();
        for (ComponentType type : types) {
            String str = type.toString();
            Lookup data = components.metaData(type);
            str = str.substring(0, str.lastIndexOf(':'));
            boolean cr = str.startsWith("core");
            Map<ComponentType, Lookup> children = cr ? core.get(str) : others.get(str);
            if (children == null) {
                children = new TreeMap<>(TypeComparator.INSTANCE);
                if (cr) {
                    core.put(str, children);
                } else {
                    others.put(str, children);
                }
            }
            children.put(type, data);
        }
        core.putIfAbsent("core:custom", Collections.emptyMap());

        String[] knownCustom = new String[]{
            "audio:custom",
            "data:custom",
            "tinkerforge:custom",
            "video:custom",
            "video:gl:custom"
        };

        for (String folder : knownCustom) {
            others.putIfAbsent(folder, Collections.emptyMap());
        }

    }

    private void buildLayerPrefix(StringBuilder sb) {
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE filesystem PUBLIC \"-//NetBeans//DTD Filesystem 1.2//EN\" \"http://www.netbeans.org/dtds/filesystem-1_2.dtd\">\n");
        sb.append("<filesystem>\n");
        sb.append("<folder name=\"PXR\">\n");
        sb.append("<folder name=\"Palette\">\n");
    }

    private void writeMap(StringBuilder sb,
            Map<String, Map<ComponentType, Lookup>> map,
            int position) {
        for (String category : map.keySet()) {
            startCategoryFolder(sb, category, position);
            for (ComponentType type : map.get(category).keySet()) {
                buildTypeFile(sb, type);
            }
            endCategoryFolder(sb);
            position += 10;
        }
    }

    private void startCategoryFolder(StringBuilder sb, String category, int position) {
        sb.append("<folder name=\"").append(safeFileName(category)).append("\">\n");
        sb.append("<attr name=\"displayName\" stringvalue=\"")
                .append(category).append("\"/>\n");
        sb.append("<attr name=\"position\" intvalue=\"")
                .append(position).append("\"/>\n");
    }

    private void buildTypeFile(StringBuilder sb, ComponentType type) {
        sb.append("<file name=\"")
                .append(safeFileName(type.toString()))
                .append(".type\">\n");
//        sb.append("<attr name=\"displayName\" stringvalue=\"")
//                .append(type.toString()).append("\"/>\n");
        sb.append("<attr name=\"").append(TypeDataObject.TYPE_ATTR_KEY)
                .append("\" stringvalue=\"")
                .append(type).append("\"/>\n");
        sb.append("</file>\n");
    }

    private void endCategoryFolder(StringBuilder sb) {
        sb.append("</folder>\n");
    }

    private void buildLayerSuffix(StringBuilder sb) {
        sb.append("</folder>\n"); // Palette
        sb.append("</folder>\n"); // PXR
        sb.append("</filesystem>");
    }

    private String safeFileName(String fileName) {
        return fileName.replace(":", "_");
    }

    private FileObject writeLayer(StringBuilder sb) {
        OutputStreamWriter writer = null;
        FileObject file = null;
        try {
            var memoryFS = FileUtil.createMemoryFileSystem();
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

    public static PaletteController getPalette(PraxisProject project, String... categories) {
        ComponentPalette palette = CACHE.computeIfAbsent(project, ComponentPalette::new);
        DataFolder paletteFolder
                = DataFolder.findFolder(palette.paletteFS.findResource(FOLDER));
        Node rootNode = new PaletteFilterNode(paletteFolder.getNodeDelegate());
        return PaletteFactory.createPalette(rootNode,
                new DefaultPaletteActions(),
                new DefaultPaletteFilter(palette.components, List.of(categories)),
                null);
    }

    private static class TypeComparator implements Comparator<ComponentType> {

        private final static TypeComparator INSTANCE = new TypeComparator();

        private TypeComparator() {
        }

        @Override
        public int compare(ComponentType type1, ComponentType type2) {

            return type1.toString().compareTo(type2.toString());

        }
    }

    private static class FallbackComponents implements Components {

        @Override
        public List<ComponentType> componentTypes() {
            return List.of();
        }

        @Override
        public List<ComponentType> rootTypes() {
            return List.of();
        }

        @Override
        public Lookup metaData(ComponentType type) {
            return null;
        }

    }
}
