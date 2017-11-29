/**
 * (C) Copyright IBM Corporation 2014, 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.wasdev.wlp.gradle.plugins.tasks

import org.gradle.api.tasks.TaskAction

import static net.wasdev.wlp.gradle.plugins.Liberty.LIBERTY_DEPLOY_CONFIGURATION

class DeployTask extends AbstractServerTask {

    @TaskAction
    void deploy() {

        def deployClosureDeclared = false

        project.ant.taskdef(name: 'deploy',
                                classname: net.wasdev.wlp.ant.DeployTask.name,
                                classpath: project.rootProject.buildscript.configurations.classpath.asPath)

        // deploys the list of deploy closures
        server.deploy.listOfClosures.add(project.liberty.deploy)
        for (Object deployable :  server.deploy.listOfClosures) {
            def params = buildLibertyMap(project)
            def fileToDeploy = deployable.file
            if (fileToDeploy != null) {
                deployClosureDeclared = true
                params.put('file', fileToDeploy)
                project.ant.deploy(params)
            } else {
                def deployDir = deployable.dir
                def include = deployable.include
                def exclude = deployable.exclude

                if (deployDir != null) {
                    deployClosureDeclared = true
                    project.ant.deploy(params) {
                        fileset(dir:deployDir, includes: include, excludes: exclude)
                    }
                }
            }
        }

        deployConfigurationBased()

        // Deploys war or ear from current project
        if (!deployClosureDeclared) {
            if (project.plugins.hasPlugin("war")) {
                def params = buildLibertyMap(project)
                def warFile = project.war.archivePath
                if (warFile.exists()) {
                    params.put('file', warFile)
                    project.ant.deploy(params)
                }
            }

            if (project.plugins.hasPlugin("ear")) {
                def params = buildLibertyMap(project)
                def earFile = project.ear.archivePath
                if (earFile.exists()) {
                    params.put('file', earFile)
                    project.ant.deploy(params)
                }
            }
        }
    }

    private void deployConfigurationBased() {
        // Deploys from the subproject configuration
        def deployConf = project.configurations.findByName(LIBERTY_DEPLOY_CONFIGURATION)
        def deployArtifacts = deployConf.incoming.resolutionResult.allDependencies as List
        def artifacts = deployConf.resolvedConfiguration.resolvedArtifacts as List
        artifacts.each {
            def params = buildLibertyMap(project)
            params.put('file', it.file.absolutePath)
            project.ant.deploy(params)
        }
    }
}
