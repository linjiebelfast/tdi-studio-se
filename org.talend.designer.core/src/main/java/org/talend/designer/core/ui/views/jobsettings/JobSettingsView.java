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
package org.talend.designer.core.ui.views.jobsettings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.ViewPart;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.core.CorePlugin;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.EmptyRepositoryObject;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.properties.tab.HorizontalTabFactory;
import org.talend.core.properties.tab.IDynamicProperty;
import org.talend.core.properties.tab.TalendPropertyTabDescriptor;
import org.talend.core.ui.images.ECoreImage;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.process.AbstractProcessProvider;
import org.talend.designer.core.ui.AbstractMultiPageTalendEditor;
import org.talend.designer.core.ui.editor.AbstractTalendEditor;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.core.ui.views.jobsettings.tabs.MainComposite;
import org.talend.designer.core.ui.views.jobsettings.tabs.ProcessVersionComposite;
import org.talend.designer.core.ui.views.properties.IJobSettingsView;
import org.talend.designer.core.ui.views.statsandlogs.StatsAndLogsComposite;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNode.EProperties;

/**
 * DOC ggu class global comment. Detailled comment
 */
public class JobSettingsView extends ViewPart implements IJobSettingsView, ISelectionChangedListener {

    /**
     * 
     */
    private static final String SEPARATOR = "->";

    public static final String VIEW_NAME = Messages.getString("JobSettingsView.JobSettings"); //$NON-NLS-1$

    public static final String VIEW_NAME_JOBLET = "Joblet";

    private HorizontalTabFactory tabFactory = null;

    private TalendPropertyTabDescriptor currentSelectedTab;

    private Element element;

    private boolean cleaned;

    private boolean selectedPrimary;

    private Process process;

    public JobSettingsView() {
        tabFactory = new HorizontalTabFactory();
        CorePlugin.getDefault().getRepositoryService().addRepositoryTreeViewListener(this);
    }

    @Override
    public void createPartControl(Composite parent) {
        // tabFactory = new HorizontalTabFactory();
        tabFactory.initComposite(parent);
        tabFactory.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                TalendPropertyTabDescriptor descriptor = (TalendPropertyTabDescriptor) selection.getFirstElement();

                if (descriptor == null) {
                    return;
                }

                if (currentSelectedTab != null) {
                    if ((!currentSelectedTab.getData().equals(descriptor.getData()) || currentSelectedTab.getCategory() != descriptor
                            .getCategory())) {
                        for (Control curControl : tabFactory.getTabComposite().getChildren()) {
                            curControl.dispose();
                        }
                    }
                }

                if (element == null || !element.equals(descriptor.getData()) || currentSelectedTab == null
                        || currentSelectedTab.getCategory() != descriptor.getCategory() || selectedPrimary) {
                    Object data = descriptor.getData();
                    if (data instanceof Element) {
                        element = (Element) data;

                        currentSelectedTab = descriptor;
                        IDynamicProperty propertyComposite = createTabComposite(tabFactory.getTabComposite(), element, descriptor
                                .getCategory());

                    } else if (data instanceof IRepositoryObject && currentSelectedTab != descriptor) {

                        currentSelectedTab = descriptor;
                        IDynamicProperty propertyComposite = createTabComposite(tabFactory.getTabComposite(), data, descriptor
                                .getCategory());

                    }
                    selectedPrimary = false;
                }
            }
        });

    }

    private IDynamicProperty createTabComposite(Composite parent, Object data, EComponentCategory category) {
        final int style = SWT.H_SCROLL | SWT.V_SCROLL | SWT.NO_FOCUS;
        IDynamicProperty dynamicComposite = null;

        if (EComponentCategory.EXTRA.equals(category)) {
            dynamicComposite = new ExtraComposite(parent, style, category, (Element) data, true);

        } else if (EComponentCategory.STATSANDLOGS.equals(category)) {
            dynamicComposite = new StatsAndLogsComposite(parent, style, category, (Element) data);

        } else if (EComponentCategory.CONTEXT.equals(category)) {
            // TODO
            // dynamicComposite = new ContextDynamicComposite(parent, style, category, element);

        } else if (EComponentCategory.MAIN.equals(category)) {
            dynamicComposite = new MainComposite(parent, SWT.NONE, tabFactory.getWidgetFactory(), (IRepositoryObject) data);
        } else if (EComponentCategory.VERSIONS.equals(category)) {
            dynamicComposite = new ProcessVersionComposite(parent, SWT.NONE, tabFactory.getWidgetFactory(), (IRepositoryObject) data);
        }
        if (dynamicComposite != null) {
            dynamicComposite.refresh();
        }
        currentSelectedTab.setPropertyComposite(dynamicComposite);
        return dynamicComposite;
    }

    /**
     * 
     * DOC ggu Comment method "setElement".
     * 
     * @param obj
     */

    private void setElement(Object obj, final String title, Image image) {
        EComponentCategory[] categories = null;

        if (obj != null && obj instanceof Process) {
            process = (Process) obj;
            if (currentSelectedTab != null && currentSelectedTab.getData().equals(process) && !cleaned) {
                return;
            }

            categories = getCategories(process);
        } else if (obj != null && obj instanceof IRepositoryObject) {
            categories = getCategories(obj);
        } else {
            cleanDisplay();
            return;
        }

        final List<TalendPropertyTabDescriptor> descriptors = new ArrayList<TalendPropertyTabDescriptor>();
        for (EComponentCategory category : categories) {
            TalendPropertyTabDescriptor d = new TalendPropertyTabDescriptor(category);
            d.setData(obj);
            descriptors.add(d);
        }

        tabFactory.setInput(descriptors);
        setPartName(title, image);
        cleaned = false;
        tabFactory.setSelection(new IStructuredSelection() {

            public Object getFirstElement() {
                return null;
            }

            public Iterator iterator() {
                return null;
            }

            public int size() {
                return 0;
            }

            public Object[] toArray() {
                return null;
            }

            public List toList() {
                List<TalendPropertyTabDescriptor> d = new ArrayList<TalendPropertyTabDescriptor>();

                if (descriptors.size() > 0) {
                    if (currentSelectedTab != null) {
                        for (TalendPropertyTabDescriptor ds : descriptors) {
                            if (ds.getCategory() == currentSelectedTab.getCategory()) {
                                d.add(ds);
                                return d;
                            }
                        }
                    }
                    d.add(descriptors.get(0));
                }
                return d;
            }

            public boolean isEmpty() {
                return false;
            }

        });

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.part.ViewPart#setPartName(java.lang.String)
     */
    @Override
    protected void setPartName(String partName) {
        setPartName(partName, null);
    }

    /**
     * 
     * DOC ggu Comment method "setPartName".
     * 
     * set title
     */
    public void setPartName(String typeTitle, Image icon) {
        String title = null;
        String type = null;

        if (typeTitle != null && typeTitle.contains(SEPARATOR)) {
            String[] tt = typeTitle.split(SEPARATOR);
            type = tt[0];
            title = tt[1];
        } else {
            title = typeTitle;
        }

        String viewName = VIEW_NAME;
        if (element instanceof IProcess && AbstractProcessProvider.isExtensionProcessForJoblet((IProcess) element)) {
            viewName = VIEW_NAME_JOBLET;
        }

        if (type != null) {
            viewName = type;
        }

        if (title == null) {
            title = ""; //$NON-NLS-1$
        }
        if (!title.equals("")) { //$NON-NLS-1$
            viewName = viewName + "(" + title + ")"; //$NON-NLS-1$ //$NON-NLS-2$            
            super.setTitleToolTip(title);
        }
        if (tabFactory != null && icon == null) {
            Image image = ImageProvider.getImage(ECoreImage.PROCESS_ICON);
            if (this.element != null && this.element instanceof IProcess) {
                if (((IProcess) this.element).disableRunJobView()) { // ?? joblet
                    image = ImageProvider.getImage(ECoreImage.JOBLET_ICON);
                }
            }
            tabFactory.setTitle(title, image);
        } else {
            tabFactory.setTitle(title, icon);
        }

        super.setPartName(viewName);
    }

    /**
     * set the category.
     */
    private EComponentCategory[] getCategories(Object obj) {
        List<EComponentCategory> category = new ArrayList<EComponentCategory>();
        if (obj instanceof Process) {
            Process process = (Process) obj;
            category.add(EComponentCategory.MAIN);
            category.add(EComponentCategory.EXTRA);
            boolean isJoblet = AbstractProcessProvider.isExtensionProcessForJoblet(process);
            if (!isJoblet) {
                category.add(EComponentCategory.STATSANDLOGS);
            }
            category.add(EComponentCategory.VERSIONS);
            // category.add(EComponentCategory.CONTEXT);

        } else if (obj instanceof IRepositoryObject) {
            category.add(EComponentCategory.MAIN);
            category.add(EComponentCategory.VERSIONS);
        }
        return category.toArray(new EComponentCategory[0]);
    }

    public Process getElement() {
        return (Process) element;
    }

    public boolean isCleaned() {
        return this.cleaned;
    }

    public void cleanDisplay() {
        setPartName(null);
        tabFactory.setInput(null);
        tabFactory.setTitle(null, null);
        if (tabFactory.getTabComposite() != null) {
            for (Control curControl : tabFactory.getTabComposite().getChildren()) {
                curControl.dispose();
            }
        }
        this.currentSelectedTab = null;
        this.element = null;
        this.cleaned = true;
        this.selectedPrimary = true;
        process = null;
    }

    public void refresh() {
        refresh(false, null);
    }

    public void refresh(boolean force, Object obj) {
        if (force) {
            cleanDisplay();
        }

        if (obj == null) {
            final IEditorPart activeEditor = getSite().getPage().getActiveEditor();
            if (activeEditor != null && activeEditor instanceof AbstractMultiPageTalendEditor) {
                AbstractTalendEditor talendEditor = ((AbstractMultiPageTalendEditor) activeEditor).getTalendEditor();
                IProcess process = talendEditor.getProcess();
                if (process != null && process instanceof Element) {
                    this.selectedPrimary = true;
                    this.cleaned = force;
                    this.element = (Element) process;

                    // remove "Job" or "Joblet" from title
                    String title = activeEditor.getTitle();
                    if (title.startsWith(VIEW_NAME_JOBLET)) {
                        title = title.substring(VIEW_NAME_JOBLET.length() + 1);
                    } else if (title.startsWith(VIEW_NAME)) {
                        title = title.substring(VIEW_NAME.length() + 1);

                    }

                    setElement(element, title, null);
                    return;
                }
            }
        } else {
            if (obj instanceof IRepositoryObject) {
                IRepositoryObject repositoryObject = (IRepositoryObject) obj;

            }
        }

        cleanDisplay();

    }

    @Override
    public void setFocus() {
        if (selectedPrimary) {
            if (getViewSite() != null) {
                getViewSite().getShell().setFocus();
            }
        } else {
            if (tabFactory.getTabComposite() != null) {
                tabFactory.getTabComposite().setFocus();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        CorePlugin.getDefault().getRepositoryService().removeRepositoryTreeViewListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent
     * )
     */
    public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = event.getSelection();
        if (selection instanceof StructuredSelection) {
            Object input = ((IStructuredSelection) selection).getFirstElement();

            if (!(input instanceof RepositoryNode)) {
                if (input instanceof IAdaptable) {
                    // see ProcessPart.getAdapter()
                    IAdaptable adaptable = (IAdaptable) input;
                    input = adaptable.getAdapter(RepositoryNode.class);
                }
            }

            if (input instanceof RepositoryNode) {
                RepositoryNode repositoryNode = (RepositoryNode) input;
                Object obj = repositoryNode.getProperties(EProperties.CONTENT_TYPE);

                String type = null;
                if (obj != null) {
                    type = obj.toString();
                    if (obj instanceof ERepositoryObjectType) {
                        switch ((ERepositoryObjectType) obj) {
                        case PROCESS:
                            type = VIEW_NAME;
                            break;
                        case JOBLET:
                            type = VIEW_NAME_JOBLET;
                            break;
                        }
                    }

                } else {
                    return;
                }

                IRepositoryObject repositoryObject = repositoryNode.getObject();
                if (repositoryObject == null) {
                    repositoryObject = new EmptyRepositoryObject();
                    return;
                }
                String title = repositoryObject.getLabel() + " " + repositoryObject.getVersion();

                setElement(repositoryObject, type + SEPARATOR + title, ImageProvider.getImage(repositoryNode.getIcon()));
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.ui.views.properties.IJobSettingsView#getSelection()
     */
    public ISelection getSelection() {
        IDynamicProperty dc = currentSelectedTab.getPropertyComposite();
        if (dc instanceof ProcessVersionComposite) {
            return ((ProcessVersionComposite) dc).getSelection();
        }
        return null;
    }
}
