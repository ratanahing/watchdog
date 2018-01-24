package nl.tudelft.watchdog.intellij.logic.ui.listeners;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.*;

import nl.tudelft.watchdog.core.logic.ui.events.WatchDogEvent;
import nl.tudelft.watchdog.intellij.logic.ui.WatchDogEventManager;
import nl.tudelft.watchdog.core.logic.ui.events.EditorEvent;

/** Editor listener for all user-triggered events. */
public class EditorListener {
    private final Editor editor;
    private final Document document;

    private DocumentListener documentListener;
    private CaretListener caretListener;
    private VisibleAreaListener  visibleAreaListener;

    /** Enriches the supplied editor with all suitable listeners. */
    public EditorListener(Editor editor) {
        this.editor = editor;
        this.document = editor.getDocument();
        listenToDocumentChanges();
        listenToEditorScrolling();
    }

    /**
     * Adds a document change listener to the supplied editor. Fires a
     * {@link EditorEvent} when a change to a document is made.
     *
     * @throws IllegalArgumentException
     */
    private void listenToDocumentChanges() throws IllegalArgumentException {
        documentListener = new DocumentListener() {

            @Override
            public void beforeDocumentChange(DocumentEvent event) {
                new EditorEvent(editor, WatchDogEvent.EventType.START_EDIT).update();
            }

            @Override
            public void documentChanged(DocumentEvent event) {
                /*
                 * Three events exist that can influence the Levenshtein distance:
                 * 1. Addition. In this case old_length=0 and new_length>0, therefore max(old_length,new_length)=new_length=Levenshtein distance.
                 * 2. Removal. In this case old_length>0 and new_length=0, therefore max(old_length,new_length)=old_length=Levenshtein distance.
                 * 3. Modification. In this case old_length>0 and new_length>0, therefore max(old_length,new_length)>=Levenshtein distance.
                 *
                 * However, when you modify something by selecting it and then pressing a key, the modCount is off by 1 compared to the one computed
                 * in Eclipse for the same changes. This is because IntelliJ generates 2 events in this particular situation (first removal, then
                 * addition of 1 character). Unfortunately, detecting this particular situation seems to be impossible or quite difficult.
                 *
                 * So, in general it holds that modCount >= Levenshtein distance.
                 */
                int new_length = event.getNewFragment().length();
                int old_length = event.getOldFragment().length();
                int modCount = Math.max(old_length, new_length);
                EditorEvent edEvent = new EditorEvent(editor, WatchDogEvent.EventType.SUBSEQUENT_EDIT);
                edEvent.setModCount(modCount);
                edEvent.update();
            }

        };
        document.addDocumentListener(documentListener);
    }

    private void listenToEditorScrolling() {
        // creates a listener for when the user moves the caret (cursor)
        caretListener = new CaretListener() {
            @Override
            public void caretPositionChanged(CaretEvent e) {
                new EditorEvent(editor, WatchDogEvent.EventType.CARET_MOVED).update();
                // cursor place changed
            }

            @Override
            public void caretAdded(CaretEvent e) {
                // Intentionally left blank
            }

            @Override
            public void caretRemoved(CaretEvent e) {
                // Intentionally left blank
            }
        };
        editor.getCaretModel().addCaretListener(caretListener);

        // creates a listener for redraws of the view, e.g. when scrolled
        visibleAreaListener = new VisibleAreaListener() {
            @Override
            public void visibleAreaChanged(VisibleAreaEvent e) {
                if(e.getEditor().isViewer()) {
                    new EditorEvent(editor, WatchDogEvent.EventType.PAINT).update();
                }
            }
        };
        editor.getScrollingModel().addVisibleAreaListener(visibleAreaListener);

    }

    /** Removes all listeners registered with this editor. */
    public void removeListeners() {
        document.removeDocumentListener(documentListener);
        if (editor == null) {
            return;
        }
        editor.getCaretModel().removeCaretListener(caretListener);
        editor.getScrollingModel().removeVisibleAreaListener(visibleAreaListener);
    }
}