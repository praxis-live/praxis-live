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
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.ArgumentFormatException;
import net.neilcsmith.praxis.core.info.ArgumentInfo;
import net.neilcsmith.praxis.core.syntax.Token;
import net.neilcsmith.praxis.core.syntax.Tokenizer;
import net.neilcsmith.praxis.core.types.*;
import net.neilcsmith.praxis.live.pxr.SyntaxUtils;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class ResourceEditor extends PraxisPropertyEditorSupport
        implements SubCommandEditor, ExPropertyEditor {

    private final static Logger LOG = Logger.getLogger(ResourceEditor.class.getName());
    private PropertyEnv env;
    private URI base;
    private boolean allowEmpty;
    private List<String> suggested;

    public ResourceEditor(PraxisProperty property, ArgumentInfo info) {
        Object dir = property.getValue("workingDir");
        if (dir instanceof File) {
            base = ((File) dir).toURI();
        } else {
            base = new File("").toURI();
        }
        PMap props = info.getProperties();
        allowEmpty = props.getBoolean(ArgumentInfo.KEY_ALLOW_EMPTY, false);
        Argument arg = props.get(ArgumentInfo.KEY_SUGGESTED_VALUES);
        if (arg != null) {
            try {
                PArray arr = PArray.coerce(arg);
                suggested = new ArrayList<String>(arr.getSize());
                for (Argument val : arr) {
                    suggested.add(val.toString());
                }
                 property.setValue("canEditAsText", Boolean.TRUE);
            } catch (ArgumentFormatException ex) {
                // no op
            }
        }
    }


    @Override
    public String getPraxisInitializationString() {

        URI uri = getURI();
        if (uri == null) {
            return "{}";
        } else {
            uri = base.relativize(uri);
            if (uri.isAbsolute()) {
                return uri.toString();
            } else {
                return "[file " + SyntaxUtils.escapeQuoted(uri.getPath()) + "]";
            }
        }

    }

    private URI getURI() {
        try {
            Argument arg = (Argument) getValue();
            if (arg.isEmpty()) {
                return null;
            } else {
                return PResource.coerce(arg).value();
            }
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        return "Resource Editor";
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        String val = text.trim();
        if (val.isEmpty()) {
            if (allowEmpty) {
                setValue(PString.EMPTY);
            } else {
                throw new IllegalArgumentException("Property doesn't support empty value");
            }
        } else {
            try {
                setValue(PResource.valueOf(val));
            } catch (ArgumentFormatException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }

    @Override
    public void setFromCommand(String command) throws Exception {
        Iterator<Token> toks = new Tokenizer(command).iterator();
        Token cmd = toks.next();
        Token file = toks.next();
        if (cmd.getType() != Token.Type.PLAIN || !"file".equals(cmd.getText())) {
            throw new IllegalArgumentException("Not file command");
        }
        switch (file.getType()) {
            case PLAIN:
            case QUOTED:
            case BRACED:
                URI path = base.resolve(new URI(null, null, file.getText(), null));
                LOG.log(Level.FINE, "Setting path to {0}", path);
                setValue(PResource.valueOf(path));
                break;
            default:
                throw new IllegalArgumentException("Couldn't parse file");
        }
    }

    @Override
    public String[] getTags() {
        if (suggested != null) {
            return suggested.toArray(new String[suggested.size()]);
        } else {
            return null;
        }
    }
    
    

    @Override
    public String[] getSupportedCommands() {
        return new String[]{"file"};
    }

    @Override
    public void attachEnv(PropertyEnv env) {
        this.env = env;
    }

    @Override
    public boolean supportsCustomEditor() {
        return env != null;
    }

    @Override
    public Component getCustomEditor() {
        return new ResourceCustomEditor(this, base, getURI(), env);
    }
}
