/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.live.pxr.api.ComponentProxy;
import net.neilcsmith.praxis.live.pxr.api.Connection;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import net.neilcsmith.praxis.live.pxr.api.PraxisPropertyEditor;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
//import org.openide.util.RequestProcessor;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class PXRWriter {

    private final static Logger LOG = Logger.getLogger(PXRWriter.class.getName());
    final static String INDENT = "  ";
    final static String AT = "@";
    final static String CONNECT = "~";
    private final RootProxy root;
    private final ContainerProxy container;
    private final Set<String> children;

    private PXRWriter(RootProxy root) {
        this.root = root;
        container = null;
        children = null;
    }

    private PXRWriter(ContainerProxy container, Set<String> children) {
        this.container = container;
        this.children = children;
        this.root = null;
    }
    
    private void doWrite(Appendable target) throws IOException {
        if (root != null) {
            // full graph
            writeComponent(target, root, 0);
        } else {
            // sub graph
            /// attributes?
            writeChildren(target, container, 0);
            writeConnections(target, container, 0);
        }
    }

    private void writeComponent(Appendable sb, ComponentProxy cmp, int level) throws IOException {
        LOG.finest("Writing component " + cmp.getAddress());
        writeIndent(sb, level);
        sb.append(AT).append(' ');
        writeComponentID(sb, cmp);
        sb.append(' ').append(cmp.getType().toString()).append(" {\n");
        writeAttributes(sb, cmp, level + 1);
        writeProperties(sb, cmp, level + 1);
        if (cmp instanceof PXRContainerProxy) {
            writeChildren(sb, (PXRContainerProxy) cmp, level + 1);
            writeConnections(sb, (PXRContainerProxy) cmp, level + 1);
        }
        writeIndent(sb, level);
        sb.append("}\n");
    }

    private void writeComponentID(Appendable sb, ComponentProxy cmp) throws IOException {
        if (cmp instanceof RootProxy) {
            sb.append(cmp.getAddress().toString());
        } else {
            ComponentAddress ad = cmp.getAddress();
            String id = ad.getComponentID(ad.getDepth() - 1);
            sb.append("./");
            sb.append(id);
        }
    }

    private void writeAttributes(Appendable sb, ComponentProxy cmp, int level) throws IOException {
        String[] keys = cmp.getAttributeKeys();
        for (String key : keys) {
            writeIndent(sb, level);
            writeAttribute(sb, key, cmp.getAttribute(key));
        }
    }

    private void writeAttribute(Appendable sb, String key, String value) throws IOException {
        LOG.log(Level.FINEST, "Writing attribute {0} : {1}", new Object[]{key, value});
        sb.append("#%").append(key).append(' ');
        sb.append(SyntaxUtils.escape(value)).append('\n');
    }

    private void writeProperties(Appendable sb, ComponentProxy cmp, int level) {
        String[] propIDs = cmp.getPropertyIDs();
        for (String id : propIDs) {
            try {
                LOG.log(Level.FINEST, "Checking property {0}", id);
                PraxisProperty prop = cmp.getProperty(id);
                if (!prop.canWrite()) {
                    continue;
                }
                if (prop.supportsDefaultValue() && prop.isDefaultValue()) {
                    continue;
                }
                if (prop.isTransient()) {
                    LOG.log(Level.FINEST, "Property is transient {0}", id);
                    continue;
                }
                LOG.log(Level.FINEST, "Writing property {0}", id);
                PraxisPropertyEditor editor = prop.getPropertyEditor();
                String code = editor.getPraxisInitializationString();
                if (code == null || code.isEmpty()) {
                    LOG.log(Level.FINEST, "No code returned from editor for {0}", id);
                    continue;
                }
                writeIndent(sb, level);
                writeProperty(sb, id, code);
            } catch (Exception e) {
                // continue ???
                LOG.log(Level.INFO, "Error writing property.", e);
            }
        }
    }

    private void writeProperty(Appendable sb, String id, String code) throws IOException {
        sb.append('.').append(id).append(' ').append(code).append('\n');
    }

    private void writeChildren(Appendable sb, ContainerProxy container, int level) throws IOException {
        String[] childIDs = container.getChildIDs();
        for (String id : childIDs) {
            if (level == 0 && children != null && !children.contains(id)) {
                LOG.log(Level.FINEST, "Skipping child : {0}", id);
                continue;
            }
            writeComponent(sb, container.getChild(id), level);
        }
    }

    private void writeConnections(Appendable sb, ContainerProxy container, int level) throws IOException {
        Connection[] connections = container.getConnections();
        for (Connection connection : connections) {
            String c1 = connection.getChild1();
            String c2 = connection.getChild2();
            String p1 = connection.getPort1();
            String p2 = connection.getPort2();
            if (level == 0 && children != null && 
                    !(children.contains(c1) && children.contains(c2))) {
                LOG.log(Level.FINEST, "Skipping connection : {0}", connection);
                continue;
            }
            writeIndent(sb, level);
            writeConnection(sb, c1, p1, c2, p2);
        }
    }

    private void writeConnection(Appendable sb, String c1, String p1,
            String c2, String p2) throws IOException {
        sb.append(CONNECT).append(' ');
        sb.append("./").append(c1).append('!').append(p1).append(' ');
        sb.append("./").append(c2).append('!').append(p2).append('\n');
    }

    private void writeIndent(Appendable sb, int level) throws IOException {
        for (int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
    }

    static void write(RootProxy root, Appendable target) throws IOException {
        new PXRWriter(root).doWrite(target);
    }

    static void writeSubGraph(ContainerProxy container, 
            Set<String> children,
            Appendable target) throws IOException {
        if (children.isEmpty()) {
            return;
        }
        new PXRWriter(container, children).doWrite(target);
    }
    
    
}
