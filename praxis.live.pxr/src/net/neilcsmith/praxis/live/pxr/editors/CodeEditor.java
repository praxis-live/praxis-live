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
import net.neilcsmith.praxis.core.info.ArgumentInfo;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class CodeEditor extends PraxisPropertyEditorSupport
        implements ExPropertyEditor {

    private final String mime;
    private PropertyEnv env;
    private String template;

    CodeEditor(PraxisProperty property, ArgumentInfo info, String mimetype) {
        this.mime = mimetype;
        this.template = info.getProperties().getString(ArgumentInfo.KEY_TEMPLATE, null);
        property.setValue("canEditAsText", Boolean.FALSE);
    }
    

    @Override
    public String getDisplayName() {
        return "Code Editor (" + mime + ")";
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
        return new CodeCustomEditor(this, env, mime, template);
    }





}
