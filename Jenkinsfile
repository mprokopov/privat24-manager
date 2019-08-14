pipeline {
    agent any

    stages{
        stage('Build jars') {
            steps{
                sh '/var/lib/jenkins/lein uberjar'
            }
        }
        stage('Build docker') {
            steps {
                script {
                    VERSION = readFile('VERSION')
                    docker.withRegistry("https://663084659937.dkr.ecr.eu-central-1.amazonaws.com", "ecr:eu-central-1:manager-credentials") {
                        def customImage = docker.build("privat-manager:${VERSION}")
                        customImage.push()
                    }
                }
            }
        }
    }
}
