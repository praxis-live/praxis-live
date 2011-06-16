/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr.editors;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.neilcsmith.praxis.core.info.ArgumentInfo;
import net.neilcsmith.praxis.core.syntax.Token;
import net.neilcsmith.praxis.core.syntax.Tokenizer;
import net.neilcsmith.praxis.core.types.PArray;
import net.neilcsmith.praxis.core.types.PResource;
import net.neilcsmith.praxis.live.pxr.SyntaxUtils;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class FileListEditor extends PraxisPropertyEditorSupport
        implements SubCommandEditor, ExPropertyEditor {

    private URI base;
    private File directory;
    private boolean setExternally;
    private PropertyEnv env;

    public FileListEditor(PraxisProperty<?> property, ArgumentInfo info) {
        Object dir = property.getValue("workingDir");
        if (dir instanceof File) {
            base = ((File) dir).toURI();
        } else {
            base = new File("").toURI();
        }
    }

    @Override
    public void setValue(Object value) {
        if (value == getValue()) {
            return;
        }
        super.setValue(value);
        setExternally = true;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        throw new IllegalArgumentException();
    }

    @Override
    public String getAsText() {
        return null;
    }

    @Override
    public boolean isPaintable() {
        return true;
    }

    @Override
    public void paintValue(Graphics gfx, Rectangle box) {
        if (setExternally) {
            gfx.setFont(gfx.getFont().deriveFont(Font.ITALIC));
        }
        FontMetrics fm = gfx.getFontMetrics();
        gfx.drawString("[file-list ... ]", box.x, box.y
                + (box.height - fm.getHeight()) / 2 + fm.getAscent());
    }

    @Override
    public boolean supportsCustomEditor() {
        return env != null;
    }

    @Override
    public Component getCustomEditor() {
        return new FileListCustomEditor(this, base, directory, env);
    }

    @Override
    public void attachEnv(PropertyEnv env) {
        this.env = env;
    }

    @Override
    public String getPraxisInitializationString() {

        if (directory == null) {
            return "{}";
        }

        URI uri = directory.toURI();
        uri = base.relativize(uri);
        String dirCode;
        if (uri.isAbsolute()) {
            dirCode = uri.toString();
        } else {
            dirCode = "[file " + SyntaxUtils.escapeQuoted(uri.getPath()) + "]";
        }
        return "[file-list " + dirCode + "]";

    }

    @Override
    public String[] getSupportedCommands() {
        return new String[]{"file-list"};
    }

    void setDirectory(File dir) throws Exception {
        super.setValue(buildFileList(dir));
        directory = dir;
        setExternally = false;
    }

    @Override
    public void setFromCommand(String command) throws Exception {
        Iterator<Token> tokens = new Tokenizer(command).iterator();
        Token cmd = tokens.next();
        if (cmd.getType() != Token.Type.PLAIN || !"file-list".equals(cmd.getText())) {
            throw new IllegalArgumentException("Not file-list command");
        }
        Token loc = tokens.next();
        File dir;
        switch (loc.getType()) {
            case SUBCOMMAND:
                dir = parseRelativeDirectory(loc);
                break;
            case PLAIN:
            case QUOTED:
            case BRACED:
                dir = parseAbsoluteDirectory(loc);
                break;
            default:
                throw new IllegalArgumentException();
        }
        super.setValue(buildFileList(dir));
        setExternally = false;
        directory = dir;
    }

    private File parseAbsoluteDirectory(Token loc) throws Exception {
        URI uri = new URI(loc.getText());
        if (uri.isAbsolute()) {
            return new File(uri);
        } else {
            throw new IllegalArgumentException("URI is not an absolute address");
        }
    }

    private File parseRelativeDirectory(Token loc) throws Exception {
        Iterator<Token> tokens = new Tokenizer(loc.getText()).iterator();
        Token cmd = tokens.next();
        if (cmd.getType() != Token.Type.PLAIN || !"file".equals(cmd.getText())) {
            throw new IllegalArgumentException("Invalid subcommand in command");
        }
        Token file = tokens.next();
        switch (file.getType()) {
            case PLAIN:
            case QUOTED:
            case BRACED:
                return resolveDirectory(file);
            default:
                throw new IllegalArgumentException("Invalid token type in [file command");
        }
    }

    private PArray buildFileList(File dir) throws Exception {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("File does not refer to a directory.");
        }
        File[] files = dir.listFiles();
        List<PResource> uris = new ArrayList<PResource>();
        for (File f : files) {
            uris.add(PResource.valueOf(f.toURI()));
        }
        Collections.sort(uris);
        return PArray.valueOf(uris);
    }

    private File resolveDirectory(Token file) throws Exception {
        URI path = base.resolve(new URI(null, null, file.getText(), null));
        return new File(path);
    }
}
