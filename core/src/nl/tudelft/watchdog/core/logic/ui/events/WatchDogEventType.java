package nl.tudelft.watchdog.core.logic.ui.events;

import nl.tudelft.watchdog.core.logic.document.Document;
import nl.tudelft.watchdog.core.logic.document.EditorWrapperBase;
import nl.tudelft.watchdog.core.logic.interval.IDEIntervalManagerBase;
import nl.tudelft.watchdog.core.logic.interval.intervaltypes.*;
import nl.tudelft.watchdog.core.logic.ui.InActivityNotifiers;

import java.util.Date;

/** The different type of events. */
@SuppressWarnings("javadoc")
public enum WatchDogEventType implements WatchDogEventTypeInterface {

    ACTIVE_WINDOW {
        @Override
        public void process(Date forcedDate, Object source) {
            IDEActiveInterval interval = intervalManager.getInterval(IDEActiveInterval.class);
            if (isClosed(interval)) {
                intervalManager.addInterval(new IDEActiveInterval(forcedDate));
            }
            InActivityNotifiers.USER_INACTIVITY.trigger(forcedDate);
        }
    }, INACTIVE_WINDOW {
        @Override
        public void process(Date forcedDate, Object source) {
            IDEActiveInterval interval = intervalManager.getInterval(IDEActiveInterval.class);
            intervalManager.closeInterval(interval, forcedDate);
        }
    }, START_IDE {
        @Override
        public void process(Date forcedDate, Object source) {
            intervalManager.addInterval(new IDEOpenInterval(forcedDate));
            InActivityNotifiers.USER_INACTIVITY.trigger(forcedDate);
        }
    }, END_IDE {
        @Override
        public void process(Date forcedDate, Object source) {
            InActivityNotifiers.USER_INACTIVITY.cancelTimer(forcedDate);
        }
    },

    ACTIVE_FOCUS {
        @Override
        public void process(Date forcedDate, Object editor) {
            processReadingEvent(forcedDate, editor);
        }
    }, INACTIVE_FOCUS {
        @Override
        public void process(Date forcedDate, Object source) {
            EditorIntervalBase editorInterval = intervalManager.getEditorInterval();
            intervalManager.closeInterval(editorInterval, forcedDate);
            InActivityNotifiers.READING.cancelTimer(forcedDate);
            InActivityNotifiers.TYPING.cancelTimer(forcedDate);
        }
    }, SUBSEQUENT_EDIT {
        @Override
        public void process(Date forcedDate, Object source) {
            EditorWithModCount editorWithModCount = (EditorWithModCount) source;
            EditorIntervalBase editorInterval = intervalManager.getEditorInterval();

            if (isClosed(editorInterval)
                    || !intervalExistsAndIsOfType(editorInterval, IntervalType.TYPING)
                    || isDifferentEditor(editorInterval, editorWithModCount.editor)) {
                WatchDogEventType.START_EDIT.process(new Date(), editorWithModCount.editor);
                return;
            }

            // Increase modCount for the current typing interval
            TypingInterval typingInt = (TypingInterval) editorInterval;
            typingInt.increaseModCountWith(editorWithModCount.modCount);

            InActivityNotifiers.TYPING.trigger();
            InActivityNotifiers.USER_INACTIVITY.trigger(forcedDate);
        }
    }, START_EDIT {
        @Override
        public void process(Date forcedDate, Object editor) {
            EditorIntervalBase editorInterval = intervalManager
                    .getEditorInterval();

            InActivityNotifiers.READING.cancelTimer(forcedDate);
            if (intervalExistsAndIsOfType(editorInterval, IntervalType.TYPING)
                    && !isDifferentEditor(editorInterval, editor)) {
                return;
            }

            intervalManager.closeInterval(editorInterval, forcedDate);

            TypingInterval typingInterval = new TypingInterval(
                    editorSpecificImplementation.createEditorWrapper(editor), forcedDate);
            Document document;
            if (editorInterval != null
                    && !isDifferentEditor(editorInterval, editor)) {
                document = editorInterval.getDocument();
            } else {
                document = editorSpecificImplementation.createDocument(editor);
            }
            typingInterval.setDocument(document);
            intervalManager.addInterval(typingInterval);

            InActivityNotifiers.TYPING.trigger();
            InActivityNotifiers.USER_INACTIVITY.trigger(forcedDate);
        }
    }, CARET_MOVED {
        @Override
        public void process(Date forcedDate, Object editor) {
            processReadingEvent(forcedDate, editor);
        }
    }, PAINT {
        @Override
        public void process(Date forcedDate, Object editor) {
            processReadingEvent(forcedDate, editor);
        }
    },
    START_PERSPECTIVE {
        @Override
        public void process(Date forcedDate, Object source) {
            PerspectiveInterval.Perspective perspective = (PerspectiveInterval.Perspective) source;
            createNewPerspectiveInterval(perspective, forcedDate);
            InActivityNotifiers.USER_INACTIVITY.trigger(forcedDate);
        }
    }, JUNIT {
        @Override
        public void process(Date forcedDate, Object source) {
            editorSpecificImplementation.addJUnitInterval(source);
        }
    },

    USER_ACTIVITY {
        @Override
        public void process(Date forcedDate, Object source) {
            UserActiveInterval interval = intervalManager.getInterval(UserActiveInterval.class);
            if (isClosed(interval)) {
                intervalManager.addInterval(new UserActiveInterval(forcedDate));
            }
            InActivityNotifiers.USER_INACTIVITY.trigger();
        }
    }, USER_INACTIVITY {
        @Override
        public void process(Date forcedDate, Object source) {
            IntervalBase interval = intervalManager.getInterval(UserActiveInterval.class);
            intervalManager.closeInterval(interval, forcedDate);
            InActivityNotifiers.TYPING.cancelTimer(forcedDate);
            InActivityNotifiers.READING.cancelTimer(forcedDate);
        }
    }, TYPING_INACTIVITY {
        @Override
        public void process(Date forcedDate, Object source) {
            EditorIntervalBase editorInterval = intervalManager.getEditorInterval();
            if (intervalExistsAndIsOfType(editorInterval, IntervalType.TYPING)) {
                intervalManager.closeInterval(editorInterval, forcedDate);
            }
        }
    }, READING_INACTIVITY {
        @Override
        public void process(Date forcedDate, Object source) {
            EditorIntervalBase editorInterval = intervalManager.getEditorInterval();
            if (intervalExistsAndIsOfType(editorInterval, IntervalType.READING)) {
                intervalManager.closeInterval(editorInterval, forcedDate);
            }
        }
    },

    START_WATCHDOGVIEW {
        @Override
        public void process(Date forcedDate, Object source) {
            WatchDogViewInterval interval = intervalManager.getInterval(WatchDogViewInterval.class);
            if (!intervalExistsAndIsOfType(interval, IntervalType.WATCHDOGVIEW)) {
                intervalManager
                        .addInterval(new WatchDogViewInterval(forcedDate));
            }
            InActivityNotifiers.USER_INACTIVITY.trigger(forcedDate);
        }
    }, END_WATCHDOGVIEW {
        @Override
        public void process(Date forcedDate, Object source) {
            WatchDogViewInterval interval = intervalManager.getInterval(WatchDogViewInterval.class);
            if (intervalExistsAndIsOfType(interval, IntervalType.WATCHDOGVIEW)) {
                intervalManager.closeInterval(interval, forcedDate);
            }
        }
    }, START_DEBUG {
        @Override
        public void process(Date forcedDate, Object source) {
            DebugInterval interval = intervalManager.getInterval(DebugInterval.class);
            if(!intervalExistsAndIsOfType(interval, IntervalType.DEBUG)) {
                intervalManager.addInterval(new DebugInterval(forcedDate));
            }
            InActivityNotifiers.USER_INACTIVITY.trigger(forcedDate);
        }
    }, END_DEBUG {
        @Override
        public void process(Date forcedDate, Object source) {
            DebugInterval interval = intervalManager.getInterval(DebugInterval.class);
            if(intervalExistsAndIsOfType(interval, IntervalType.DEBUG)) {
                intervalManager.closeInterval(interval, forcedDate);
            }
        }
    };

    public static IDEIntervalManagerBase intervalManager;
    public static WatchDogEventEditorSpecificImplementation editorSpecificImplementation;

    private static boolean needToCreateNewReadingInterval(EditorIntervalBase editorInterval, Object editor) {
        return isClosed(editorInterval) || isDifferentEditor(editorInterval, editor);
    }

    private static boolean isDifferentEditor(EditorIntervalBase editorInterval, Object editor) {
        return editorInterval.getEditorWrapper().getEditor() != editor;
    }

    private static boolean intervalExistsAndIsOfType(IntervalBase interval, IntervalType type) {
        return interval != null && interval.getType() == type;
    }

    private static boolean isClosed(IntervalBase interval) {
        return interval == null || interval.isClosed();
    }

    /** Creates a new perspective Interval of the given type. */
    private static void createNewPerspectiveInterval(PerspectiveInterval.Perspective perspectiveType, Date forcedDate) {
        PerspectiveInterval perspectiveInterval = intervalManager.getInterval(PerspectiveInterval.class);
        if (perspectiveInterval != null && perspectiveInterval.getPerspectiveType() == perspectiveType) {
            // abort if such an interval is already open.
            return;
        }
        intervalManager.closeInterval(perspectiveInterval, forcedDate);
        intervalManager.addInterval(new PerspectiveInterval(perspectiveType, forcedDate));
    }

    private static void processReadingEvent(Date forcedDate, Object editor) {
        EditorIntervalBase editorInterval = intervalManager.getEditorInterval();

        if (needToCreateNewReadingInterval(editorInterval, editor)) {
            if (!isClosed(editorInterval)) {
                intervalManager.closeInterval(editorInterval, forcedDate);
            }

            ReadingInterval readingInterval = new ReadingInterval(editorSpecificImplementation.createEditorWrapper(editor), forcedDate);
            readingInterval.setDocument(editorSpecificImplementation.createDocument(editor));
            intervalManager.addInterval(readingInterval);
        }

        InActivityNotifiers.READING.trigger();
        InActivityNotifiers.USER_INACTIVITY.trigger(forcedDate);
    }

    public static class EditorWithModCount {
        private final Object editor;
        private final int modCount;

        public EditorWithModCount(Object editor, int modCount) {
            this.editor = editor;
            this.modCount = modCount;
        }
    }

    public interface WatchDogEventEditorSpecificImplementation {
        void addJUnitInterval(Object source);

        EditorWrapperBase createEditorWrapper(Object editor);

        Document createDocument(Object editor);

        default void updatePerspectiveInterval() {}
    }
}
