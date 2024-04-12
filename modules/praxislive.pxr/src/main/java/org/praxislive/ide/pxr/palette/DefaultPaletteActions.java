package org.praxislive.ide.pxr.palette;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.filechooser.FileFilter;
import org.netbeans.spi.palette.PaletteActions;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 *
 */
class DefaultPaletteActions extends PaletteActions {
    
    private final static RequestProcessor RP = new RequestProcessor(DefaultPaletteActions.class);
    
    private static Action[] EMPTY_ACTIONS = new Action[0];
    
    @Override
    public Action[] getImportActions() {
        return EMPTY_ACTIONS;
    }
    
    @Override
    public Action[] getCustomPaletteActions() {
        return EMPTY_ACTIONS;
    }
    
    @Override
    public Action[] getCustomCategoryActions(Lookup category) {
        DataFolder folder = category.lookup(DataFolder.class);
        if (folder != null) {
            return new Action[]{
                new ImportAction(folder)
            };
        } else {
            return EMPTY_ACTIONS;
        }
        
    }
    
    @Override
    public Action[] getCustomItemActions(Lookup item) {
        return EMPTY_ACTIONS;
    }
    
    @Override
    public Action getPreferredAction(Lookup item) {
        return null;
    }
    
    private static class ImportAction extends AbstractAction {
        
        private DataFolder folder;
        
        private ImportAction(DataFolder folder) {
            super("Import...");
            this.folder = folder;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            final File file = new FileChooserBuilder(ImportAction.class)
                    .setTitle("Import Subgraph")
                    .setFileHiding(true)
                    .setFilesOnly(true)
                    .setApproveText("Import")
                    .setFileFilter(new FileFilter() {
                        
                        @Override
                        public boolean accept(File f) {
                            return f.isDirectory() || f.getName().toLowerCase().endsWith(".pxg");
                        }
                        
                        @Override
                        public String getDescription() {
                            return "SubGraph (.pxg)";
                        }
                    })
                    .showOpenDialog();
            if (file != null) {
                RP.post(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            FileObject pxg = FileUtil.toFileObject(file);
                            FileUtil.copyFile(
                                    pxg,
                                    folder.getPrimaryFile(),
                                    pxg.getName());
                        } catch (Exception ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                });
            }
        }
        
    }
    
}
