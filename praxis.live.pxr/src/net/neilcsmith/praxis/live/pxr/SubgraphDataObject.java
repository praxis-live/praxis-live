/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.pxr;

import java.io.IOException;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

@Messages({
    "LBL_Subgraph_LOADER=Files of Subgraph"
})
@MIMEResolver.ExtensionRegistration(
    displayName = "#LBL_Subgraph_LOADER",
mimeType = "text/x-praxis-subgraph",
extension = {"pxg"})
@DataObject.Registration(
    mimeType = "text/x-praxis-subgraph",
iconBase = "net/neilcsmith/praxis/live/pxr/resources/pxg16.png",
displayName = "#LBL_Subgraph_LOADER",
position = 300)
@ActionReferences({
//    @ActionReference(
//        path = "Loaders/text/x-praxis-subgraph/Actions",
//    id =
//    @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
//    position = 100,
//    separatorAfter = 200),
    @ActionReference(
        path = "Loaders/text/x-praxis-subgraph/Actions",
    id =
    @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
    position = 300),
    @ActionReference(
        path = "Loaders/text/x-praxis-subgraph/Actions",
    id =
    @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
    position = 400,
    separatorAfter = 500),
    @ActionReference(
        path = "Loaders/text/x-praxis-subgraph/Actions",
    id =
    @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
    position = 600),
    @ActionReference(
        path = "Loaders/text/x-praxis-subgraph/Actions",
    id = @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
    position = 700,
    separatorAfter = 800),
    @ActionReference(
        path = "Loaders/text/x-praxis-subgraph/Actions",
    id =
    @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
    position = 1100,
    separatorAfter = 1200),
    @ActionReference(
        path = "Loaders/text/x-praxis-subgraph/Actions",
    id =
    @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
    position = 1300),
    @ActionReference(
        path = "Loaders/text/x-praxis-subgraph/Actions",
    id =
    @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
    position = 1400)
})
public class SubgraphDataObject extends MultiDataObject {

    public SubgraphDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
    }

    @Override
    protected int associateLookup() {
        return 1;
    }
}
