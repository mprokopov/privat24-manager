pipeline {
    agent any

    stages{
        stage('Build jars') {
            steps{
                sh 'lein uberjar'
            }
        }
    }
}
