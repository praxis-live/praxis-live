/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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
package org.praxislive.ide.pxr;

import java.awt.EventQueue;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.praxislive.core.ComponentType;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PString;
import org.praxislive.project.GraphElement;
import org.praxislive.project.GraphModel;

@Messages({
    "LBL_Subgraph_LOADER=Files of Subgraph"
})
@MIMEResolver.ExtensionRegistration(
        displayName = "#LBL_Subgraph_LOADER",
        mimeType = "text/x-praxis-subgraph",
        extension = {"pxg"})
@DataObject.Registration(
        mimeType = "text/x-praxis-subgraph",
        iconBase = "org/praxislive/ide/pxr/resources/pxg16.png",
        displayName = "#LBL_Subgraph_LOADER",
        position = 300)
@ActionReferences({
    //    @ActionReference(
    //        path = "Loaders/text/x-praxis-subgraph/Actions",
    //    id =
    //    @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
    //    position = 100,
    //    separatorAfter = 200),
    @ActionReference(
            path = "Loaders/text/x-praxis-subgraph/Actions",
            id
            = @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
            position = 300),
    @ActionReference(
            path = "Loaders/text/x-praxis-subgraph/Actions",
            id
            = @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
            position = 400,
            separatorAfter = 500),
    @ActionReference(
            path = "Loaders/text/x-praxis-subgraph/Actions",
            id
            = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
            position = 600),
    @ActionReference(
            path = "Loaders/text/x-praxis-subgraph/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
            position = 700,
            separatorAfter = 800),
    @ActionReference(
            path = "Loaders/text/x-praxis-subgraph/Actions",
            id
            = @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
            position = 1100,
            separatorAfter = 1200),
    @ActionReference(
            path = "Loaders/text/x-praxis-subgraph/Actions",
            id
            = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
            position = 1300),
    @ActionReference(
            path = "Loaders/text/x-praxis-subgraph/Actions",
            id
            = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
            position = 1400)
})
public class SubgraphDataObject extends MultiDataObject {

    public static final String KEY_ATTR_TYPES = "componentTypes";

    private static final RequestProcessor RP = new RequestProcessor();

    private final RequiredTypes requiredTypes;

    public SubgraphDataObject(FileObject file, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(file, loader);
        requiredTypes = initTypes(file);
    }

    @Override
    public Lookup getLookup() {
        return new ProxyLookup(super.getLookup(), Lookups.singleton(requiredTypes));
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    private RequiredTypes initTypes(FileObject file) {
        Object attr = file.getAttribute(KEY_ATTR_TYPES);
        if (attr instanceof String str) {
            try {
                return PArray.from(PString.of(str))
                        .map(a -> a.asListOf(ComponentType.class))
                        .map(RequiredTypes::new)
                        .orElseThrow();
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        // calculate types
        RP.execute(() -> {
            try {
                String script = file.asText();
                GraphModel model = GraphModel.parseSubgraph(file.getParent().toURI(), script);
                Set<ComponentType> types = new LinkedHashSet<>();
                extractUniqueTypes(model.root(), types);
                PArray typeArray = PArray.of(types);
                file.setAttribute(KEY_ATTR_TYPES, typeArray.toString());
                List<ComponentType> typeList = typeArray.asListOf(ComponentType.class);
                EventQueue.invokeLater(() -> {
                    requiredTypes.updateTypes(typeList);
                });
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
                try {
                    file.setAttribute(KEY_ATTR_TYPES, PArray.EMPTY.toString());
                } catch (IOException ex1) {
                    Exceptions.printStackTrace(ex1);
                }
            }
        });

        return new RequiredTypes(List.of());

    }

    private void extractUniqueTypes(GraphElement.Component component, Set<ComponentType> types) {
        component.children().values().forEach(child -> {
            types.add(child.type());
            extractUniqueTypes(child, types);
        });

    }

    public static final class RequiredTypes {

        private List<ComponentType> types;

        private RequiredTypes(List<ComponentType> types) {
            this.types = List.copyOf(types);
        }

        private void updateTypes(List<ComponentType> types) {
            this.types = List.copyOf(types);
        }

        public List<ComponentType> requiredTypes() {
            return types;
        }

    }
}
