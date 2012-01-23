/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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
package net.neilcsmith.praxis.live.project;

import org.openide.filesystems.FileUtil;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.neilcsmith.praxis.core.syntax.Token;
import net.neilcsmith.praxis.core.syntax.Tokenizer;
import net.neilcsmith.praxis.live.project.api.ExecutionLevel;
import net.neilcsmith.praxis.live.project.api.PraxisProjectProperties;
import org.openide.filesystems.FileObject;

import static net.neilcsmith.praxis.core.syntax.Token.Type.*;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class PXPReader {

    final static String INCLUDE_CMD = "include";
    final static String FILE_CMD = "file";
    final static String BUILD_LEVEL_SWITCH = "<<<BUILD>>>";
    final static String RUN_LEVEL_SWITCH = "<<<RUN>>>";


    private FileObject projectDir;
    private FileObject data;
    private ProjectPropertiesImpl props;
    private ExecutionLevel level;
    private List<FileObject> buildFiles;
    private List<FileObject> runFiles;

    private PXPReader(FileObject projectDir,
            FileObject data, ProjectPropertiesImpl props) {
        this.projectDir = projectDir;
        this.data = data;
        this.props = props;
        buildFiles = new ArrayList<FileObject>();
        runFiles =  new ArrayList<FileObject>();
    }

    private void parse() throws Exception {

        Iterator<Token> tokens = new Tokenizer(loadFile(data)).iterator();

        while (tokens.hasNext()) {
            Token[] line = tokensToEOL(tokens);
            if (line.length == 0) {
                continue;
            }
            Token first = line[0];
            switch (first.getType()) {
                case PLAIN:
                    parseInclude(line);
                    break;
                case COMMENT:
                    parseComment(line);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected token");

            }
        }

        props.setProjectFiles(ExecutionLevel.BUILD, buildFiles.toArray(new FileObject[buildFiles.size()]));
        props.setProjectFiles(ExecutionLevel.RUN, runFiles.toArray(new FileObject[runFiles.size()]));

    }

    private String loadFile(FileObject data) throws IOException {
        return data.asText();
    }

    private void parseComment(Token[] tokens) {
        String text = tokens[0].getText();
        if (text.contains(BUILD_LEVEL_SWITCH)) {
            switchLevel(ExecutionLevel.BUILD);
        } else if (text.contains(RUN_LEVEL_SWITCH)) {
            switchLevel(ExecutionLevel.RUN);
        }
    }

    private void switchLevel(ExecutionLevel level) {
        if (level == ExecutionLevel.BUILD && this.level == ExecutionLevel.RUN) {
            throw new IllegalArgumentException("Can't move level down");
        }
        this.level = level;
    }

    private void parseInclude(Token[] tokens) throws Exception {
        if (! INCLUDE_CMD.equals(tokens[0].getText())) {
            throw new IllegalArgumentException("Unexpected command in project file : " + tokens[0].getText());
        }
        if (tokens.length != 2) {
            throw new IllegalArgumentException("Unexpected number of arguments in include command");
        }
        if (tokens[1].getType() == Token.Type.SUBCOMMAND) {
            FileObject file = parseFileCommand(tokens[1].getText());
            if (level == null) {
                switchLevel(ExecutionLevel.RUN);
            }
            if (level == ExecutionLevel.BUILD) {
                buildFiles.add(file);
            } else {
                runFiles.add(file);
            }
        }
    }

    private FileObject parseFileCommand(String command) throws Exception {
        Token[] toks = tokensToEOL(new Tokenizer(command).iterator());
        if (toks.length == 2 && FILE_CMD.equals(toks[0].getText())) {
            URI base = FileUtil.toFile(projectDir).toURI();
            URI path = base.resolve(new URI(null, null, toks[1].getText(), null));
            return FileUtil.toFileObject(new File(path));
        }
        throw new IllegalArgumentException("Invalid file in include line : " + command);
    }


    private static Token[] tokensToEOL(Iterator<Token> tokens) {
        List<Token> tks = new ArrayList<Token>();
        while (tokens.hasNext()) {
            Token t = tokens.next();
            if (t.getType() == EOL) {
                break;
            }
            tks.add(t);
        }
        return tks.toArray(new Token[tks.size()]);
    }


    static void initializeProjectProperties(FileObject projectDir,
            FileObject file,
            ProjectPropertiesImpl props) throws Exception {
        new PXPReader(projectDir, file, props).parse();
    }

}
