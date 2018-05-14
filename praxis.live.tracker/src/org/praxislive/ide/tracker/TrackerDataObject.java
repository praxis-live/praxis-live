package org.praxislive.ide.tracker;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.praxislive.core.ValueFormatException;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.cookies.OpenCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.CookieSet;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;

@Messages({
    "LBL_Tracker_LOADER=Files of Tracker"
})
@MIMEResolver.ExtensionRegistration(
        displayName = "#LBL_Tracker_LOADER",
        mimeType = "text/x-praxis-tracker",
        extension = {"pxt"}
)
@DataObject.Registration(
        mimeType = "text/x-praxis-tracker",
        iconBase = "org/praxislive/ide/tracker/resources/patterns.png",
        displayName = "#LBL_Tracker_LOADER",
        position = 300
)
@ActionReferences({
    @ActionReference(
            path = "Loaders/text/x-praxis-tracker/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200
    ),
    @ActionReference(
            path = "Loaders/text/x-praxis-tracker/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
            position = 1400
    )
})
public class TrackerDataObject extends MultiDataObject {

    private final static RequestProcessor RP = new RequestProcessor();

    private final PatternsListener patternsListener;
    private final Saver saver;

    private TrackerTopComponent editor;
    private Patterns patterns;

    public TrackerDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        CookieSet cookies = getCookieSet();
        cookies.add(new Opener());
        patternsListener = new PatternsListener();
        saver = new Saver();
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    @Override
    public void setModified(boolean modified) {
        assert EventQueue.isDispatchThread();
        if (isModified() == modified) {
            return;
        }
        super.setModified(modified);
        if (isValid()) {
            if (modified) {
                getCookieSet().add(saver);
            } else {
                getCookieSet().remove(saver);
            }
            if (editor != null) {
                editor.setModified(modified);
            }
        }
    }

    @Override
    protected void dispose() {
        assert EventQueue.isDispatchThread();
        super.dispose();
        if (editor != null) {
            TrackerTopComponent ed = editor;
            editor = null;
            ed.setModified(false);
            ed.close();
        }
    }

    void editorClosed(TrackerTopComponent editor) {
        if (this.editor == editor) {
            this.editor = null;
            updatePatterns(null);
        }
    }

    private void createEditor() {
        editor = new TrackerTopComponent(TrackerDataObject.this);
        RP.post(new Runnable() {

            @Override
            public void run() {
                try {
                    String data = getPrimaryFile().asText();
                    final Patterns patterns
                            = TrackerUtils.parse(data);
                    EventQueue.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            updatePatterns(patterns);
                        }
                    });
                } catch (IOException | ValueFormatException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
    }

    private void updatePatterns(Patterns patterns) {
        if (this.patterns != null) {
            this.patterns.removeChangeListener(patternsListener);
            this.patterns = null;
        }
        setModified(false);
        if (patterns != null && editor != null) {
            this.patterns = patterns;
            patterns.addChangeListener(patternsListener);
            editor.updatePatterns(patterns);
        }
    }

    private class Opener implements OpenCookie {

        @Override
        public void open() {
            if (editor == null) {
                createEditor();
            }
            editor.open();
            editor.requestActive();
        }

    }

    private class Saver implements SaveCookie {

        @Override
        public void save() throws IOException {
            assert EventQueue.isDispatchThread();
            if (editor != null) {
                editor.finishEditing();
            }
            setModified(false);
            final String data = TrackerUtils.write(patterns);
            RP.post(new Runnable() {

                @Override
                public void run() {
                    Writer writer = null;
                    try {
                        FileObject file = getPrimaryFile();
                        writer = new OutputStreamWriter(file.getOutputStream());
                        writer.append(data);
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    } finally {
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                    }
                }
            });
        }

    }

    private class PatternsListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            setModified(true);
        }
    }

}
