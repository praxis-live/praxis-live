package net.neilcsmith.praxis.live.pxr.palette;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.ComponentFactory;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.live.components.api.Components;
import net.neilcsmith.praxis.live.core.api.DynamicFileSystem;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.XMLFileSystem;
import org.openide.util.Exceptions;
import org.xml.sax.SAXException;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
public class ComponentPalette {

    private final static Logger LOG = Logger.getLogger(ComponentPalette.class.getName());

    private final static String FOLDER = "PXR/Palette/";

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
        TreeMap<String, TreeMap<ComponentType, ComponentFactory.MetaData<?>>> core
                = new TreeMap<>();
        TreeMap<String, TreeMap<ComponentType, ComponentFactory.MetaData<?>>> others
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
            TreeMap<String, TreeMap<ComponentType, ComponentFactory.MetaData<?>>> core,
            TreeMap<String, TreeMap<ComponentType, ComponentFactory.MetaData<?>>> others) {
        ComponentType[] types = Components.getComponentTypes();
        for (ComponentType type : types) {
            String str = type.toString();
            ComponentFactory.MetaData<?> data = Components.getMetaData(type);
            str = str.substring(0, str.lastIndexOf(':'));
            boolean cr = str.startsWith("core");
            TreeMap<ComponentType, ComponentFactory.MetaData<?>> children = cr ? core.get(str) : others.get(str);
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
    }

    private void buildLayerPrefix(StringBuilder sb) {
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE filesystem PUBLIC \"-//NetBeans//DTD Filesystem 1.2//EN\" \"http://www.netbeans.org/dtds/filesystem-1_2.dtd\">\n");
        sb.append("<filesystem>\n");
        sb.append("<folder name=\"PXR\">\n");
        sb.append("<folder name=\"Palette\">\n");
    }

    private void writeMap(StringBuilder sb,
            TreeMap<String, TreeMap<ComponentType, ComponentFactory.MetaData<?>>> map,
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
        sb.append("<attr name=\"displayName\" stringvalue=\"")
                .append(type.toString()).append("\"/>\n");
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
        return fileName.replace(":", "__");
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

    public String getFolderPath() {
        return FOLDER;
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
