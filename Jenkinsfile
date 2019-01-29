pipeline {
    agent any

    stages{
        stage('Build jars') {
            steps{
                sh 'lein uberjar'
            }
        }
        stage('Build docker') {
            steps {
                withDockerRegistry(credentialsId: 'a85c1d64-dcc5-4253-9092-c11eb058aa45', url: 'https://registry.it-expert.com.ua') {
                    def customImage = docker.build('registry.it-expert.com.ua/nexus/privat-manager:1.2-beta1')
                    customImage.push()
                }
            }
        }
    }
}
