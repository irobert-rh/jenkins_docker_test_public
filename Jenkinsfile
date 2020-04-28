node {
    def app

    stage('Clone repository') {
        /* Let's make sure we have the repository cloned to our workspace */

        checkout scm
    }

    stage('Build image') {
        /* This builds the actual image; synonymous to
         * docker build on the command line */

        app = docker.build("irobert0126/imagescantest")
    }

    stage('Test image') {
        /* Ideally, we would run a test framework against our image.
         * For this example, we're using a Volkswagen-type approach ;-) */

        app.inside {
            sh 'echo "Tests passed"'
        }
    }
    
    stage('Container Security Scan') {
        // Anchore Image Scanner
        /*
        sh 'echo "docker.io/irobert0126/imagescantest `pwd`/Dockerfile" > anchore_images'
        anchore name: 'anchore_images'
        */
        
        // Trivy Image Scanner
        // sh 'docker run --rm --net=bridge aquasec/trivy client --remote http://172.17.0.2:4954 irobert0126/imagescantest'
        aqua locationType: 'hosted', apiURL: 'http://172.17.0.2:4954', customFlags: '', hideBase: false, hostedImage: '', localImage: 'irobert0126/imagescantest', notCompliesCmd: '', onDisallowed: 'ignore', policies: '', register: false, registry: '', showNegligible: false
    }
    
    stage('Push image') {
        /* Finally, we'll push the image with two tags:
         * First, the incremental build number from Jenkins
         * Second, the 'latest' tag.
         * Pushing multiple tags is cheap, as all the layers are reused. */
        docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
            app.push("${env.BUILD_NUMBER}")
            app.push("latest")
        }
    }
}
