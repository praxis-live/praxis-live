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
package org.praxislive.ide.pxr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PMap;
import org.praxislive.core.types.PString;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.project.api.ProjectProperties;
import org.praxislive.project.GraphElement;
import org.praxislive.project.GraphModel;
import org.praxislive.project.ParseException;
import org.praxislive.project.SyntaxUtils;

@NbBundle.Messages({
    "CTL_SaveAsTemplateAction=Save as Template..."
})
@ActionID(
        category = "PXR", id = "org.praxislive.ide.pxr.SaveAsTemplateAction"
)
@ActionRegistration(
        displayName = "#CTL_SaveAsTemplateAction"
)
public final class SaveAsTemplateAction implements ActionListener {

    private static final String SHARED_CODE = "shared-code";

    private static final RequestProcessor RP = new RequestProcessor();

    private final PXRDataObject rootDOB;

    public SaveAsTemplateAction(PXRDataObject context) {
        this.rootDOB = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        Project project = FileOwnerQuery.getOwner(rootDOB.getPrimaryFile());
        ProjectProperties props = project == null ? null : project.getLookup().lookup(ProjectProperties.class);
        List<URI> libs = props == null ? List.of() : props.getLibraries();
        SaveAsTemplateWizard wizard = new SaveAsTemplateWizard(rootDOB.getPrimaryFile().getName(), libs);
        if (wizard.display() == WizardDescriptor.FINISH_OPTION) {
            FileObject destination = wizard.getDestination();
            String filename = wizard.getFileName();
            PArray exportLibs = wizard.getExportLibraries();
            PXRRootProxy active = PXRRootRegistry.findRootForFile(rootDOB.getPrimaryFile());
            if (active != null) {
                Task.run(SaveTask.createSaveTask(Set.of(rootDOB)))
                        .thenRunAsync(() -> copyTemplate(destination, filename, exportLibs), RP);
            } else {
                RP.execute(() -> copyTemplate(destination, filename, exportLibs));
            }
        }
    }

    private void copyTemplate(FileObject destination, String filename, PArray libs) {
        try {
            String fileText = rootDOB.getPrimaryFile().asText();
            GraphModel model = GraphModel.parse(fileText);
            if (!libs.isEmpty()) {
                model = model.withTransform(r -> r.command("libraries "
                        + SyntaxUtils.valueToToken(libs)));
            }
            if (model.root().properties().get("shared-code") instanceof GraphElement.Property prop
                    && prop.hasCommand()) {
                PMap sources = PMap.EMPTY;
                Project project = FileOwnerQuery.getOwner(rootDOB.getPrimaryFile());
                if (project != null) {
                    FileObject sourceFolder = project.getProjectDirectory().getFileObject("code/" + rootDOB.getName());
                    if (sourceFolder != null) {
                        sources = readSources(sourceFolder.toURI());
                    }
                }
                GraphElement.Property replacement = GraphElement.property(sources);
                model = model.withTransform(r -> r.transformProperties(props
                        -> props.map(p -> {
                            if (SHARED_CODE.equals(p.getKey())) {
                                return Map.entry(p.getKey(), replacement);
                            } else {
                                return p;
                            }
                        }).toList()));
            }
            String templateContents = model.writeToString();
            FileObject template = FileUtil.createData(destination, filename);
            try (OutputStreamWriter writer = new OutputStreamWriter(template.getOutputStream())) {
                writer.append(templateContents);
            }
            template.setAttribute("template", true);
            template.setAttribute("displayName", template.getName());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (ParseException ex) {
            throw new IllegalStateException(ex);
        }

    }

    // adapted from SourcesSupport so can be executed outside of running project
    private static PMap readSources(URI base) throws IOException {
        Path baseDir = Path.of(base);
        try (Stream<Path> files = Files.walk(baseDir)) {
            Map<String, PString> sourceMap = new TreeMap<>();
            List<Path> sourceFiles = files
                    .filter(p -> p.toString().endsWith(".java") && Files.isRegularFile(p))
                    .toList();
            for (Path source : sourceFiles) {
                String binaryName = base.relativize(source.toUri()).toString();
                binaryName = binaryName.substring(0, binaryName.lastIndexOf("."));
                binaryName = binaryName.replace("/", ".");
                sourceMap.put(binaryName, PString.of(Files.readString(source)));
            }
            return PMap.ofMap(sourceMap);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

}
