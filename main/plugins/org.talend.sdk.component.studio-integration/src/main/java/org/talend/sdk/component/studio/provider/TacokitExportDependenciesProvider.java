/*
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */
package org.talend.sdk.component.studio.provider;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.core.model.components.ComponentCategory;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.runtime.repository.build.BuildExportManager;
import org.talend.core.runtime.repository.build.BuildExportManager.EXPORT_TYPE;
import org.talend.core.runtime.repository.build.IBuildExportDependenciesProvider;
import org.talend.core.ui.component.ComponentsFactoryProvider;
import org.talend.designer.core.model.utils.emf.talendfile.NodeType;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;
import org.talend.designer.core.utils.UnifiedComponentUtil;
import org.talend.repository.documentation.ExportFileResource;
import org.talend.sdk.component.studio.ComponentModel;

public class TacokitExportDependenciesProvider implements IBuildExportDependenciesProvider {

    private final static Logger LOGGER = LoggerFactory.getLogger(TacokitExportDependenciesProvider.class.getName());

    public static final String MAVEN_INF = "MAVEN-INF";

    public static final String TALEND_INF = "TALEND-INF";

    /**
     * called when exporting a job, this is up to the implementor to check that the item should export the dependencies
     *
     * @see org.talend.core.runtime.repository.build.IBuildExportDependenciesProvider
     */
    @Override
    public void exportDependencies(final ExportFileResource exportFileResource, final Item item) {
        if (!BuildExportManager.getInstance().getCurrentExportType().equals(EXPORT_TYPE.OSGI)) {
            return;
        }
        final Map<String, String> tckPlugins = new HashMap<String, String>();
        final EList<?> nodes = ((ProcessType) ((ProcessItem) item).getProcess()).getNode();
        final String DI_COMP = ComponentCategory.CATEGORY_4_DI.getName();
        nodes.stream()
                .map(node -> {
                    final String componentName = ((NodeType) node).getComponentName();
                    IComponent component = ComponentsFactoryProvider.getInstance().get(componentName, DI_COMP);
                    if (component == null) {
                        component = UnifiedComponentUtil.getDelegateComponent(componentName, DI_COMP);
                    }
                    return component;
                })
                .filter(Objects::nonNull)
                .filter(ComponentModel.class::isInstance)
                .map(ComponentModel.class::cast)
                .forEach(comp -> {
                    tckPlugins.put(comp.getId().getPlugin(), comp.getId().getPluginLocation());
                });
        if (tckPlugins.isEmpty()) {
            return;
        }
        try {
            // TODO get resources path
            final String output = exportFileResource.getDirectoryName();
            //
            final Path m2 = findM2Path();
            final Path resMvnRepo = Paths.get(output, MAVEN_INF, "repository");
            final Path pluginsProps = Paths.get(output, TALEND_INF, "plugins.properties");
            final StringBuffer pluginsFileContent = new StringBuffer("");
            Files.createDirectories(resMvnRepo);
            if (Files.exists(pluginsProps)) {
                Files.readAllLines(pluginsProps).stream().map(line ->
                        line.split("="))
                        .collect(Collectors.toList());
            } else {
                Files.createDirectories(pluginsProps.getParent());
                pluginsFileContent.append("# component-runtime components coordinates:\n");
            }
            tckPlugins.forEach((plugin, location) -> {
                final Path src = resolveGavToJar(m2, l);
                final Path dst = resolveGavToJar(resMvnRepo, l);
                try {
                    if (!Files.exists(dst)) {
                    }
                    Files.createDirectories(dst.getParent());
                    Files.copy(src, dst);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            Files.copy(new BufferedInputStream(new ByteArrayInputStream(pluginsFileContent.toString()
                    .getBytes())), pluginsProps);
            System.setProperty("talend.component.manager.components.present", "true");
        } catch (Exception e) {
            LOGGER.error("[exportDependencies] Error occurred:", e);
        }
    }

    public static Path resolveGavToJar(Path m2, String gav) {
        final String[] segments = gav.split(":");
        final String jar = String.format("%s-%s.jar", segments[1], segments[2]);
        return Paths.get(m2.toString(), segments[0].replaceAll("\\.", "/"), segments[1], segments[2], jar);
    }

    public static Path findM2Path() {
        return Optional.ofNullable(System.getProperty("talend.component.manager.m2.repository"))
                .map(Paths::get)
                .orElseGet(() -> {
                    // check if we are in the studio process if so just grab the the studio config
                    final String m2Repo = System.getProperty("maven.repository");
                    if (!"global".equals(m2Repo)) {
                        final String m2StudioRepo = System.getProperty("osgi.configuration.area", "")
                                .replaceAll("^file:", "");
                        final Path localM2 = Paths.get(m2StudioRepo, ".m2/repository");
                        if (Files.exists(localM2)) {
                            return localM2;
                        }
                    }
                    // defaults to user m2
                    return Paths.get(System.getProperty("user.home", "")).resolve(".m2/repository");
                });
    }

}
