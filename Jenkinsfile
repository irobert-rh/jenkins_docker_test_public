node {
    def app

    stage('Clone repository') {
        /* Let's make sure we have the repository cloned to our workspace */

        checkout scm
    }

    stage('Build image') {
        /* This builds the actual image; synonymous to
         * docker build on the command line */
        // sh 'docker run -v /var/run/docker.sock:/var/run/docker.sock -v /tmp/.cache/:/root/.cache/ aquasec/trivy -f json golang:stretch'
        //app = docker.build("irobert0126/imagescantest")
    }

    stage('Test image') {
        /* Ideally, we would run a test framework against our image.
         * For this example, we're using a Volkswagen-type approach ;-) */

        /*app.inside {
            sh 'echo "Tests passed"'
        }*/
    }
    
    stage('Trivy Plugin Scan') {
        aqua locationType: 'local', localImage: 'python:rc-alpine', caCertificates: false, customFlags: '', hideBase: false, hostedImage: '', notCompliesCmd: '', onDisallowed: 'ignore', policies: '', register: false, registry: '', showNegligible: false
    }
    
    stage('Container Security Scan') {
        // Anchore Image Scanner
        /*
        sh 'echo "docker.io/irobert0126/imagescantest `pwd`/Dockerfile" > anchore_images'
        anchore name: 'anchore_images'
        */
        
        // Trivy Image Scanner
        // sh 'docker run --rm --net=bridge aquasec/trivy client --remote http://172.17.0.3:4954 irobert0126/imagescantest'
        // sh 'docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v /tmp/.cache/:/root/.cache/ aquasec/trivy irobert0126/imagescantest'
    }
        
    stage('Push image') {
        /* Finally, we'll push the image with two tags:
         * First, the incremental build number from Jenkins
         * Second, the 'latest' tag.
         * Pushing multiple tags is cheap, as all the layers are reused. */
        
        /*
        docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
            app.push("${env.BUILD_NUMBER}")
            app.push("latest")
        }
        */
    }
}
