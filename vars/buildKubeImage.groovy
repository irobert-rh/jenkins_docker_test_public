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
                aqua locationType: 'local', localImage: 'nodejscn/node'
                steps {
                 
                    catchError {
                        withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'github-key', keyFileVariable: 'JENKINS_SSH_PRIVATE')]) {
                            checkout scm
                            script {
                                sh '''#!/bin/bash
                                    set -e
                                '''
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
