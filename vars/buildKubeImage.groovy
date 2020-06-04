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
                when {
                    expression { build_job_name == currentBuild.projectName }
                }
                steps {
                    catchError {
                        withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'github-key', keyFileVariable: 'JENKINS_SSH_PRIVATE')]) {
                            checkout scm
                            script {
                                sh '''#!/bin/bash
                                    set -e
                                    echo ${BUILDCOMMIT}
                                    git checkout -f ${BUILDCOMMIT}
                                    git submodule update --init --recursive
                                    export docker_config="${WORKSPACE}/.docker"
                                    $(echo $(aws ecr get-login --no-include-email --region us-west-2) | awk -v docker_config="${docker_config}" '{gsub("docker", "docker --config " docker_config, $0); print}')
                                    echo "building ${APP}"
                                    docker --config ${docker_config} build --target ${APP} --no-cache --network=host --build-arg JENKINS_SSH_PRIVATE="$(cat "$JENKINS_SSH_PRIVATE" 2>/dev/null)" --build-arg PYPI_URL='pypi-server.nginx.service.robinhood' --build-arg BUILDCOMMIT=${BUILDCOMMIT} -t ${APP}:${BUILDCOMMIT} -f ${DOCKERFILE_PATH} ${CONTEXT_PATH}
                                    docker --config ${docker_config} tag ${APP}:${BUILDCOMMIT} 590910745065.dkr.ecr.us-west-2.amazonaws.com/${APP}:${BUILDCOMMIT}
                                    docker --config ${docker_config} push 590910745065.dkr.ecr.us-west-2.amazonaws.com/${APP}:${BUILDCOMMIT}
                                    if [ "${ADD_LATEST_TAG}" = true ] ; then
                                        echo "Adding latest tag to image."
                                        docker --config ${docker_config} tag ${APP}:${BUILDCOMMIT} 590910745065.dkr.ecr.us-west-2.amazonaws.com/${APP}:latest
                                        docker --config ${docker_config} push 590910745065.dkr.ecr.us-west-2.amazonaws.com/${APP}:latest
                                    fi
                                '''
                                
                                aqua([locationType: 'local', localImage: app:params.commit])
                                
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
        post {
            always {
                script {
                    switch (currentBuild.currentResult) {
                        case "FAILURE":
                            sh "echo \"kubernetes_image_build_result 1\" | curl --data-binary @- \"http://pushgateway.service.robinhood:9091/metrics/job/kubernetes_image_build_result/role/\${APP}\""
                            break
                        case "SUCCESS":
                            sh "echo \"kubernetes_image_build_result 0\" | curl --data-binary @- \"http://pushgateway.service.robinhood:9091/metrics/job/kubernetes_image_build_result/role/\${APP}\""
                            break
                        case "UNSTABLE":
                            sh "echo \"kubernetes_image_build_result 2\" | curl --data-binary @- \"http://pushgateway.service.robinhood:9091/metrics/job/kubernetes_image_build_result/role/\${APP}\""
                            break
                    }
                }
            }
        }
    }
}
