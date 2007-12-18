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
package org.talend.componentdesigner.ui.dialog;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IInputValidator;
import org.talend.componentdesigner.PluginConstant;

/**
 * @author rli
 *
 */
public class CopyComponentValidator implements IInputValidator {
	public CopyComponentValidator() {

	}

	public String isValid(String componentName) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(
				PluginConstant.PROJECTNAME_DEFAULT);
		IFolder componentFolder = project.getFolder(componentName);
		if (componentFolder.exists()) {
			return "The component has been exsit";
		}
		return null;
	}
}
