/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2022 Neil C Smith.
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

package org.praxislive.ide.pxj;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.filesystems.FileObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

@NbBundle.Messages({
    "LBL_changeBaseTypeAction=Change Base Type",
    "LBL_baseType=Base type (empty is default)",
    "TITLE_baseType=Set base type"
})
@ActionID(id = "org.praxislive.ide.pxj.ChangeBaseTypeAction", category = "Other")
@ActionRegistration(displayName = "#LBL_changeBaseTypeAction", lazy = false)
@ActionReferences({
    @ActionReference(path = "Editors/text/x-java/Popup", position = 1000)
})
public class ChangeBaseTypeAction extends AbstractAction implements ContextAwareAction {

    private final PXJDataObject pxjDob;
    
    public ChangeBaseTypeAction() {
        pxjDob = null;
        setEnabled(false);
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
    }

    public ChangeBaseTypeAction(PXJDataObject pxjDob) {
        super(Bundle.LBL_changeBaseTypeAction());
        this.pxjDob = pxjDob;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        assert pxjDob != null;
        if (pxjDob == null) {
            return;
        }
        String baseType = pxjDob.getBaseType();
        if (baseType == null) {
            baseType = "";
        }
        var input = new NotifyDescriptor.InputLine(Bundle.LBL_baseType(), 
                Bundle.TITLE_baseType());
        input.setInputText(baseType);
        var result = DialogDisplayer.getDefault().notify(input);
        if (result == NotifyDescriptor.OK_OPTION) {
            pxjDob.setBaseType(input.getInputText().strip());
        }
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        FileObject file = actionContext.lookup(FileObject.class);
        if (file != null) {
            var attr = file.getAttribute(PXJDataObject.PXJ_DOB_KEY);
            if (attr instanceof PXJDataObject) {
                return new ChangeBaseTypeAction((PXJDataObject) attr);
            }
        }
        return this;
    }

}
