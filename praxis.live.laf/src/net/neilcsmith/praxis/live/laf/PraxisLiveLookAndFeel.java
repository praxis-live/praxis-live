/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package net.neilcsmith.praxis.live.laf;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.concurrent.Callable;
import javax.swing.JLabel;
import javax.swing.Painter;
import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 * Dark-themed Nimbus l&f
 *
 * @author Neil C Smith / S. Aubrecht
 */
public class PraxisLiveLookAndFeel extends NimbusLookAndFeel {
//

    private final static Color DARK_GREY = new ColorUIResource(Color.decode("#121212"));
    private final static Color MEDIUM_GREY = new ColorUIResource(Color.decode("#262626"));
    private final static Color LIGHT_GREY = new ColorUIResource(Color.decode("#AAAAAA"));
    private final static Color OFF_WHITE = new ColorUIResource(Color.decode("#DCDCDC"));
    private final static Color PRIMARY = new ColorUIResource(Color.decode("#4545A1"));
    private final static Color SECONDARY = new ColorUIResource(Color.decode("#5959B5"));
    
    private final static Color BASE = new ColorUIResource(Color.decode("#010105"));

    @Override
    public String getName() {
        return "Praxis LIVE";
    }
    
    @Override
    public UIDefaults getDefaults() {
        UIDefaults res = super.getDefaults();
        extendDefaults(res);
        return res;
    }

    private static void swap(UIDefaults def, String key1, String key2) {
        Object tmp = def.get(key1);
        def.put(key1, def.get(key2));
        def.put(key2, tmp);
    }
    
    private void extendDefaults(UIDefaults res) {

        // BEGIN :: Keep this section in sync with PraxisLookAndFeel
        res.put("control", DARK_GREY);
        res.put("info", Color.BLACK);// new Color(128,128,128) );
        res.put("nimbusBase", BASE);
        res.put("nimbusBlueGrey", MEDIUM_GREY);
////        res.put("nimbusBlueGrey", Color.decode("#4545a1"));
//        res.put("nimbusAlertYellow", new Color(248, 187, 0));
//        res.put("nimbusDisabledText", new Color(196, 196, 196));
        res.put("nimbusDisabledText", LIGHT_GREY);
        res.put("nimbusFocus", SECONDARY);
//        res.put("nimbusGreen", new Color(176, 179, 50));
        res.put("nimbusInfoBlue", MEDIUM_GREY);
        res.put("nimbusLightBackground", Color.BLACK);
//        res.put("nimbusOrange", new Color(191, 98, 4));
        res.put("nimbusOrange", PRIMARY);
//        res.put("nimbusRed", new Color(169, 46, 34));
        res.put("nimbusSelectedText", Color.WHITE);
        res.put("nimbusSelectionBackground", PRIMARY);
        res.put("text", OFF_WHITE);
        res.put("textForeground", OFF_WHITE);
        res.put("textText", OFF_WHITE);

        //  Menus
        SolidColorPainter primaryBG = new SolidColorPainter(PRIMARY);
        SolidColorPainter greyBG = new SolidColorPainter(MEDIUM_GREY);
        res.put("PopupMenu[Disabled].backgroundPainter", greyBG);
        res.put("PopupMenu[Enabled].backgroundPainter", greyBG);
        res.put("MenuBar:Menu[Enabled].textForeground", OFF_WHITE);
        res.put("Menu[Enabled].textForeground", OFF_WHITE);
        res.put("Menu[Disabled].textForeground", LIGHT_GREY);
        res.put("MenuItem[Enabled].textForeground", OFF_WHITE);
        res.put("MenuItem[Disabled].textForeground", LIGHT_GREY);
        res.put("CheckBoxMenuItem[Enabled].textForeground", OFF_WHITE);
        res.put("RadioButtonMenuItem[Enabled].textForeground", OFF_WHITE);
        res.put("Menu[Enabled].arrowIconPainter", res.get("Menu[Enabled+Selected].arrowIconPainter"));
        res.put("MenuBar:Menu[Selected].backgroundPainter", greyBG);
        res.put("Menu[Enabled+Selected].backgroundPainter", primaryBG);
        res.put("MenuItem[MouseOver].backgroundPainter", primaryBG);
        res.put("RadioButtonMenuItem[MouseOver].backgroundPainter", primaryBG);
        res.put("RadioButtonMenuItem[MouseOver+Selected].backgroundPainter", primaryBG);
        res.put("CheckBoxMenuItem[MouseOver].backgroundPainter", primaryBG);
        res.put("CheckBoxMenuItem[MouseOver+Selected].backgroundPainter", primaryBG);

        res.put("Tree[Enabled].collapsedIconPainter", res.get("Tree[Enabled+Selected].collapsedIconPainter"));
        res.put("Tree[Enabled].expandedIconPainter", res.get("Tree[Enabled+Selected].expandedIconPainter"));

        res.put("Table[Enabled+Selected].textForeground", OFF_WHITE);

        res.put("TabbedPane:TabbedPaneTab[Enabled].backgroundPainter", null);
        res.put("TabbedPane:TabbedPaneTab[Disabled].backgroundPainter", null);
        
        res.put("ToggleButton[Selected].backgroundPainter", res.get("Button[Default].backgroundPainter"));
        res.put("ToggleButton[MouseOver+Selected].backgroundPainter", res.get("Button[Default+MouseOver].backgroundPainter"));
        res.put("ToggleButton[Focused+Selected].backgroundPainter", res.get("Button[Default+Focused].backgroundPainter"));
        res.put("ToggleButton[Focused+MouseOver+Selected].backgroundPainter", res.get("Button[Default+Focused+MouseOver].backgroundPainter"));
        res.put("ToolBar:ToggleButton[Selected].backgroundPainter", res.get("Button[Default].backgroundPainter"));
        res.put("ToolBar:ToggleButton[MouseOver+Selected].backgroundPainter", res.get("Button[Default+MouseOver].backgroundPainter"));
        res.put("ToolBar:ToggleButton[Focused+Selected].backgroundPainter", res.get("Button[Default+Focused].backgroundPainter"));
        res.put("ToolBar:ToggleButton[Focused+MouseOver+Selected].backgroundPainter", res.get("Button[Default+Focused+MouseOver].backgroundPainter"));

        
        
//        
//        invertIcon(res, "Tree[Enabled].openIconPainter");
//        invertIcon(res, "Tree[Enabled].openIconPainter");
//        invertIcon(res, "Tree[Enabled].collapsedIconPainter");
//        invertIcon(res, "Tree[Enabled].expandedIconPainter");


        // END :: Keep this section in sync with PraxisLookAndFeel
        //
        //
        //
        // fix combo box highlighting in property sheet
        res.put("ComboBox.selectionBackground", PRIMARY);
        //        res.put("nb.errorForeground", new Color(127, 0, 0)); //NOI18N
//        res.put("nb.warningForeground", new Color(255, 216, 0)); //NOI18N

        // Below kept from DarkNimbusTheme from NetBeans
        res.put("nb.dark.theme", Boolean.TRUE);
        res.put("nb.heapview.border1", new Color(128, 128, 128)); //NOI18N
        res.put("nb.heapview.border2", new Color(128, 128, 128).darker()); //NOI18N
        res.put("nb.heapview.border3", new Color(115, 164, 209)); //NOI18N

        res.put("nb.heapview.foreground", new Color(230, 230, 230)); //NOI18N

        res.put("nb.heapview.background1", new Color(18, 30, 49)); //NOI18N

        res.put("nb.heapview.background2", new Color(18, 30, 49).brighter()); //NOI18N

        res.put("nb.heapview.grid1.start", new Color(97, 95, 87)); //NOI18N
        res.put("nb.heapview.grid1.end", new Color(98, 96, 88)); //NOI18N
        res.put("nb.heapview.grid2.start", new Color(99, 97, 90)); //NOI18N
        res.put("nb.heapview.grid2.end", new Color(101, 99, 92)); //NOI18N
        res.put("nb.heapview.grid3.start", new Color(102, 101, 93)); //NOI18N
        res.put("nb.heapview.grid3.end", new Color(105, 103, 95)); //NOI18N
        res.put("nb.heapview.grid4.start", new Color(107, 105, 97)); //NOI18N
        res.put("nb.heapview.grid4.end", new Color(109, 107, 99)); //NOI18N

        res.put("PropSheet.setBackground", new Color(112, 112, 112)); //NOI18N
        res.put("PropSheet.selectedSetBackground", new Color(100, 100, 100)); //NOI18N
        res.put("nb.bugtracking.comment.background", new Color(112, 112, 112)); //NOI18N
        res.put("nb.bugtracking.comment.foreground", new Color(230, 230, 230)); //NOI18N
        res.put("nb.bugtracking.label.highlight", new Color(160, 160, 160)); //NOI18N
        res.put("nb.bugtracking.table.background", new Color(18, 30, 49)); //NOI18N
        res.put("nb.bugtracking.table.background.alternate", new Color(13, 22, 36)); //NOI18N
        res.put("nb.bugtracking.new.color", new Color(0, 224, 0)); //NOI18N
        res.put("nb.bugtracking.modified.color", new Color(81, 182, 255)); //NOI18N
        res.put("nb.bugtracking.obsolete.color", new Color(153, 153, 153)); //NOI18N
        res.put("nb.bugtracking.conflict.color", new Color(255, 51, 51)); //NOI18N

        res.put("nb.html.link.foreground", new Color(164, 164, 255)); //NOI18N
        res.put("nb.html.link.foreground.hover", new Color(255, 216, 0)); //NOI18N
        res.put("nb.html.link.foreground.visited", new Color(0, 200, 0)); //NOI18N
        res.put("nb.html.link.foreground.focus", new Color(255, 216, 0)); //NOI18N

        res.put("nb.startpage.defaultbackground", Boolean.TRUE);
        res.put("nb.startpage.defaultbuttonborder", Boolean.TRUE);
        res.put("nb.startpage.bottombar.background", new Color(64, 64, 64));
        res.put("nb.startpage.topbar.background", new Color(64, 64, 64));
        res.put("nb.startpage.border.color", new Color(18, 30, 49));
        res.put("nb.startpage.tab.border1.color", new Color(64, 64, 64));
        res.put("nb.startpage.tab.border2.color", new Color(64, 64, 64));
        res.put("nb.startpage.rss.details.color", new Color(230, 230, 230));
        res.put("nb.startpage.rss.header.color", new Color(128, 128, 255));
        res.put("nb.startpage.tab.imagename.selected", "org/netbeans/modules/welcome/resources/tab_selected_dark.png"); //NOI18N
        res.put("nb.startpage.tab.imagename.rollover", "org/netbeans/modules/welcome/resources/tab_rollover_dark.png"); //NOI18N
        res.put("nb.startpage.imagename.contentheader", "org/netbeans/modules/welcome/resources/content_banner_dark.png"); //NOI18N
        res.put("nb.startpage.contentheader.color1", new Color(12, 33, 61)); //NOI18N
        res.put("nb.startpage.contentheader.color2", new Color(16, 24, 42)); //NOI18N

        res.put("nb.popupswitcher.background", new Color(18, 30, 49));

        res.put("nb.editor.errorstripe.caret.color", new Color(230, 230, 230)); //NOI18N

        res.put("nb.wizard.hideimage", Boolean.TRUE); //NOI18N

        //diff & diff sidebar
        res.put("nb.diff.added.color", new Color(36, 52, 36)); //NOI18N
        res.put("nb.diff.changed.color", new Color(32, 40, 51)); //NOI18N
        res.put("nb.diff.deleted.color", new Color(51, 32, 36)); //NOI18N
        res.put("nb.diff.applied.color", new Color(36, 52, 36)); //NOI18N
        res.put("nb.diff.notapplied.color", new Color(32, 40, 51)); //NOI18N
        res.put("nb.diff.unresolved.color", new Color(51, 32, 36)); //NOI18N

        res.put("nb.diff.sidebar.changed.color", new Color(18, 30, 74)); //NOI18N
        res.put("nb.diff.sidebar.deleted.color", new Color(66, 30, 49)); //NOI18N

        res.put("nb.versioning.tooltip.background.color", new Color(18, 30, 74)); //NOI18N

        //form designer
        res.put("nb.formdesigner.gap.fixed.color", new Color(112, 112, 112)); //NOI18N
        res.put("nb.formdesigner.gap.resizing.color", new Color(116, 116, 116)); //NOI18N
        res.put("nb.formdesigner.gap.min.color", new Color(104, 104, 104)); //NOI18N

        res.put("nbProgressBar.Foreground", new Color(230, 230, 230));
        res.put("nbProgressBar.popupDynaText.foreground", new Color(191, 186, 172));

        // debugger
        res.put("nb.debugger.debugging.currentThread", new Color(30, 80, 28)); //NOI18N
        res.put("nb.debugger.debugging.highlightColor", new Color(40, 60, 38)); //NOI18N
        res.put("nb.debugger.debugging.BPHits", new Color(65, 65, 0)); //NOI18N
        res.put("nb.debugger.debugging.bars.BPHits", new Color(120, 120, 25)); //NOI18N
        res.put("nb.debugger.debugging.bars.currentThread", new Color(40, 100, 35)); //NOI18N

        //versioning
        res.put("nb.versioning.added.color", new Color(0, 224, 0)); //NOI18N
        res.put("nb.versioning.modified.color", new Color(81, 182, 255)); //NOI18N
        res.put("nb.versioning.deleted.color", new Color(153, 153, 153)); //NOI18N
        res.put("nb.versioning.conflicted.color", new Color(255, 51, 51)); //NOI18N
        res.put("nb.versioning.ignored.color", new Color(153, 153, 153)); //NOI18N
        res.put("nb.versioning.remotemodification.color", new Color(230, 230, 230)); //NOI18N

        // db.dataview
        res.put("nb.dataview.table.background", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "Table.background")); //NOI18N
        res.put("nb.dataview.table.altbackground", new RelativeColor(new Color(0, 0, 0), new Color(30, 30, 30), "Table.background")); //NOI18N
        res.put("nb.dataview.table.sqlconstant.foreground", new Color(220, 220, 220)); //NOI18N
        res.put("Table.selectionBackground", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "Table[Enabled+Selected].textBackground")); //NOI18N
        res.put("nb.dataview.tablecell.focused", new RelativeColor(new Color(0, 0, 0), new Color(30, 10, 10), "Table.selectionBackground")); //NOI18N
        res.put("nb.dataview.table.rollOverRowBackground", new RelativeColor(new Color(0, 0, 0), new Color(30, 30, 30), "Table[Enabled+Selected].textBackground")); //NOI18N
        res.put("nb.dataview.tablecell.edited.selected.foreground", new Color(255, 248, 60));  //NOI18N
        res.put("nb.dataview.tablecell.edited.unselected.foreground", new Color(0, 255, 16));  //NOI18N
        res.put("nb.dataview.jxdatetimepicker.background", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "Table.background")); //NOI18N
        res.put("nb.dataview.jxdatetimepicker.foreground", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "Table.foreground")); //NOI18N
        res.put("nb.dataview.jxdatetimepicker.selectedBackground", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "Table[Enabled+Selected].textBackground")); //NOI18N
        res.put("nb.dataview.jxdatetimepicker.selectedForeground", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "Table[Enabled+Selected].textForeground")); //NOI18N
        res.put("nb.dataview.jxdatetimepicker.daysOfTheWeekForeground", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "Table.background")); //NOI18N
        res.put("nb.dataview.jxdatetimepicker.todayBackground", new RelativeColor(new Color(0, 0, 0), new Color(20, 20, 20), "Table.background")); //NOI18N
        res.put("nb.dataview.jxdatetimepicker.todayPanel.background.gradient.start", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "TableHeader.background")); //NOI18N
        res.put("nb.dataview.jxdatetimepicker.todayPanel.background.gradient.end", new RelativeColor(new Color(0, 0, 0), new Color(10, 10, 10), "TableHeader.background")); //NOI18N
        res.put("nb.dataview.jxdatetimepicker.todayPanel.linkForeground", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "TableHeader.foreground")); //NOI18N

        // autoupdate
        res.put("nb.autoupdate.search.highlight", new Color(255, 75, 0));

        res.put("selection.highlight", new Color(202, 152, 0));
        res.put("textArea.background", new Color(128, 128, 128));

        res.put("nb.laf.postinstall.callable", new Callable<Object>() { //NOI18N

            @Override
            public Object call() throws Exception {
                //change the default link foreground color
                HTMLEditorKit kit = new HTMLEditorKit();
                StyleSheet newStyleSheet = new StyleSheet();
                Font f = new JLabel().getFont();
                newStyleSheet.addRule(new StringBuffer("body { font-size: ").append(f.getSize()) // NOI18N
                        .append("; font-family: ").append(f.getName()).append("; }").toString()); // NOI18N
                newStyleSheet.addRule("a { color: #A4A4FF; text-decoration: underline}"); //NOI18N
                newStyleSheet.addStyleSheet(kit.getStyleSheet());
                kit.setStyleSheet(newStyleSheet);
                return null;
            }
        });

        res.put("nb.close.tab.icon.enabled.name", "org/openide/awt/resources/vista_close_enabled.png");
        res.put("nb.close.tab.icon.pressed.name", "org/openide/awt/resources/vista_close_pressed.png");
        res.put("nb.close.tab.icon.rollover.name", "org/openide/awt/resources/vista_close_rollover.png");
        res.put("nb.bigclose.tab.icon.enabled.name", "org/openide/awt/resources/vista_bigclose_rollover.png");
        res.put("nb.bigclose.tab.icon.pressed.name", "org/openide/awt/resources/vista_bigclose_rollover.png");
        res.put("nb.bigclose.tab.icon.rollover.name", "org/openide/awt/resources/vista_bigclose_rollover.png");

        //browser picker
        res.put("Nb.browser.picker.background.light", new Color(116, 116, 116));
        res.put("Nb.browser.picker.foreground.light", new Color(192, 192, 192));
        //#233622
        res.put("List[Selected].textForeground", res.getColor("nimbusSelectedText"));

        res.put("nb.explorer.noFocusSelectionBackground", res.get("nimbusSelectionBackground"));

        //search in projects
        res.put("nb.search.sandbox.highlight", new Color(104, 93, 156));
        res.put("nb.search.sandbox.regexp.wrong", new Color(255, 71, 71));

    }

}
