// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.talend.commons.utils.VersionUtils;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.ui.views.jobsettings.tabs.ProcessVersionComposite;

/**
 * DOC xye class global comment. Detailled comment
 */
public class OpenExistVersionProcessPage extends WizardPage {

    private final static String TITLE = Messages.getString("OpenExistVersionProcess.open.title"); //$NON-NLS-1$

    private final IRepositoryObject processObject;

    private ProcessVersionComposite versionListComposite;

    /** Version text. */
    protected Text versionText;

    /** Version upgrade major button. */
    private Button versionMajorBtn;

    /** Version upgrade minor button. */
    private Button versionMinorBtn;

    private Button createNewVersionButton;

    private Composite versionModifComposite;

    private String originVersion = null;

    private boolean createNewVersionJob = false;

    private boolean alreadyLockedByUser = false;

    /**
     * DOC xye OpenExistVersionProcessPage constructor comment.
     * 
     * @param pageName
     */
    protected OpenExistVersionProcessPage(final boolean alreadyLockedByUser, final IRepositoryObject processObject) {
        super("OpenExistVersionProcessPage");
        setTitle(TITLE);
        setMessage("If this job is locked by user, then you can't create new version job!");
        this.processObject = processObject;
        originVersion = getProperty().getVersion();
        this.alreadyLockedByUser = alreadyLockedByUser;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        parent.setLayout(new GridLayout());
        versionListComposite = new ProcessVersionComposite(parent, SWT.NULL, null, processObject);
        versionListComposite.setParentWizard(this);
        versionListComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        setControl(versionListComposite);
        versionListComposite.refresh();

        createNewVersionButton = new Button(parent, SWT.CHECK);
        createNewVersionButton.setText("Create new version and open it?");
        createNewVersionButton.setEnabled(!alreadyLockedByUser);

        Composite bc = new Composite(parent, SWT.NULL);
        bc.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        bc.setLayout(layout);

        // Create Version
        Label versionLab = new Label(bc, SWT.NONE);
        versionLab.setText(Messages.getString("PropertiesWizardPage.Version")); //$NON-NLS-1$

        versionModifComposite = new Composite(bc, SWT.NONE);
        versionModifComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout versionLayout = new GridLayout(3, false);
        versionLayout.marginHeight = 0;
        versionLayout.marginWidth = 0;
        versionLayout.horizontalSpacing = 0;
        versionModifComposite.setLayout(versionLayout);

        versionText = new Text(versionModifComposite, SWT.BORDER);
        versionText.setEnabled(false);
        versionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        versionMajorBtn = new Button(versionModifComposite, SWT.PUSH);
        versionMajorBtn.setText(Messages.getString("PropertiesWizardPage.Version.Major")); //$NON-NLS-1$
        versionMajorBtn.setEnabled(true);

        versionMinorBtn = new Button(versionModifComposite, SWT.PUSH);
        versionMinorBtn.setText(Messages.getString("PropertiesWizardPage.Version.Minor")); //$NON-NLS-1$
        versionMinorBtn.setEnabled(true);

        versionText.setText(getProperty().getVersion());

        addListener();

        setPageComplete(false);
    }

    private void addListener() {

        createNewVersionButton.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {

            }

            public void widgetSelected(SelectionEvent e) {
                boolean create = createNewVersionButton.getSelection();
                versionModifComposite.setEnabled(create);
                versionListComposite.setEnabled(!create);
                createNewVersionJob = create;
                updatePageStatus();
            }

        });

        versionMajorBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                String version = getProperty().getVersion();
                version = VersionUtils.upMajor(version);
                versionText.setText(version);
                getProperty().setVersion(version);
                updatePageStatus();
            }
        });

        versionMinorBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                String version = getProperty().getVersion();
                version = VersionUtils.upMinor(version);
                versionText.setText(version);
                getProperty().setVersion(version);
                updatePageStatus();
            }
        });
    }

    public void updatePageStatus() {
        if (createNewVersionButton.getSelection()) {
            setPageComplete(!versionText.getText().equals(getOriginVersion()));
            if (!isPageComplete()) {
                setErrorMessage("Please set new version");
            } else {
                setErrorMessage(null);
            }
        } else {
            setPageComplete(getSelection() != null);
        }
    }

    public Object getSelection() {
        return versionListComposite.getSelection();
    }

    private Property getProperty() {
        return processObject.getProperty();
    }

    public String getOriginVersion() {
        return this.originVersion;
    }

    public boolean isCreateNewVersionJob() {
        return this.createNewVersionJob;
    }
}
