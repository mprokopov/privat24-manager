pipeline {
    agent any

    stages{
        stage('Build jars') {
            steps{
                sh '/var/lib/jenkins/lein pom'
                sh '/var/lib/jenkins/lein uberjar'
            }
        }
        stage('Build docker') {
            steps {
                script {
                    def VERSION = readMavenPom(file: 'pom.xml').getVersion()

                    docker.withRegistry("https://663084659937.dkr.ecr.eu-central-1.amazonaws.com", "ecr:eu-central-1:manager-credentials") {
                        def customImage = docker.build("privat-manager:${VERSION}","--build-arg VERSION=${VERSION} .")
                        customImage.push()
                    }
                }
            }
        }
    }
}
