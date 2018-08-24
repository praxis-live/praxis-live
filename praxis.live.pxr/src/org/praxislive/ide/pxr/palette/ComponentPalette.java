/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2018 Neil C Smith.
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
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.praxislive.core.services.ComponentFactory;
import org.praxislive.core.ComponentType;
import org.praxislive.ide.components.api.Components;
import org.praxislive.ide.core.api.DynamicFileSystem;
import org.netbeans.spi.palette.PaletteController;
import org.netbeans.spi.palette.PaletteFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.XMLFileSystem;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.xml.sax.SAXException;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
public class ComponentPalette {

    private final static Logger LOG = Logger.getLogger(ComponentPalette.class.getName());

    final static String FOLDER = "PXR/Palette/";

    private final static ComponentPalette INSTANCE = new ComponentPalette();

    private final FileSystem memoryFS;
    private final FileSystem layer;

    private ComponentPalette() {
        memoryFS = FileUtil.createMemoryFileSystem();
        layer = init();
        if (layer != null) {
            DynamicFileSystem.getDefault().mount(layer);
        }
    }

    private FileSystem init() {
        Map<String, Map<ComponentType, ComponentFactory.MetaData<?>>> core
                = new TreeMap<>();
        Map<String, Map<ComponentType, ComponentFactory.MetaData<?>>> others
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
            Map<String, Map<ComponentType, ComponentFactory.MetaData<?>>> core,
            Map<String, Map<ComponentType, ComponentFactory.MetaData<?>>> others) {
        ComponentType[] types = Components.getComponentTypes();
        for (ComponentType type : types) {
            String str = type.toString();
            ComponentFactory.MetaData<?> data = Components.getMetaData(type);
            str = str.substring(0, str.lastIndexOf(':'));
            boolean cr = str.startsWith("core");
            Map<ComponentType, ComponentFactory.MetaData<?>> children = cr ? core.get(str) : others.get(str);
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
            Map<String, Map<ComponentType, ComponentFactory.MetaData<?>>> map,
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

    public PaletteController createPalette(String ... categories) {
        DataFolder paletteFolder = 
                DataFolder.findFolder(FileUtil.getConfigFile(FOLDER));
        Node rootNode = new PaletteFilterNode(paletteFolder.getNodeDelegate());
        return PaletteFactory.createPalette(rootNode,
                new DefaultPaletteActions(),
                new DefaultPaletteFilter(categories.clone()),
                null);
    }
    

    public static ComponentPalette getDefault() {
        return INSTANCE;
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
}
