/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2017 Neil C Smith.
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
package net.neilcsmith.praxis.live.editor.saveflash;

import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import org.netbeans.api.editor.EditorActionRegistration;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@EditorActionRegistration(
        name = "toggle-flash-on-save",
        menuPath = "View",
        menuPosition = 995,
        preferencesKey = FlashOnSaveHighlight.ENABLED_KEY,
        preferencesDefault = true
)
public class FlashOnSaveToggleAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        FlashOnSaveHighlight.enabled = 
                MimeLookup.getLookup(MimePath.EMPTY).lookup(Preferences.class)
                        .getBoolean(FlashOnSaveHighlight.ENABLED_KEY, false);
    }
    
}
