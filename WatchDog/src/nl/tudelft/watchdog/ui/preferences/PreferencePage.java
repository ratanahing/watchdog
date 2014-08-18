package nl.tudelft.watchdog.ui.preferences;

import nl.tudelft.watchdog.ui.UIUtils;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * The WatchDog preference page in the Eclipse preference settings.
 */
public class PreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	/** The length of a WatchDog id. */
	private static final int ID_LENGTH = 40;

	/** The project ID input field for this workspace. */
	private Text projectIDInput;

	/** Whether WatchDog should be enabled in this workspace. */
	private Button enableWatchdogInput;

	/** WatchDog preferences. */
	private Preferences preferences = Preferences.getInstance();

	/** This workspace. */
	private String workspace = UIUtils.getWorkspaceName();

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Preferences.getInstance().getStore());
		setDescription("Settings for WatchDog");
	}

	@Override
	protected Control createContents(Composite parent) {
		UIUtils.createLabel(
				"WatchDog has local settings (different per workspace) and global settings (per Eclipse installation).",
				parent);
		UIUtils.createLabel("", parent);
		Group localGroup = createGroup(parent, "Local Settings", 1);
		UIUtils.createLabel(
				"Here you can define the local WatchDog settings for this workspace.",
				localGroup);
		UIUtils.createLabel("", localGroup);

		Composite projectComposite = UIUtils.createGridedComposite(localGroup,
				2);
		projectComposite.setLayoutData(UIUtils.createFullGridUsageData());
		UIUtils.createLabel("Project-ID ", projectComposite);
		projectIDInput = UIUtils.createTextInput(projectComposite);
		projectIDInput.setTextLimit(ID_LENGTH);
		enableWatchdogInput = new Button(localGroup, SWT.CHECK);
		enableWatchdogInput.setText("Monitor this workspace with WatchDog ");

		WorkspacePreferenceSetting workspaceSetting = preferences
				.getOrCreateWorkspaceSetting(workspace);
		projectIDInput.setText(workspaceSetting.projectId);
		enableWatchdogInput.setSelection(workspaceSetting.enableWatchdog);

		UIUtils.createLabel("", parent);

		Group globalGroup = createGroup(parent, "Global Settings", 1);
		UIUtils.createLabel(
				"Here you can enter settings that will affect WatchDog no matter which workspace you have opened.  ",
				globalGroup);
		UIUtils.createLabel("", globalGroup);

		return super.createContents(globalGroup);
	}

	@Override
	public boolean performOk() {
		boolean returnStatus = super.performOk();
		// TODO (MMB) hook into me
		preferences.registerWorkspaceProject(workspace,
				projectIDInput.getText());
		preferences.registerWorkspaceUse(workspace,
				enableWatchdogInput.getSelection());
		return returnStatus;
	}

	@Override
	protected void createFieldEditors() {
		addField(new IDFieldEditor(Preferences.USERID_KEY, "User-ID"));
		addField(new URLFieldEditor(Preferences.SERVER_KEY, "Server-URL"));
		addField(new BooleanFieldEditor(Preferences.AUTHENTICATION_ENABLED_KEY,
				"Enable Authentication", getFieldEditorParent()));
		addField(new BooleanFieldEditor(Preferences.LOGGING_ENABLED_KEY,
				"Enable Logging", getFieldEditorParent()));
	}

	/** A specific field editor allowing input of valid user URLs only. */
	class URLFieldEditor extends StringFieldEditor {
		/** Constructor, delegating call to parent's constructor. */
		public URLFieldEditor(String key, String description) {
			super(key, description, getFieldEditorParent());
		}

		/**
		 * This hack-ish implementation allows for a modification of the inputed
		 * value in the text field, by first saving the unchecked value in the
		 * preferences, then loading it in again, then performing the URL checks
		 * for a trailing / and then saving it.
		 * 
		 * Finally, it reloads the value to give imminent user-feedback.
		 */
		@Override
		protected void doStore() {
			super.doStore();
			super.doLoad();
			String url = oldValue;
			url = url.trim();
			if (!url.endsWith("/")) {
				url = url.concat("/");
			}
			getPreferenceStore().setValue(getPreferenceName(), url);
			super.doLoad();
		}
	}

	/** A specific field editor allowing input of valid user IDs only. */
	class IDFieldEditor extends StringFieldEditor {
		/** Constructor, delegating call to parent's constructor. */
		public IDFieldEditor(String key, String description) {
			super(key, description, getFieldEditorParent());
		}

		@Override
		protected void createControl(Composite parent) {
			super.createControl(parent);
			setTextLimit(40);
		}
	}

	/**
	 * Creates and returns a {@link Group} with an enclosed GridData layout with
	 * the given number of columns.
	 */
	private Group createGroup(Composite area, String name, int columns) {
		Group group = new Group(area, SWT.None);
		group.setText(name);
		group.setLayout(new GridLayout(columns, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
				| GridData.GRAB_HORIZONTAL));
		return group;
	}
}
