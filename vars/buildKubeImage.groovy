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
                        
            stage("Build") {
                steps {
                    catchError {
                            checkout scm
                            script {
                                sh '''#!/bin/bash
                                    set -e
                                    ls -l
                                    echo ${BUILDCOMMIT}
                                    echo "building ${APP}"
                                    docker build -t ${APP}:${APP} .
                                '''
                                aqua([locationType: 'local', localImage: "${app}:${app}"])
                        }
                    }
                }
            }
        }
    }
}
