/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2026 Neil C Smith.
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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.project.ui;

import org.praxislive.ide.project.api.PraxisProject;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.PathFinder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public class PraxisLogicalViewProvider implements LogicalViewProvider {

    private final PraxisProject project;

    public PraxisLogicalViewProvider(PraxisProject project) {
        this.project = project;
    }

    @Override
    public Node createLogicalView() {
        try {
            DataObject dob = DataObject.find(project.getProjectDirectory());
            Node originalRoot = dob.getNodeDelegate();
            return new PraxisProjectNode(project, originalRoot);
        } catch (Exception ex) {
            return new AbstractNode(Children.LEAF);
        }
    }

    @Override
    public Node findPath(Node root, Object target) {
        FileObject file = switch (target) {
            case FileObject f ->
                f;
            case DataObject dob ->
                dob.getPrimaryFile();
            default ->
                null;
        };
        if (file == null || !FileUtil.isParentOf(project.getProjectDirectory(), file)) {
            return null;
        }
        return findPathImpl(root, file);
    }

    private Node findPathImpl(Node node, FileObject file) {
        PathFinder pathFinder = node.getLookup().lookup(PathFinder.class);
        if (pathFinder != null) {
            return pathFinder.findPath(node, file);
        }
        FileObject nodeFile = node.getLookup().lookup(FileObject.class);
        if (file.equals(nodeFile)) {
            return node;
        } else if (nodeFile != null && FileUtil.isParentOf(nodeFile, file)) {
            for (Node child : node.getChildren().getNodes(true)) {
                Node result = findPathImpl(child, file);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

}
