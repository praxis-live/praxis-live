/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package net.neilcsmith.praxis.live.laf;

import com.nilo.plaf.nimrod.NimRODTheme;
import org.netbeans.swing.plaf.LFCustoms;
import org.netbeans.swing.plaf.util.UIBootstrapValue;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import net.neilcsmith.praxis.laf.PraxisLookAndFeel;
import org.netbeans.swing.plaf.util.UIUtils;

/** Default system-provided customizer for Metal LF
 * Public only to be accessible by ProxyLazyValue, please don't abuse.
 */
public final class PraxisLFCustoms extends LFCustoms {

    public Object[] createLookAndFeelCustomizationKeysAndValues() {
        int fontsize = 11;
        Integer in = (Integer) UIManager.get(CUSTOM_FONT_SIZE); //NOI18N
        if (in != null) {
            fontsize = in.intValue();
        }

        //XXX fetch the custom font size here instead
        Font controlFont = new Font("Dialog", Font.PLAIN, fontsize); //NOI18N
        Object[] result = {
            //The assorted standard NetBeans metal font customizations
            CONTROLFONT, controlFont,
            SYSTEMFONT, controlFont,
            USERFONT, controlFont,
            MENUFONT, controlFont,
            WINDOWTITLEFONT, controlFont,
            LISTFONT, controlFont,
            TREEFONT, controlFont,
            PANELFONT, controlFont,
            SUBFONT, new Font("Dialog", Font.PLAIN, Math.min(fontsize - 1, 6)),
            //Bug in JDK 1.5 thru b59 - pale blue is incorrectly returned for this
            "textInactiveText", Color.GRAY, //NOI18N
            // #61395        
            SPINNERFONT, controlFont,
            EDITOR_ERRORSTRIPE_SCROLLBAR_INSETS, new Insets(16, 0, 16, 0),};
        return result;
    }

    public Object[] createApplicationSpecificKeysAndValues() {
        Border outerBorder = BorderFactory.createLineBorder(UIManager.getColor("controlShadow")); //NOI18N
//        Object propertySheetColorings = new MetalPropertySheetColorings();
        Color unfocusedSelBg = UIManager.getColor("controlShadow");
        if (!Color.WHITE.equals(unfocusedSelBg.brighter())) { // #57145
            unfocusedSelBg = unfocusedSelBg.brighter();
        }


        Color borderColor = (Color) UIManager.get("InternalFrame.borderShadow");
        if (borderColor == null) {
            borderColor = new Color(144,150,162);
        }

        Object[] result = {

            "LabelUI", "net.neilcsmith.praxis.live.laf.OptionsAwareLabelUI",
            "EditorPaneUI", "net.neilcsmith.praxis.live.laf.HonorDisplayEditorPaneUI",

            DESKTOP_BORDER, new EmptyBorder(1, 1, 1, 1),
            SCROLLPANE_BORDER, new PraxisScrollPaneBorder(),
            EXPLORER_STATUS_BORDER, new PraxisStatusLineBorder(PraxisStatusLineBorder.TOP),
            EDITOR_STATUS_LEFT_BORDER, new PraxisStatusLineBorder(PraxisStatusLineBorder.TOP | PraxisStatusLineBorder.RIGHT),
            EDITOR_STATUS_RIGHT_BORDER, new PraxisStatusLineBorder(PraxisStatusLineBorder.TOP | PraxisStatusLineBorder.LEFT),
            EDITOR_STATUS_INNER_BORDER, new PraxisStatusLineBorder(PraxisStatusLineBorder.TOP | PraxisStatusLineBorder.LEFT | PraxisStatusLineBorder.RIGHT),
            EDITOR_STATUS_ONLYONEBORDER, new PraxisStatusLineBorder(PraxisStatusLineBorder.TOP),
            EDITOR_TOOLBAR_BORDER, new PraxisEditorToolbarBorder(),





////            PROPERTYSHEET_BOOTSTRAP, propertySheetColorings,
            PROPSHEET_SELECTION_BACKGROUND, PraxisLookAndFeel.getControlShadow(),
            PROPSHEET_SELECTION_FOREGROUND, PraxisLookAndFeel.getBlack(),
            PROPSHEET_SET_BACKGROUND, PraxisLookAndFeel.getControl(),
            PROPSHEET_SET_FOREGROUND, PraxisLookAndFeel.getBlack(),
            PROPSHEET_SELECTED_SET_BACKGROUND, PraxisLookAndFeel.getControlDarkShadow(),
            PROPSHEET_SELECTED_SET_FOREGROUND, PraxisLookAndFeel.getBlack(),
            PROPSHEET_DISABLED_FOREGROUND, PraxisLookAndFeel.getMenuDisabledForeground(),

//            PROPSHEET_SELECTION_BACKGROUND, UIManager.get("Table.selectionBackground"),
//            PROPSHEET_SELECTION_FOREGROUND, UIManager.get("Table.selectionForeground"),
//            PROPSHEET_SET_BACKGROUND, UIManager.get("Table.selectionBackground"),
//            PROPSHEET_SET_FOREGROUND, UIManager.get("Table.selectionForeground"),
//            PROPSHEET_SELECTED_SET_BACKGROUND, UIManager.get("Table.selectionBackground"),
//            PROPSHEET_SELECTED_SET_FOREGROUND, UIManager.get("Table.selectionForeground"),
//            PROPSHEET_DISABLED_FOREGROUND, new Color(153, 153, 153),

            "textText", PraxisLookAndFeel.getBlack(),
            TAB_SELECTION_FOREGROUND, new Color(224,224,224),
            TAB_SELECTION_BACKGROUND, new Color(24,24,24),
            TAB_ACTIVE_SELECTION_FOREGROUND, UIManager.get("Table.selectionForeground"),
            TAB_ACTIVE_SELECTION_BACKGROUND, UIManager.get("Table.selectionBackground"),



            //UI Delegates for the tab control
            //            EDITOR_TAB_DISPLAYER_UI, "org.netbeans.swing.tabcontrol.plaf.MetalEditorTabDisplayerUI",
            //            VIEW_TAB_DISPLAYER_UI, "org.netbeans.swing.tabcontrol.plaf.MetalViewTabDisplayerUI",
            //            SLIDING_BUTTON_UI, "org.netbeans.swing.tabcontrol.plaf.MetalSlidingButtonUI",
            EDITOR_TAB_DISPLAYER_UI, "net.neilcsmith.praxis.live.laf.tabs.PraxisEditorTabDisplayerUI",
            VIEW_TAB_DISPLAYER_UI, "net.neilcsmith.praxis.live.laf.tabs.PraxisViewTabDisplayerUI",
            SLIDING_BUTTON_UI, "net.neilcsmith.praxis.live.laf.tabs.PraxisSlidingButtonUI",
//            EDITOR_TAB_OUTER_BORDER, outerBorder,
//            VIEW_TAB_OUTER_BORDER, outerBorder,
            EXPLORER_MINISTATUSBAR_BORDER, BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("controlShadow")),

            VIEW_TAB_OUTER_BORDER, BorderFactory.createEmptyBorder(),
            VIEW_TAB_TABS_BORDER, BorderFactory.createEmptyBorder(),
            VIEW_TAB_CONTENT_BORDER, BorderFactory.createMatteBorder(0,1,1,1,borderColor),
            EDITOR_TAB_OUTER_BORDER, BorderFactory.createEmptyBorder(),
            EDITOR_TAB_CONTENT_BORDER, BorderFactory.createMatteBorder(0,1,1,1,borderColor),
            EDITOR_TAB_TABS_BORDER, BorderFactory.createEmptyBorder(),

//            EDITOR_STATUS_LEFT_BORDER, new InsetBorder (false, true),
//            EDITOR_STATUS_RIGHT_BORDER, new InsetBorder (false, false),
//            EDITOR_STATUS_ONLYONEBORDER, new InsetBorder (false, false),
//            EDITOR_STATUS_INNER_BORDER, new InsetBorder (false, true),
            

            //#48951 invisible unfocused selection background in Metal L&F
            "nb.explorer.unfocusedSelBg", unfocusedSelBg,
            PROGRESS_CANCEL_BUTTON_ICON, UIUtils.loadImage("org/netbeans/swing/plaf/resources/cancel_task_win_linux_mac.png"),
            // progress component related
            //            "nbProgressBar.Foreground", new Color(49, 106, 197),
            //            "nbProgressBar.Background", Color.WHITE,
            "nbProgressBar.popupDynaText.foreground", new Color(115, 115, 115),
            //            "nbProgressBar.popupText.background", new Color(231, 249, 249),
            "nbProgressBar.popupText.foreground", UIManager.getColor("TextField.foreground"),
            "nbProgressBar.popupText.selectBackground", UIManager.getColor("List.selectionBackground"),
            "nbProgressBar.popupText.selectForeground", UIManager.getColor("List.selectionForeground"),}; //NOI18N




        //#108517 - turn off ctrl+page_up and ctrl+page_down mapping
        return UIUtils.addInputMapsWithoutCtrlPageUpAndCtrlPageDown(result);
    }

//    private class MetalPropertySheetColorings extends UIBootstrapValue.Lazy {
//
//        public MetalPropertySheetColorings() {
//            super(null);
//        }
//
//        public Object[] createKeysAndValues() {
//            return new Object[]{
//                        //Property sheet settings as defined by HIE
//                        PROPSHEET_SELECTION_BACKGROUND, new Color(204, 204, 255),
//                        PROPSHEET_SELECTION_FOREGROUND, Color.BLACK,
//                        PROPSHEET_SET_BACKGROUND, new Color(224, 224, 224),
//                        PROPSHEET_SET_FOREGROUND, Color.BLACK,
//                        PROPSHEET_SELECTED_SET_BACKGROUND, new Color(204, 204, 255),
//                        PROPSHEET_SELECTED_SET_FOREGROUND, Color.BLACK,
//                        PROPSHEET_DISABLED_FOREGROUND, new Color(153, 153, 153),};
//        }
//    }
}
