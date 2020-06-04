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
                        withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'github-key', keyFileVariable: 'JENKINS_SSH_PRIVATE')]) {
                            checkout scm
                            script {
                                sh '''#!/bin/bash
                                    set -e
                                    ls -l
                                    echo ${BUILDCOMMIT}
                                '''
                                
                                aqua([locationType: 'local', localImage: 'app:params.commit'])
                                
                                if(build_airflow) {
                                    sh '''#!/bin/bash
                                    export docker_config="${WORKSPACE}/.docker"
                                    echo "building ${APP}-worker"
                                    docker --config ${docker_config} build --target ${APP}-worker --network=host --build-arg JENKINS_SSH_PRIVATE="$(cat "$JENKINS_SSH_PRIVATE" 2>/dev/null)" --build-arg PYPI_URL='pypi-server.nginx.service.robinhood' --build-arg BUILDCOMMIT=${BUILDCOMMIT} -t ${APP}-worker:${BUILDCOMMIT} -f ${DOCKERFILE_PATH} ${CONTEXT_PATH}
                                    docker --config ${docker_config} tag ${APP}-worker:${BUILDCOMMIT} 590910745065.dkr.ecr.us-west-2.amazonaws.com/${APP}-worker:${BUILDCOMMIT}
                                    docker --config ${docker_config} push 590910745065.dkr.ecr.us-west-2.amazonaws.com/${APP}-worker:${BUILDCOMMIT}
                                    '''
                                }

                            }
                        }
                    }
                }
            }
        }
    }
}
