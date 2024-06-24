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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import javax.swing.SwingUtilities;
import org.netbeans.api.actions.Openable;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.spi.FileHandler;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.model.RootProxy;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.Value;
import org.praxislive.core.types.PError;
import org.praxislive.ide.core.api.CallExecutionException;
import org.praxislive.ide.project.api.ExecutionElement;
import org.praxislive.project.GraphBuilder;
import org.praxislive.project.GraphModel;

/**
 *
 */
public class PXRFileHandler implements FileHandler {

    private static final RequestProcessor RP = new RequestProcessor();

    private final PraxisProject project;
    private final PXRDataObject source;
    private final List<String> warnings;

    private Callback callback;

    public PXRFileHandler(PraxisProject project, PXRDataObject source) {
        if (project == null || source == null) {
            throw new NullPointerException();
        }
        this.project = project;
        this.source = source;
        warnings = new ArrayList<>();
    }

    @Override
    public void process(final Callback callback) throws Exception {
        if (callback == null) {
            throw new NullPointerException();
        }
        this.callback = callback;
        this.warnings.clear();

        RootProxy root = PXRRootRegistry.findRootForFile(source.getPrimaryFile());
        if (root != null) {
            // already built
            callback.onReturn(List.of());
            return;
        }

        RP.execute(() -> {
            try {
                String script = source.getPrimaryFile().asText();
                URI context = project.getProjectDirectory().toURI();
                GraphModel model = GraphModel.parse(context, script);
                SwingUtilities.invokeLater(() -> {
                    build(context, model);
                });
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
                SwingUtilities.invokeLater(() -> {
                    callback.onError(List.of(PError.of(ex)));
                });
            }
        });
    }

    @Override
    public List<String> warnings() {
        return warnings;
    }

    private void build(URI context, GraphModel fileModel) {
        PXRHelper helper = project.getLookup().lookup(PXRHelper.class);
        if (helper == null) {
            throw new IllegalStateException("No PXRHelper found");
        }
        GraphModel model = AttrUtils.rewriteAttr(fileModel);
        GraphModel root = model.withTransform(r -> {
            r.clearChildren();
            r.clearConnections();
        });
        String rootID = model.root().id();
        ComponentAddress rootAddress = ComponentAddress.of("/" + rootID);
        GraphBuilder.Root subBuilder = GraphBuilder.syntheticRoot();
        model.root().children().forEach(subBuilder::child);
        model.root().connections().forEach(subBuilder::connection);
        GraphModel sub = GraphModel.of(subBuilder.build(), context);

        Openable open = source.getLookup().lookup(Openable.class);
        if (open != null) {
            open.open();
        }

        helper.safeEval(context, root.writeToString())
                .exceptionally(this::handleException)
                .thenCompose(r -> helper.componentInfo(rootAddress))
                .thenAccept(info -> {
                    assert EventQueue.isDispatchThread();
                    project.getLookup().lookup(PXRRootRegistry.class)
                            .register(new PXRRootProxy(project, helper, source, rootID,
                                    model.root().type(), info));
                })
                .thenCompose(r -> helper.safeContextEval(context, rootAddress, sub.writeToString()))
                .exceptionally(this::handleException)
                .thenRun(() -> {
                    if (warnings.isEmpty()) {
                        callback.onReturn(List.of());
                    } else {
                        callback.onError(List.of());
                    }
                });

    }

    private List<Value> handleException(Throwable ex) {
        if (ex instanceof CompletionException ce) {
            return handleException(ce.getCause());
        }
        if (ex instanceof CallExecutionException err) {
            warnings.addAll(err.error().message().lines().toList());
            return List.of(err.error());
        } else {
            return List.of();
        }
    }

    @ServiceProvider(service = FileHandler.Provider.class)
    public static class Provider implements FileHandler.Provider {

        @Override
        public Optional<FileHandler> createHandler(PraxisProject project, ExecutionLevel level,
                ExecutionElement.File element) {
            var file = element.file();
            if (file.hasExt("pxr")) {
                if (level != ExecutionLevel.BUILD) {
                    throw new IllegalArgumentException("PXR files must be in build level");
                }
                try {
                    DataObject dob = DataObject.find(file);
                    if (dob instanceof PXRDataObject) {
                        return Optional.of(new PXRFileHandler(project, (PXRDataObject) dob));
                    }
                } catch (DataObjectNotFoundException ex) {
                    // fall through
                }

            }
            return Optional.empty();

        }
    }
}
