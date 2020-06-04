def call(app, build_airflow = false, build_job_name = "image-builder", dockerfile_path = "./kubernetes/Dockerfile", context_path = ".", add_latest_tag = false, affected_files_path = null) {
    pipeline {
        agent any
        environment {
            APP = "${app}"
            BUILDCOMMIT = "${params.commit}"
            DOCKERFILE_PATH = "${dockerfile_path}"
            CONTEXT_PATH = "${context_path}"
            ADD_LATEST_TAG = "${add_latest_tag}"
        }
        
        stages {
            
            stage("Trigger") {
                when {
                    expression { build_job_name != currentBuild.projectName }
                }
                steps {
                    catchError {
                        cleanWs()
                        checkout scm
                        script {
                            def commits = []
                            if (affected_files_path == null) {
                                // if affected_files_path is not set explicitely, it should default to context_path
                                affected_files_path = context_path
                            }
                            currentBuild.changeSets.each { changeSet ->
                                changeSet.items.each { item ->
                                    def shouldFilterCommits = affected_files_path != "."
                                    if (shouldFilterCommits) {
                                        for (file in item.affectedFiles) {
                                            if (file.path.startsWith(affected_files_path)) {
                                                commits << item.commitId
                                                break
                                            }
                                        }
                                    } else {
                                        commits << item.commitId
                                    }
                                }
                            }
                            if (commits.size() == 0) {
                                echo "Got no changes, assuming the currently checked out commit should be built."
                                commits = [checkout(scm).GIT_COMMIT]
                            }
                            commits.each { commit ->
                                echo "${commit}"
                                build job: build_job_name, parameters: [[$class: 'StringParameterValue', name: 'commit', value: commit]], propogate: false, wait: false
                            }
                        }
                    }
                }
            }
            
            stage("Build") {
                steps {
                    catchError {
                            checkout scm
                            script {
                                aqua([locationType: 'local', localImage: 'nodejscn/node'])
                                sh '''#!/bin/bash
                                    set -e
                                    ls -l
                                    echo ${BUILDCOMMIT}
                                    echo "building ${APP}"
                                    docker build -t ${APP}:${BUILDCOMMIT} .
                                '''
                        }
                    }
                }
            }
        }
    }
}
