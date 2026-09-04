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

import java.util.List;
import org.netbeans.api.queries.VisibilityQuery;
import org.praxislive.ide.project.api.PraxisProject;
import org.openide.filesystems.FileObject;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.praxislive.ide.project.spi.ui.ProjectNodeDecorator;

// @TODO stop hiding config / show project file?
class PraxisFolderChildren extends FilterNode.Children {

    private final static String CONFIG_FOLDER_NAME = "config";
    private final static String PXP_EXT = "pxp";
    private final static Node[] EMPTY_NODES = new Node[0];

    private final PraxisProject project;
    private final List<? extends ProjectNodeDecorator> decorators;

    public PraxisFolderChildren(PraxisProject project, Node original) {
        super(original);
        this.project = project;
        this.decorators = project.getLookup()
                .lookupAll(ProjectNodeDecorator.class)
                .stream()
                .toList();
    }

    @Override
    protected Node[] createNodes(Node original) {
        FileObject fob = original.getLookup().lookup(FileObject.class);

        if (fob != null) {

            if (!VisibilityQuery.getDefault().isVisible(fob)) {
                return EMPTY_NODES;
            }

            Node projectNode;
            if (fob.isFolder()) {
                // hide config
                if (CONFIG_FOLDER_NAME.equals(fob.getName())
                        && fob.getParent().equals(project.getProjectDirectory())) {
                    return EMPTY_NODES;
                }
                projectNode = new PraxisFolderNode(project, original);
            } else {
                if (fob.hasExt(PXP_EXT)) {
                    return EMPTY_NODES;
                }
                projectNode = new PraxisFileNode(project, original);
            }
            Node result = decorators.stream()
                    .flatMap(d -> d.decorate(projectNode).stream())
                    .findFirst()
                    .orElse(projectNode);
            return new Node[]{result};
        }
        return EMPTY_NODES;
    }

}
