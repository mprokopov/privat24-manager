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
                    withCredentials(credentialsId: 'manager-credentials') {
                        sh '(aws ecr get-login –registry-ids 663084659937  --region eu-central-1)'
                    }

                    VERSION = readFile('VERSION')
                    def customImage = docker.build("663084659937.dkr.ecr.eu-central-1.amazonaws.com/privat-manager:${VERSION}")
                    customImage.push()
                }
            }
        }
    }
}
