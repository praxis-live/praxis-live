package com.revivius.nb.darcula;

import com.revivius.nb.darcula.ui.IncreasedInsetsTableHeaderBorder;
import com.revivius.nb.darcula.ui.ReducedInsetsDarculaButtonPainter;
import com.revivius.nb.darcula.options.DarculaLAFOptionsPanelController;
import com.revivius.nb.darcula.options.DarculaLAFPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.ColorUIResource;
import org.netbeans.swing.plaf.LFCustoms;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
//import sun.swing.SwingLazyValue;

/**
 * LFCustoms for Darcula LAF.
 *
 * @author Revivius
 */
public class DarculaLFCustoms extends LFCustoms {

    private static final String FONT_DEFAULT_NAME = "Dialog";
    private static final int FONT_DEFAULT_SIZE = 12;
    public static final String DEFAULT_FONT = FONT_DEFAULT_NAME + " " + FONT_DEFAULT_SIZE;

    private static final String TAB_FOCUS_FILL_UPPER = "tab_focus_fill_upper"; //NOI18N
    private static final String TAB_FOCUS_FILL_LOWER = "tab_focus_fill_lower"; //NOI18N

    private static final String TAB_UNSEL_FILL_UPPER = "tab_unsel_fill_upper"; //NOI18N
    private static final String TAB_UNSEL_FILL_LOWER = "tab_unsel_fill_lower"; //NOI18N

    private static final String TAB_SEL_FILL = "tab_sel_fill"; //NOI18N

    private static final String TAB_MOUSE_OVER_FILL_UPPER = "tab_mouse_over_fill_upper"; //NOI18N
    private static final String TAB_MOUSE_OVER_FILL_LOWER = "tab_mouse_over_fill_lower"; //NOI18N

    private static final String TAB_ATTENTION_FILL_UPPER = "tab_attention_fill_upper"; //NOI18N
    private static final String TAB_ATTENTION_FILL_LOWER = "tab_attention_fill_lower"; //NOI18N

    private static final String TAB_BORDER = "tab_border"; //NOI18N      
    private static final String TAB_SEL_BORDER = "tab_sel_border"; //NOI18N
    private static final String TAB_BORDER_INNER = "tab_border_inner"; //NOI18N    
    
    private final static Color DARK_GREY = new ColorUIResource(Color.decode("#1F1F1F"));
    private final static Color MEDIUM_GREY = new ColorUIResource(Color.decode("#262626"));
    private final static Color PRIMARY = new ColorUIResource(Color.decode("#4545A1"));

    @Override
    public Object[] createGuaranteedKeysAndValues() {
        // same color for DarculaMetalTheme getAcceleratorForeground()
        Color asfg = new ColorUIResource(187, 187, 187);
        
        Object[] result = {
            "controlShadow", new ColorUIResource(41, 43, 45),
            "controlHighlight", new ColorUIResource(70, 72, 74),
            "controlDkShadow", new ColorUIResource(41, 43, 45),
            "controlLtHighlight", new ColorUIResource(70, 72, 74),
            
            "Menu.acceleratorSelectionForeground", asfg,
            "MenuItem.acceleratorSelectionForeground", asfg,
            "CheckBoxMenuItem.acceleratorSelectionForeground", asfg,
            "RadioButtonMenuItem.acceleratorSelectionForeground", asfg
        };

        return result;
    }
    
    @Override
    public Object[] createLookAndFeelCustomizationKeysAndValues() {
        Preferences prefs = NbPreferences.forModule(DarculaLAFPanel.class);
        boolean useStretchedTabs = prefs.getBoolean(DarculaLAFOptionsPanelController.STRETCHED_TABS_BOOLEAN, false);
        if (useStretchedTabs) {
            // stretch view tabs
            System.setProperty("winsys.stretching_view_tabs", "true");
            // stretching view tabs seems to be causing resize problems
            System.setProperty("NB.WinSys.Splitter.Respect.MinimumSize.Enabled", "false");
        }
        Font controlFont = Font.decode(DEFAULT_FONT);
        Integer in = (Integer) UIManager.get(CUSTOM_FONT_SIZE); //NOI18N
        if (in != null) {
            controlFont = Font.decode(FONT_DEFAULT_NAME + " " + in);
        }

        boolean overrideFontOption = prefs.getBoolean(DarculaLAFOptionsPanelController.OVERRIDE_FONT_BOOLEAN, false);
        if (overrideFontOption) {
            String fontOption = prefs.get(DarculaLAFOptionsPanelController.FONT_STRING, DEFAULT_FONT);
            controlFont = Font.decode(fontOption);
        }

        /**
         * HtmlLabelUI sets the border color to BLUE for focused cells if
         * "Tree.selectionBorderColor" is same as background color (see lines
         * 230 - 247). Here we modify "Tree.selectionBorderColor" slightly.
         * Modification is not noticable to naked eye but enough to stop
         * HtmlLabelUI to set focused renderer border color to BLUE.
         */
        Color c = UIManager.getColor("Tree.selectionBackground");
        Color focusColor = new Color(c.getRed(), c.getGreen(), c.getBlue() + 1);

        Object[] result = {
            // The assorted standard NetBeans metal font customizations
            // The assorted standard NetBeans metal font customizations
            CONTROLFONT, controlFont,
            SYSTEMFONT, controlFont,
            USERFONT, controlFont,
            MENUFONT, controlFont,
            WINDOWTITLEFONT, controlFont,
            SUBFONT, controlFont.deriveFont(Font.PLAIN, Math.min(controlFont.getSize() - 1, 6)),
            
            // Bug in JDK 1.5 thru b59 - pale blue is incorrectly returned for this
            "textInactiveText", Color.GRAY,
            
            /**
             * Work around a bug in windows which sets the text area font to 
             * "MonoSpaced", causing all accessible dialogs to have monospaced text
             */
            "TextArea.font", new GuaranteedValue("Label.font", controlFont),
            
            /**
             * HtmlLabelUI uses UIManager.getColor("text") to find background
             * color for unselected items. Make sure the background color used
             * by HtmlLabelUI is same with the LAF.
             */
            "text", new Color(60, 63, 65),
            "textText", new Color(187, 187, 187),
            "infoText", new Color(187, 187, 187),
            
            "TabbedPaneUI", "com.revivius.nb.darcula.ui.DarkScrollButtonTabbedPaneUI",

            "LabelUI", "com.revivius.nb.darcula.ui.OptionsAwareLabelUI",
            "Label.font", controlFont,
            
            "ButtonUI", "com.revivius.nb.darcula.ui.ContentAreaAwareButtonUI",
            "Button.border", new ReducedInsetsDarculaButtonPainter(),
            "Button.font", controlFont,
                        
            "ToggleButtonUI", "com.revivius.nb.darcula.ui.ContentAreaAwareToggleButtonUI",
            "ToggleButton.border", new ReducedInsetsDarculaButtonPainter(),
            "ToggleButton.font", controlFont,
            
            "ToolBarUI", "com.revivius.nb.darcula.ui.RolloverToolBarUI",
            "ToolBar.font", controlFont,            
            
            "SplitPaneUI", "com.revivius.nb.darcula.ui.DarculaSplitPaneUI",
            
            SPINNERFONT, controlFont,
            "Spinner.font", controlFont,
            
            /**
             * #31 
             * Icon provided by Aqua LAF is not visible on dark background
             * provide default Metal arrow icon for all LAFs
             */
//            "Menu.arrowIcon", new SwingLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getMenuArrowIcon"),
            "Menu.acceleratorFont", controlFont,
            "Menu.font", controlFont,

            "Table.font", controlFont,
            "Table.ascendingSortIcon", new ImageIcon(DarculaLFCustoms.class.getResource("column-asc.png")),
            "Table.descendingSortIcon", new ImageIcon(DarculaLFCustoms.class.getResource("column-desc.png")),
            "Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder(),
            
            "TableHeader.cellBorder", new IncreasedInsetsTableHeaderBorder(),
            "TableHeader.font", controlFont,
            
            "TitledBorder.border", BorderFactory.createLineBorder(new Color(41, 43, 45), 1),
            "TitledBorder.font", controlFont,
            
            "MenuItem.acceleratorForeground", new Color(238, 238, 238),
            "MenuItem.acceleratorFont", controlFont,
            "MenuItem.font", controlFont,
            
            LISTFONT, controlFont,
            "List.font", controlFont,
            "List.focusCellHighlightBorder", BorderFactory.createEmptyBorder(),
            
            TREEFONT, controlFont,
            "Tree.font", controlFont,
            "Tree.closedIcon", new ImageIcon(DarculaLFCustoms.class.getResource("closed.png")),
            "Tree.openIcon", new ImageIcon(DarculaLFCustoms.class.getResource("open.png")),
            "Tree.selectionBorderColor", focusColor, // Use calculateD border color for HtmlLabelUI.

            // FileChooser icons
            "FileView.directoryIcon", new ImageIcon(DarculaLFCustoms.class.getResource("closed.png")),
            "FileView.fileIcon", new ImageIcon(DarculaLFCustoms.class.getResource("file.png")),
            
            "FileChooser.newFolderIcon", new ImageIcon(DarculaLFCustoms.class.getResource("newFolder.png")),
            "FileChooser.upFolderIcon", new ImageIcon(DarculaLFCustoms.class.getResource("upFolder.png")),
            "FileChooser.homeFolderIcon", new ImageIcon(DarculaLFCustoms.class.getResource("homeFolder.png")),
            "FileChooser.detailsViewIcon", new ImageIcon(DarculaLFCustoms.class.getResource("detailsView.png")),
            "FileChooser.listViewIcon", new ImageIcon(DarculaLFCustoms.class.getResource("listView.png")),
            "FileChooser.computerIcon", new ImageIcon(DarculaLFCustoms.class.getResource("computer.png")),
            "FileChooser.hardDriveIcon", new ImageIcon(DarculaLFCustoms.class.getResource("hardDrive.png")),
            "FileChooser.floppyDriveIcon", new ImageIcon(DarculaLFCustoms.class.getResource("floppyDrive.png")),
            
            "CheckBox.font", controlFont,
            "CheckBoxMenuItem.acceleratorFont", controlFont,
            "CheckBoxMenuItem.font", controlFont,
            "CheckBoxMenuItem.acceleratorForeground", new Color(238, 238, 238),

            "ColorChooser.font", controlFont,
            
            "ComboBox.font", controlFont,
            
            "EditorPane.font", controlFont,
            
            "FormattedTextField.font", controlFont,
            
            "IconButton.font", controlFont,
            
            "InternalFrame.optionDialogTitleFont", controlFont,
            "InternalFrame.paletteTitleFont", controlFont,
            "InternalFrame.titleFont", controlFont,
            
            "MenuBar.font", controlFont,
            
            "OptionPane.buttonFont", controlFont,
            "OptionPane.font", controlFont,
            "OptionPane.messageFont", controlFont,
            "OptionPane.messageForeground", new Color(187, 187, 187),

            PANELFONT, controlFont,
            "Panel.font", controlFont,
            
            "PasswordField.font", controlFont,
            
            "PopupMenu.font", controlFont,
            
            "ProgressBar.font", controlFont,
            
            "RadioButton.font", controlFont,
            "RadioButtonMenuItem.acceleratorFont", controlFont,
            "RadioButtonMenuItem.font", controlFont,
            "RadioButtonMenuItem.acceleratorForeground", new Color(238, 238, 238),
            
            "ScrollPane.font", controlFont,
            
            "Slider.font", controlFont,
            
            "TabbedPane.font", controlFont,
            //"TabbedPane.smallFont", controlFont,
            
            "TextArea.font", controlFont,
            
            "TextField.font", controlFont,
            
            "TextPane.font", controlFont,
            
            "ToolTip.font", controlFont,
            "ToolTip.border", BorderFactory.createLineBorder(new Color(154, 154, 102)),
            "ToolTip.borderInactive", BorderFactory.createLineBorder(new Color(154, 154, 102)),
            "ToolTip.foregroundInactive", new Color(187, 187, 187),
            "ToolTip.backgroundInactive", new Color(92, 92, 66),
            
            "Viewport.font", controlFont,
            
            
        };

        removeEnterFromTreeInputMap();

        replaceSearchNotFoundColor();
        replaceGlyphGutterLineColor();
        replaceFormDesignerGapBorderColors();

        replaceLFCustomsTextFgColors();
        replaceCompletionColors();
        replaceSQLCompletionColumnColor();
        replaceJSPCompletionColor();
        replaceHTMLCompletionColor();      
        replaceCSSPreprocessorCompletionColors();
        replaceProjectTabColors();

        List<Object> resList = new ArrayList<>(Arrays.asList(result));
        
        darkenBackgrounds(resList);
        changeSelectionColor(resList);
        
        return resList.toArray();
    }
    
    private void darkenBackgrounds(List<Object> resList) {
        Set<String> keys = UIManager.getLookAndFeelDefaults().keySet().stream()
                .map(Object::toString)
                .filter(s -> s.endsWith(".background"))
                .collect(Collectors.toSet());
        for (String key : keys) {
            if (key.contains("Menu") || key.contains("Separator")) {
                continue;
            }
            resList.add(key);
            resList.add(key.contains("Text") ? DARK_GREY : MEDIUM_GREY);
        }
    }
    
    private void changeSelectionColor(List<Object> resList) {
        Set<String> keys = UIManager.getLookAndFeelDefaults().keySet().stream()
                .map(Object::toString)
                .filter(s -> s.endsWith(".selectionBackground"))
                .collect(Collectors.toSet());
        for (String key : keys) {
            resList.add(key);
            resList.add(PRIMARY);
        }
    }
    

    @Override
    public Object[] createApplicationSpecificKeysAndValues() {

        Object[] result = {
            // enable _dark postfix for resource loading
            "nb.dark.theme", Boolean.TRUE,
//            "nb.wizard.hideimage", Boolean.TRUE,
            "window", MEDIUM_GREY,
            
            // main toolbar
            "Nb.MainWindow.Toolbar.Dragger", "com.revivius.nb.darcula.ToolbarXP",
            "Nb.MainWindow.Toolbar.Border", BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(41, 43, 45)),
            "Nb.ToolBar.border", BorderFactory.createEmptyBorder(),
            
            EDITOR_TAB_DISPLAYER_UI, "org.netbeans.swing.tabcontrol.plaf.Windows8EditorTabDisplayerUI",
            VIEW_TAB_DISPLAYER_UI, "org.netbeans.swing.tabcontrol.plaf.Windows8ViewTabDisplayerUI",
            SLIDING_BUTTON_UI, "org.netbeans.swing.tabcontrol.plaf.WinXPSlidingButtonUI",
            
            SCROLLPANE_BORDER, BorderFactory.createLineBorder(new Color(41, 43, 45)),
            SCROLLPANE_BORDER_COLOR, new Color(41, 43, 45),
            
            EDITOR_TOOLBAR_BORDER, BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(41, 43, 45)),
            EDITOR_ERRORSTRIPE_SCROLLBAR_INSETS, new Insets(16, 0, 16, 0),
            
            DESKTOP_BACKGROUND, Color.RED,
            DESKTOP_BORDER, BorderFactory.createEmptyBorder(),
            WORKPLACE_FILL, Color.RED,
            
            DESKTOP_SPLITPANE_BORDER, BorderFactory.createEmptyBorder(),
            SPLIT_PANE_DIVIDER_SIZE_VERTICAL, 2,
            SPLIT_PANE_DIVIDER_SIZE_HORIZONTAL, 2,
            
            WARNING_FOREGROUND, new Color(254, 183, 24),
            ERROR_FOREGROUND, new Color(255, 102, 102),
            
            // tabs
            //selected & focused
            TAB_FOCUS_FILL_UPPER, new Color(75, 110, 175),
            TAB_FOCUS_FILL_LOWER, new Color(65, 81, 109),
            
            //no selection, no focus
            TAB_UNSEL_FILL_UPPER, new Color(84, 88, 91),
            TAB_UNSEL_FILL_LOWER, new Color(56, 58, 60),
            
            //selected, no focus
            TAB_SEL_FILL, new Color(84, 88, 91),
            
            //no selection, mouse over
            TAB_MOUSE_OVER_FILL_UPPER, new Color(114, 119, 122),
            TAB_MOUSE_OVER_FILL_LOWER, new Color(98, 101, 104),
            TAB_ATTENTION_FILL_UPPER, new Color(255, 255, 128),
            TAB_ATTENTION_FILL_LOWER, new Color(230, 200, 64),
            
            TAB_BORDER, new Color(41, 43, 45),
            TAB_SEL_BORDER, new Color(41, 43, 45),
            TAB_BORDER_INNER, new Color(70, 72, 74),
            
            //Borders for the tab control
            EDITOR_TAB_OUTER_BORDER, BorderFactory.createEmptyBorder(),
            EDITOR_TAB_CONTENT_BORDER, BorderFactory.createCompoundBorder(
                    new MatteBorder(0, 0, 1, 0, new Color(41, 43, 45)),
                    BorderFactory.createEmptyBorder(0, 1, 0, 1)
            ),
            EDITOR_TAB_TABS_BORDER, BorderFactory.createEmptyBorder(),
            VIEW_TAB_OUTER_BORDER, BorderFactory.createEmptyBorder(),
            VIEW_TAB_CONTENT_BORDER, new MatteBorder(0, 1, 1, 1, new Color(41, 43, 45)),
            VIEW_TAB_TABS_BORDER, BorderFactory.createEmptyBorder(),
            
            // quicksearch
            "nb.quicksearch.border", BorderFactory.createEmptyBorder(),
            
            // progress ui
            "nb.progress.cancel.icon", ImageUtilities.loadImage("org/openide/awt/resources/mac_close_rollover_dark.png", false),
            "nb.progress.cancel.icon.mouseover", ImageUtilities.loadImage("org/openide/awt/resources/mac_close_enabled_dark.png", false),
            "nb.progress.cancel.icon.pressed", ImageUtilities.loadImage("org/openide/awt/resources/mac_close_pressed_dark.png", false),
            
            // explorer views
            "nb.explorer.unfocusedSelBg", new Color(13, 41, 62),
            "nb.explorer.unfocusedSelFg", new Color(187, 187, 187),
            "nb.explorer.noFocusSelectionBackground", new Color(13, 41, 62),
            "nb.explorer.noFocusSelectionForeground", new Color(187, 187, 187),
            "ETableHeader.ascendingIcon", new ImageIcon(DarculaLFCustoms.class.getResource("column-asc.png")),
            "ETableHeader.descendingIcon", new ImageIcon(DarculaLFCustoms.class.getResource("column-desc.png")),
            
            // popup switcher
            "nb.popupswitcher.border", BorderFactory.createLineBorder(new Color(45, 45, 45)),
            
             // debugger
            "nb.debugger.debugging.currentThread", new Color(30, 80, 28),
            "nb.debugger.debugging.highlightColor", new Color(40, 60, 38),
            "nb.debugger.debugging.BPHits", new Color(65, 65, 0),
            "nb.debugger.debugging.bars.BPHits", new Color(120, 120, 25),
            "nb.debugger.debugging.bars.currentThread", new Color(40, 100, 35),
            
            // heapview
            "nb.heapview.border1", new Color(113, 113, 113),
            "nb.heapview.border2", new Color(91, 91, 95),
            "nb.heapview.border3", new Color(128, 128, 128),
            "nb.heapview.foreground", new Color(222, 222, 222),
            "nb.heapview.background1", new Color(53, 56, 58),
            "nb.heapview.background2", new Color(50, 66, 114),
            "nb.heapview.grid1.start", new Color(97, 95, 87),
            "nb.heapview.grid1.end", new Color(98, 96, 88),
            "nb.heapview.grid2.start", new Color(99, 97, 90),
            "nb.heapview.grid2.end", new Color(101, 99, 92),
            "nb.heapview.grid3.start", new Color(102, 101, 93),
            "nb.heapview.grid3.end", new Color(105, 103, 95),
            "nb.heapview.grid4.start", new Color(107, 105, 97),
            "nb.heapview.grid4.end", new Color(109, 107, 99),

            // bug tracking
            "nb.bugtracking.comment.background", new Color(59, 63, 64),
            "nb.bugtracking.comment.foreground", new Color(187, 187, 187),
            "nb.bugtracking.label.highlight", new Color(205, 205, 0),
            "nb.bugtracking.table.background", new Color(59, 63, 64),
            "nb.bugtracking.table.background.alternate", new Color(69, 73, 74),
            "nb.bugtracking.new.color", new Color(73, 210, 73),
            "nb.bugtracking.modified.color", new Color(26, 184, 255),
            "nb.bugtracking.obsolete.color", new Color(142, 142, 142),
            "nb.bugtracking.conflict.color", new Color(255, 100, 100),

            // db.dataview
            "nb.dataview.table.gridbackground", UIManager.getColor("Table.gridColor"),
            "nb.dataview.table.background", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "Table.background"),
            "nb.dataview.table.altbackground", new RelativeColor(new Color(0, 0, 0), new Color(30, 30, 30), "Table.background"),
            "nb.dataview.table.sqlconstant.foreground", new Color(220, 220, 220),
            "nb.dataview.tablecell.focused", /*new RelativeColor(new Color(0, 0, 0), new Color(10, 10, 30), "Table.selectionBackground"), */ new Color(13, 41, 62),
            "nb.dataview.table.rollOverRowBackground", new RelativeColor(new Color(0, 0, 0), new Color(30, 30, 30), "Table.selectionBackground"),
            "nb.dataview.tablecell.edited.selected.foreground", new Color(241, 255, 177),
            "nb.dataview.tablecell.edited.unselected.foreground", /*new Color(0, 255, 16),*/ new Color(172, 221, 124),
            "nb.dataview.jxdatetimepicker.background", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "Table.background"),
            "nb.dataview.jxdatetimepicker.foreground", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "Table.foreground"),
            "nb.dataview.jxdatetimepicker.selectedBackground", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "Table.selectionBackground"),
            "nb.dataview.jxdatetimepicker.selectedForeground", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "Table.selectionForeground"),
            "nb.dataview.jxdatetimepicker.daysOfTheWeekForeground", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "Table.background"),
            "nb.dataview.jxdatetimepicker.todayBackground", new RelativeColor(new Color(0, 0, 0), new Color(20, 20, 20), "TableHeader.background"),
            "nb.dataview.jxdatetimepicker.todayPanel.background.gradient.start", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "TableHeader.background"),
            "nb.dataview.jxdatetimepicker.todayPanel.background.gradient.end", new RelativeColor(new Color(0, 0, 0), new Color(10, 10, 10), "TableHeader.background"),
            "nb.dataview.jxdatetimepicker.todayPanel.linkForeground", new RelativeColor(new Color(0, 0, 0), new Color(0, 0, 0), "TableHeader.foreground"),
            
            // form designer
            "nb.formdesigner.gap.fixed.color", new Color(70, 73, 75),
            "nb.formdesigner.gap.resizing.color", new Color(66, 69, 71),
            "nb.formdesigner.gap.min.color", new Color(78, 81, 83),
            
            // link
            "nb.html.link.foreground", new Color(125, 160, 225), //NOI18N
            "nb.html.link.foreground.hover", new Color(13, 41, 62), //NOI18N
            "nb.html.link.foreground.visited", new Color(125, 160, 225), //NOI18N
            "nb.html.link.foreground.focus", new Color(13, 41, 62), //NOI18N

            // startpage
            "nb.startpage.defaultbackground", Boolean.TRUE,
            "nb.startpage.defaultbuttonborder", Boolean.TRUE,
            "nb.startpage.bottombar.background", new Color(13, 41, 62),
            "nb.startpage.topbar.background", new Color(13, 41, 62),
            "nb.startpage.border.color", new Color(13, 41, 62),
            "nb.startpage.tab.border1.color", new Color(13, 41, 62),
            "nb.startpage.tab.border2.color", new Color(13, 41, 62),
            "nb.startpage.rss.details.color", new Color(187, 187, 187),
            "nb.startpage.rss.header.color", new Color(125, 160, 225),
            "nb.startpage.tab.imagename.selected", "org/netbeans/modules/welcome/resources/tab_selected_dark.png", //NOI18N
            "nb.startpage.tab.imagename.rollover", "org/netbeans/modules/welcome/resources/tab_rollover_dark.png", //NOI18N
            "nb.startpage.imagename.contentheader", "org/netbeans/modules/welcome/resources/content_banner_dark.png", //NOI18N
            "nb.startpage.contentheader.color1", new Color(12, 33, 61),
            "nb.startpage.contentheader.color2", new Color(16, 24, 42),
            
            // autoupdate
            "nb.autoupdate.search.highlight", new Color(13, 41, 62),
            
            // notification displayer balloon
            "nb.core.ui.balloon.defaultGradientStartColor", new Color(92, 92, 66),
            "nb.core.ui.balloon.defaultGradientFinishColor", new Color(92, 92, 66),
            "nb.core.ui.balloon.mouseOverGradientStartColor", new Color(92, 92, 66),
            "nb.core.ui.balloon.mouseOverGradientFinishColor", new Color(92, 92, 66).brighter(),
                
            // git
            "nb.versioning.added.color", new Color(73, 210, 73),
            "nb.versioning.modified.color", new Color(26, 184, 255),
            "nb.versioning.deleted.color", new Color(255, 175, 175),
            "nb.versioning.conflicted.color", new Color(255, 100, 100),
            "nb.versioning.ignored.color", new Color(142, 142, 142),
            "nb.versioning.textannotation.color", Color.WHITE,
            "nb.versioning.tooltip.background.color", new Color(92, 92, 66),

            // diff
            "nb.diff.added.color", new Color(43, 85, 43),
            "nb.diff.changed.color", new Color(40, 85, 112),
            "nb.diff.deleted.color", new Color(85, 43, 43),
            "nb.diff.applied.color", new Color(68, 113, 82),
            "nb.diff.notapplied.color", new Color(67, 105, 141),
            "nb.diff.unresolved.color", new Color(130, 30, 30),
            "nb.diff.sidebar.deleted.color", new Color(85, 43, 43),
            "nb.diff.sidebar.changed.color", new Color(30, 75, 112),
            
            // output
            "nb.output.backgorund", new Color(43, 43, 43),
            "nb.output.foreground", new Color(187, 187, 187),
            "nb.output.input", new Color(0, 127, 0),
            "nb.output.err.foreground", new Color(255, 107, 104),
            "nb.output.link.foreground", new Color(126, 174, 241),
            "nb.output.link.foreground.important", new Color(126, 174, 241),
            "nb.output.warning.foreground", new Color(205, 205, 0),
            "nb.output.failure.foreground", new Color(255, 107, 104),
            "nb.output.success.foreground", new Color(112, 255, 112),
            "nb.output.debug.foreground", new Color(145, 145, 145),
            "textHighlight", new Color(240, 119, 70),
            
            
            PROPSHEET_BACKGROUND, DARK_GREY,
            PROPSHEET_SELECTION_BACKGROUND, PRIMARY,
            PROPSHEET_SELECTION_FOREGROUND, Color.WHITE,
            PROPSHEET_SET_BACKGROUND, new Color(82, 85, 86),
            PROPSHEET_SET_FOREGROUND, Color.WHITE,
            PROPSHEET_SELECTED_SET_BACKGROUND, PRIMARY,
            PROPSHEET_SELECTED_SET_FOREGROUND, Color.WHITE,
            PROPSHEET_DISABLED_FOREGROUND, new Color(161, 161, 146),
            PROPSHEET_BUTTON_FOREGROUND, new Color(187, 187, 187),
            PROPSHEET_ROWHEIGHT, 18
            
        
        };
        
        result = maybeEnableIconFilter(result);

        return result;
    }

    /**
     * Enables invert filter for icons if user requested. 
     */
    private Object[] maybeEnableIconFilter(Object[] defaults) {
        if (NbPreferences.forModule(DarculaLAFPanel.class).getBoolean("invertIcons", false)) {
            return appendToArray(defaults, "nb.imageicon.filter", new DarkIconFilter());
        }
        return defaults;
    }

    private Object[] appendToArray(Object[] result, final String key, final Object value) {
        result = Arrays.copyOf(result, result.length + 2);
        result[result.length - 2] = key;
        result[result.length - 1] = value;
        return result;
    }

    /**
     * DarculaLaf:L354-L358 registers ENTER to invoke 'toggle' action. This seems
     * to cause problems as reported in #14 because enter key can not invoke
     * default button in dialogs.
     */
    private void removeEnterFromTreeInputMap() {
        // Make ENTER work in JTrees
        InputMap treeInputMap = (InputMap) UIManager.get("Tree.focusInputMap");
        if (treeInputMap != null) { // it's really possible. For example,  GTK+ doesn't have such map
            treeInputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        }
    }

    /**
     * NOT_FOUND color is hardcoded, should be taken from UIManager.
     * Use reflection as in DefaultOutlineCellRenderer. 
     */
    private static final String SEARCH_BAR_CLASS = "org.netbeans.modules.editor.search.SearchBar";
    private static final String NOT_FOUND_COLOR_FIELD = "NOT_FOUND";
    private void replaceSearchNotFoundColor() {
        replaceFieldValue(SEARCH_BAR_CLASS, NOT_FOUND_COLOR_FIELD, new Color(255, 102, 102));
    }
    
    /**
     * DEFAULT_GUTTER_LINE color is hardcoded, should be taken from UIManager.
     * Use reflection as in DefaultOutlineCellRenderer. 
     */
    private static final String GLYPH_GUUTER_CLASS = "org.netbeans.editor.GlyphGutter";
    private static final String DEFAULT_GUTTER_LINE_COLOR_FIELD = "DEFAULT_GUTTER_LINE";
    private void replaceGlyphGutterLineColor() {
        replaceFieldValue(GLYPH_GUUTER_CLASS, DEFAULT_GUTTER_LINE_COLOR_FIELD, new Color(136, 136, 136));
    }
    
    /**
     * GAP_BORDER_COLOR and SAW_COLOR are hardcoded, should be taken from UIManager.
     * Use reflection as in DefaultOutlineCellRenderer. 
     */
    private static final String FORMDESIGNER_LAYOUT_PAINTER_CLASS = "org.netbeans.modules.form.layoutdesign.LayoutPainter";
    private static final String GAP_BORDER_COLOR_FIELD = "gapBorderColor";
    private static final String SAW_COLOR_FIELD = "sawColor";
    private void replaceFormDesignerGapBorderColors() {
        replaceFieldValue(FORMDESIGNER_LAYOUT_PAINTER_CLASS, GAP_BORDER_COLOR_FIELD, new Color(49, 53, 54));
        replaceFieldValue(FORMDESIGNER_LAYOUT_PAINTER_CLASS, SAW_COLOR_FIELD, new Color(49, 53, 54));
    }

    /**
     * LFCustoms.getTextFgColor() && LFCustoms.getTextFgColorHTML() uses
     * windowText. DarculaLaf does not override windowText which is initialized
     * to Color.BLACK by BasicLookAndFeel (DarculaLaf uses BasicLookAndFeel
     * with reflection)
     */
    private static final String TEXT_FG_COLOR_HTML_FIELD = "textFgColorHTML";
    private static final String TEXT_FG_COLOR_FIELD = "textFgColor";
    private void replaceLFCustomsTextFgColors() {
        replaceFieldValue(LFCustoms.class, TEXT_FG_COLOR_FIELD, new Color(187, 187, 187));
        replaceFieldValue(LFCustoms.class, TEXT_FG_COLOR_HTML_FIELD, "<font color=#bbbbbb>");
    }

    /**
     * #21, #26
     * fixes code completion colors for all languages (at least for those extending GsfCompletionItem)
     */
    private static final String GSF_COMPLETION_FORMATTER_CLASS = "org.netbeans.modules.csl.editor.completion.GsfCompletionItem$CompletionFormatter";
    private static final String PARAMETER_NAME_COLOR_FIELD = "PARAMETER_NAME_COLOR"; //getHTMLColor(160, 96, 1);
    private static final String CLASS_COLOR_FIELD = "CLASS_COLOR"; // getHTMLColor(86, 0, 0);
    private static final String PKG_COLOR_FIELD = "PKG_COLOR"; // getHTMLColor(128, 128, 128);
    private static final String KEYWORD_COLOR_FIELD = "KEYWORD_COLOR"; //getHTMLColor(0, 0, 153);
    private static final String FIELD_COLOR_FIELD = "FIELD_COLOR"; // getHTMLColor(0, 134, 24);
    private static final String VARIABLE_COLOR_FIELD = "VARIABLE_COLOR"; // getHTMLColor(0, 0, 124);
    private static final String CONSTRUCTOR_COLOR_FIELD = "CONSTRUCTOR_COLOR"; // getHTMLColor(178, 139, 0);
    private static final String INTERFACE_COLOR_FIELD = "INTERFACE_COLOR"; // getHTMLColor(64, 64, 64);
    private static final String PARAMETERS_COLOR_FIELD = "PARAMETERS_COLOR"; // getHTMLColor(128, 128, 128);
    private void replaceCompletionColors() {
        replaceFieldValue(GSF_COMPLETION_FORMATTER_CLASS, PARAMETER_NAME_COLOR_FIELD, getHTMLColor(new Color(255, 198, 109)));
        replaceFieldValue(GSF_COMPLETION_FORMATTER_CLASS, CLASS_COLOR_FIELD, getHTMLColor(new Color(214, 128, 128)));
        replaceFieldValue(GSF_COMPLETION_FORMATTER_CLASS, PKG_COLOR_FIELD, getHTMLColor(new Color(128, 214, 128)));
        replaceFieldValue(GSF_COMPLETION_FORMATTER_CLASS, KEYWORD_COLOR_FIELD, getHTMLColor(new Color(180, 180, 255)));
        replaceFieldValue(GSF_COMPLETION_FORMATTER_CLASS, FIELD_COLOR_FIELD, getHTMLColor(new Color(0, 202, 88)));
        replaceFieldValue(GSF_COMPLETION_FORMATTER_CLASS, VARIABLE_COLOR_FIELD, getHTMLColor(new Color(0, 192, 255)));
        replaceFieldValue(GSF_COMPLETION_FORMATTER_CLASS, CONSTRUCTOR_COLOR_FIELD, getHTMLColor(new Color(178, 139, 0)));
        replaceFieldValue(GSF_COMPLETION_FORMATTER_CLASS, INTERFACE_COLOR_FIELD, getHTMLColor(new Color(214, 128, 128)));
        replaceFieldValue(GSF_COMPLETION_FORMATTER_CLASS, PARAMETERS_COLOR_FIELD, getHTMLColor(new Color(64, 128, 64)));
    }

    /**
     * #67, Column color for SQL
     */
    private static final String SQL_COMPLETION_ITEM_CLASS = "org.netbeans.modules.db.sql.editor.completion.SQLCompletionItem";
    private static final String COLUMN_COLOR_FIELD = "COLUMN_COLOR"; // getHtmlColor(7, 7, 171); // NOI18N
    private void replaceSQLCompletionColumnColor() {
        replaceFieldValue(SQL_COMPLETION_ITEM_CLASS, COLUMN_COLOR_FIELD, getHTMLColor(new Color(0, 202, 88)));  
    }
    
    /**
     * JSP completion colors
     */
    private static final String JSP_COMPLETION_ITEM_CLASS = "org.netbeans.modules.web.core.syntax.completion.api.JspCompletionItem";
    private static final String JSP_COLOR_BASE_COMPLETION = "COLOR_BASE_COMPLETION";
    private void replaceJSPCompletionColor() {
        replaceFieldValue(JSP_COMPLETION_ITEM_CLASS, JSP_COLOR_BASE_COMPLETION, new Color(204, 105, 50));
    }

    /**
     * #106
     * HTML completion colors for HTML Tags and Custom Tags
     */
    private static final String HTML_COMPLETION_ITEM_CLASS = "org.netbeans.modules.html.editor.api.completion.HtmlCompletionItem$Tag";
    private static final String CUSTOM_TAG_COMPLETION_ITEM_CLASS = "org.netbeans.modules.html.custom.CustomTagCompletionItem";
    private static final String HTML_DEFAULT_FG_COLOR = "DEFAULT_FG_COLOR";
    private void replaceHTMLCompletionColor() {
        replaceFieldValue(HTML_COMPLETION_ITEM_CLASS, HTML_DEFAULT_FG_COLOR, new Color(232, 191, 106));
        replaceFieldValue(CUSTOM_TAG_COMPLETION_ITEM_CLASS, HTML_DEFAULT_FG_COLOR, new Color(64, 127, 255));
    } 

    /**
     * #91, CSS selector and preprocessor completion colors (LESS and SASS)
     */
    private static final String CP_COMPLETION_ITEM_CLASS = "org.netbeans.modules.css.prep.editor.CPCompletionItem";
    private static final String CP_LHS_COLOR_FIELD = "COLOR";
    private static final String CP_RHS_COLOR_FIELD = "ORIGIN_COLOR";
    private void replaceCSSPreprocessorCompletionColors() {
        replaceFieldValue(CP_COMPLETION_ITEM_CLASS, CP_LHS_COLOR_FIELD, new Color(0, 164, 164));
        replaceFieldValue(CP_COMPLETION_ITEM_CLASS, CP_RHS_COLOR_FIELD, new Color(255, 255, 255));
    }

    /**
     * #85, #88
     * Tab colors for files belonging to same project
     */
    private static final String PROJECT_COLOR_TAB_DECORATOR_CLASS = "org.netbeans.core.multitabs.impl.ProjectColorTabDecorator";
    private static final String BACKGROUND_COLORS_FIELD = "backGroundColors";
    private void replaceProjectTabColors() {
        List<Color> backgroundColors = new ArrayList<Color>();
        backgroundColors.add(new Color(96, 135, 117));
        backgroundColors.add(new Color(135, 101, 101));
        backgroundColors.add(new Color(135, 127, 94));
        backgroundColors.add(new Color(96, 119, 135));
        backgroundColors.add(new Color(121, 135, 89));
        backgroundColors.add(new Color(135, 105, 89));
        backgroundColors.add(new Color(108, 135, 96));
        backgroundColors.add(new Color(107, 135, 38));
        backgroundColors.add(new Color(118, 89, 135));

        replaceFieldValue(PROJECT_COLOR_TAB_DECORATOR_CLASS, BACKGROUND_COLORS_FIELD, backgroundColors);
    }

    private static String getHTMLColor(Color c) {
        return "<font color=#" //NOI18N
                + LFCustoms.getHexString(c.getRed())
                + LFCustoms.getHexString(c.getGreen())
                + LFCustoms.getHexString(c.getBlue())
                + ">"; //NOI18N
    }

    private void replaceFieldValue(String className, String fieldName, Object value) {

        Class<?> sbClass = null;
        try {
            sbClass = ClassLoader.getSystemClassLoader().loadClass(className);
        } catch (ClassNotFoundException ex) {
            try {
                sbClass = Thread.currentThread().getContextClassLoader().loadClass(className);
            } catch (ClassNotFoundException ex1) {
                try {
                    ClassLoader systemClassLoader = (ClassLoader) Lookup.getDefault().lookup(ClassLoader.class);
                    if (systemClassLoader != null) {

                        sbClass = systemClassLoader.loadClass(className);
                    }
                } catch (ClassNotFoundException ex2) {
                    Logger.getLogger(DarculaLFCustoms.class.getName()).log(Level.FINE,
                            "Can not find class, will not be able to replace its field...", ex2);
                }
            }
        } catch (SecurityException ex) {
            Logger.getLogger(DarculaLFCustoms.class.getName()).log(Level.FINE,
                    "Can not find class, will not be able to replace its field...", ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(DarculaLFCustoms.class.getName()).log(Level.FINE,
                    "Can not find class, will not be able to replace its field...", ex);
        }

        if (sbClass == null) {
            return;
        }

        replaceFieldValue(sbClass, fieldName, value);

    }
    
    private void replaceFieldValue(Class<?> clazz, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(null, value);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(DarculaLFCustoms.class.getName()).log(Level.FINE,
                    "Can not replace field...", ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(DarculaLFCustoms.class.getName()).log(Level.FINE,
                    "Can not replace field...", ex);
        } catch (SecurityException ex) {
            Logger.getLogger(DarculaLFCustoms.class.getName()).log(Level.FINE,
                    "Can not replace field...", ex);
        }
    }
    
}