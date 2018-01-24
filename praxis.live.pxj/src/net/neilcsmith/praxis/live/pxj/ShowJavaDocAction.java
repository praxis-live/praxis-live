/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2018 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxj;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.HtmlBrowser;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Help",
        id = "net.neilcsmith.praxis.live.pxj.ShowJavaDocAction"
)
@ActionRegistration(
        displayName = "#CTL_ShowJavaDocAction"
)
@ActionReference(path = "Menu/Help", position = 225)
@Messages("CTL_ShowJavaDocAction=Show Core JavaDoc")
public final class ShowJavaDocAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            URL coreDocs = JavadocQueryImpl.JAVADOC_ARCHIVE;
            coreDocs = new URL(coreDocs, "index.html");
            HtmlBrowser.URLDisplayer.getDefault().showURLExternal(coreDocs);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }

    }
}
