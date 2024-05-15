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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.project;

import org.openide.filesystems.FileUtil;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import org.praxislive.core.syntax.Token;
import org.praxislive.core.syntax.Tokenizer;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.openide.filesystems.FileObject;
import org.praxislive.ide.project.api.ExecutionElement;

import static org.praxislive.core.syntax.Token.Type.*;

/**
 *
 */
class PXPReader {

    final static String INCLUDE_CMD = "include";
    final static String JAVA_RELEASE_CMD = "java-compiler-release";
    final static String ADD_LIBS_CMD = "add-libs";
    final static String FILE_CMD = "file";
    final static String BUILD_LEVEL_SWITCH = "<<<BUILD>>>";
    final static String RUN_LEVEL_SWITCH = "<<<RUN>>>";

    private final FileObject projectDir;
    private final FileObject data;
    private final ProjectPropertiesImpl props;
    private final EnumMap<ExecutionLevel, List<ExecutionElement>> elements;

    private ExecutionLevel level;
//    private boolean libsAdded;

    private PXPReader(FileObject projectDir,
            FileObject data, ProjectPropertiesImpl props) {
        this.projectDir = projectDir;
        this.data = data;
        this.props = props;
        this.level = ExecutionLevel.CONFIGURE;
        elements = new EnumMap<>(ExecutionLevel.class);
        for (ExecutionLevel l : ExecutionLevel.values()) {
            elements.put(l, new ArrayList<>());
        }
    }

    private void parse() throws Exception {

        String script = loadFile(data);
        Iterator<Token> tokens = new Tokenizer(script).iterator();
        List<Token> line = new ArrayList<>();

        while (tokens.hasNext()) {
            line.clear();
            tokensToEOL(tokens, line);
            if (line.isEmpty()) {
                continue;
            }
            Token first = line.get(0);
            switch (first.getType()) {
                case PLAIN:
                    // @TODO allow other token types at start?
                    parseCommand(script, line);
                    break;
                case COMMENT:
                    parseComment(line);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected token");

            }
        }

        props.initElements(elements);

    }

    private String loadFile(FileObject data) throws IOException {
        return data.asText();
    }

    private void parseComment(List<Token> tokens) {
        String text = tokens.get(0).getText();
        if (text.contains(BUILD_LEVEL_SWITCH)) {
            switchLevel(ExecutionLevel.BUILD);
        } else if (text.contains(RUN_LEVEL_SWITCH)) {
            switchLevel(ExecutionLevel.RUN);
        }
    }

    private void switchLevel(ExecutionLevel level) {
        if (level.compareTo(this.level) < 0) {
            throw new IllegalArgumentException("Can't move level down");
        }
        this.level = level;
    }

    private void parseCommand(String script, List<Token> tokens) throws Exception {
        String command = tokens.get(0).getText();
        if (INCLUDE_CMD.equals(command)) {
            parseInclude(tokens);
        } else {
            String line = script.substring(tokens.get(0).getStartIndex(), 
                        tokens.get(tokens.size() - 1).getEndIndex());
            elements.get(level).add(ExecutionElement.forLine(line));
        }
    }

    private void parseInclude(List<Token> tokens) throws Exception {
        if (tokens.size() != 2) {
            throw new IllegalArgumentException("Unexpected number of arguments in include command");
        }
        if (tokens.get(1).getType() == Token.Type.SUBCOMMAND) {
            FileObject file = parseFileCommand(tokens.get(1).getText());
            ExecutionElement el = ExecutionElement.forFile(file);
            elements.get(level).add(el);
        }
    }

    private FileObject parseFileCommand(String command) throws Exception {
        List<Token> line = new ArrayList<>();
        tokensToEOL(new Tokenizer(command).iterator(), line);
        if (line.size() == 2 && FILE_CMD.equals(line.get(0).getText())) {
            URI base = FileUtil.toFile(projectDir).toURI();
            URI path = base.resolve(new URI(null, null, line.get(1).getText(), null));
            return FileUtil.toFileObject(new File(path));
        }
        throw new IllegalArgumentException("Invalid file in include line : " + command);
    }

//    private void parseAddLibs(Token[] tokens) throws Exception {
//        if (libsAdded) {
//            throw new IllegalArgumentException("add-libs command already found");
//        }
//        if (level != null) {
//            throw new IllegalArgumentException("add-libs command found after level switch");
//        }
//        libsAdded = true;
//    }
//    
//    private void parseJavaRelease(Token[] tokens) throws Exception {
//        if (tokens.length != 2) {
//            throw new IllegalArgumentException("Unexpected number of arguments in java-compiler-release command");
//        }
//        try {
//            int release = Integer.parseInt(tokens[1].getText());
//            props.setJavaRelease(release);
//        } catch (Exception ex) {
//            // fall through?
//        }
//        
//    }

    private static void tokensToEOL(Iterator<Token> tokens, List<Token> line) {
        while (tokens.hasNext()) {
            Token t = tokens.next();
            if (t.getType() == EOL) {
                break;
            }
            line.add(t);
        }
    }

    static void initializeProjectProperties(FileObject projectDir,
            FileObject file,
            ProjectPropertiesImpl props) throws Exception {
        new PXPReader(projectDir, file, props).parse();
    }

}
