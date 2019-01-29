pipeline {
    agent any

    stages{
        stage('Build jars') {
            steps{
                sh '/root/lein ubejar'
            }
        }
    }
}
