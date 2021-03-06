/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.plugins.tomcat

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.plugins.WarPluginConvention

/**
 * <p>A {@link Plugin} which extends the {@link WarPlugin} to add tasks which run the web application using an embedded
 * Tomcat web container.</p>
 *
 * @author Benjamin Muschko
 */
class TomcatPlugin implements Plugin<Project> {
    static final String TOMCAT_RUN_TASK_NAME = 'tomcatRun'
    static final String TOMCAT_RUN_WAR_TASK_NAME = 'tomcatRunWar'
    static final String TOMCAT_STOP_TASK_NAME = 'tomcatStop'
    static final String CLASSPATH = 'classpath'
    static final String HTTP_PORT_CONVENTION = 'httpPort'
    static final String STOP_PORT_CONVENTION = 'stopPort'
    static final String STOP_KEY_CONVENTION = 'stopKey'
    static final String TOMCAT_CONFIGURATION_NAME = 'tomcat'

    @Override
    void apply(Project project) {
        project.plugins.apply(WarPlugin.class)

        project.configurations.add(TOMCAT_CONFIGURATION_NAME).setVisible(false).setTransitive(true)
               .setDescription('The Tomcat libraries to be used for this project.')

        TomcatPluginConvention tomcatConvention = new TomcatPluginConvention()
        project.convention.plugins.tomcat = tomcatConvention

        configureMappingRules(project, tomcatConvention)
        configureTomcatRun(project)
        configureTomcatRunWar(project)
        configureTomcatStop(project, tomcatConvention)
    }

    private void configureMappingRules(final Project project, final TomcatPluginConvention tomcatConvention) {
        project.tasks.withType(AbstractTomcatRunTask.class).whenTaskAdded { AbstractTomcatRunTask abstractTomcatRunTask ->
            configureAbstractTomcatTask(project, tomcatConvention, abstractTomcatRunTask)        
        }
    }

    private void configureAbstractTomcatTask(final Project project, final TomcatPluginConvention tomcatConvention, AbstractTomcatRunTask tomcatTask) {
        tomcatTask.daemon = false
        tomcatTask.reloadable = true
        tomcatTask.conventionMapping.map('buildscriptClasspath') { project.buildscript.configurations.getByName(CLASSPATH).asFileTree }
        tomcatTask.conventionMapping.map('tomcatClasspath') { project.configurations.getByName(TOMCAT_CONFIGURATION_NAME).asFileTree }
        tomcatTask.conventionMapping.map('contextPath') { project.tasks.getByName(WarPlugin.WAR_TASK_NAME).baseName }
        tomcatTask.conventionMapping.map(HTTP_PORT_CONVENTION) { tomcatConvention.httpPort }
        tomcatTask.conventionMapping.map(STOP_PORT_CONVENTION) { tomcatConvention.stopPort }
        tomcatTask.conventionMapping.map(STOP_KEY_CONVENTION) { tomcatConvention.stopKey }
    }

    private void configureTomcatRun(final Project project) {
        project.tasks.withType(TomcatRun.class).whenTaskAdded { TomcatRun tomcatRun ->
            tomcatRun.conventionMapping.map(CLASSPATH) { project.tasks.getByName(WarPlugin.WAR_TASK_NAME).classpath }
            tomcatRun.conventionMapping.map('webAppSourceDirectory') { getWarConvention(project).webAppDir }
        }

        TomcatRun tomcatRun = project.tasks.add(TOMCAT_RUN_TASK_NAME, TomcatRun.class)
        tomcatRun.description = 'Uses your files as and where they are and deploys them to Tomcat.'
        tomcatRun.group = WarPlugin.WEB_APP_GROUP
    }

    private void configureTomcatRunWar(final Project project) {
        project.tasks.withType(TomcatRunWar.class).whenTaskAdded { TomcatRunWar tomcatRunWar ->
            tomcatRunWar.dependsOn(WarPlugin.WAR_TASK_NAME)
            tomcatRunWar.conventionMapping.map('webApp') { project.tasks.getByName(WarPlugin.WAR_TASK_NAME).archivePath }
        }

        TomcatRunWar tomcatRunWar = project.tasks.add(TOMCAT_RUN_WAR_TASK_NAME, TomcatRunWar.class)
        tomcatRunWar.description = 'Assembles the webapp into a war and deploys it to Tomcat.'
        tomcatRunWar.group = WarPlugin.WEB_APP_GROUP
    }

    private void configureTomcatStop(final Project project, final TomcatPluginConvention tomcatConvention) {
        TomcatStop tomcatStop = project.tasks.add(TOMCAT_STOP_TASK_NAME, TomcatStop.class)
        tomcatStop.description = 'Stops Tomcat.'
        tomcatStop.group = WarPlugin.WEB_APP_GROUP
        tomcatStop.conventionMapping.map(STOP_PORT_CONVENTION) { tomcatConvention.stopPort }
        tomcatStop.conventionMapping.map(STOP_KEY_CONVENTION) { tomcatConvention.stopKey }
    }

    WarPluginConvention getWarConvention(Project project) {
        project.convention.getPlugin(WarPluginConvention.class)
    }
}