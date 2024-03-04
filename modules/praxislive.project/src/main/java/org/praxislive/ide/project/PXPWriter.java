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
package org.praxislive.ide.project;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.praxislive.ide.project.api.ExecutionElement;
import org.praxislive.ide.project.spi.ElementHandler;
import org.praxislive.ide.project.spi.LineHandler;

/**
 *
 */
class PXPWriter {

    private final FileObject projectDir;
    private final FileObject projectFile;
    private final ProjectPropertiesImpl props;

    private PXPWriter(FileObject projectDir, FileObject projectFile,
            ProjectPropertiesImpl props) {
        this.projectDir = projectDir;
        this.projectFile = projectFile;
        this.props = props;
    }

    private void write() throws IOException {
        var elements = props.elements();
        try (Writer writer = new OutputStreamWriter(projectFile.getOutputStream())) {
            for (var e : elements.get(ExecutionLevel.CONFIGURE)) {
                writeElement(writer, e.element(), e.handler());
            }
            writeLevel(writer, ExecutionLevel.BUILD);
            for (var e : elements.get(ExecutionLevel.BUILD)) {
                writeElement(writer, e.element(), e.handler());
            }
            writeLevel(writer, ExecutionLevel.RUN);
            for (var e : elements.get(ExecutionLevel.RUN)) {
                writeElement(writer, e.element(), e.handler());
            }
        }
    }

//    private void writeConfig(Writer writer) throws IOException {
//        writer.write(PXPReader.JAVA_RELEASE_CMD);
//        writer.write(" ");
//        writer.write(String.valueOf(props.getJavaRelease()));
//        writer.write("\n");
//        writer.write(DefaultPraxisProject.LIBS_COMMAND);
//        writer.write("\n");
//    }
    private void writeLevel(Writer writer, ExecutionLevel level) throws IOException {
        writer.write("\n# ");
        if (level == ExecutionLevel.BUILD) {
            writer.write(PXPReader.BUILD_LEVEL_SWITCH);
        } else {
            writer.write(PXPReader.RUN_LEVEL_SWITCH);
        }
        writer.write("\n");
    }

    private void writeElement(Writer writer, ExecutionElement element,
            ElementHandler handler) throws IOException {
        if (element instanceof ExecutionElement.File) {
            writeFile(writer, ((ExecutionElement.File) element).file());
        } else if (element instanceof ExecutionElement.Line
                && handler instanceof LineHandler) {
            var line = ((ExecutionElement.Line) element).line();
            line = ((LineHandler) handler).rewrite(line);
            writer.write(line);
            writer.write("\n");
        }
    }

    private void writeFile(Writer writer, FileObject file) throws IOException {
        String path = FileUtil.getRelativePath(projectDir, file);
        if (path == null) {
            path = file.getPath();
        }
        writer.write(PXPReader.INCLUDE_CMD);
        writer.write(" [");
        writer.write(PXPReader.FILE_CMD);
        writer.write(" \"");
        writer.write(path);
        writer.write("\"]\n");
    }

    static void writeProjectProperties(FileObject projectDir,
            FileObject file,
            ProjectPropertiesImpl props) throws IOException {
        new PXPWriter(projectDir, file, props).write();
    }

}
