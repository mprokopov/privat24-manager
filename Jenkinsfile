#!/usr/bin/env groovy
def registry = "663084659937.dkr.ecr.eu-central-1.amazonaws.com"
def repo = "${registry}/privat-manager"
def tag = "latest"
def image = "${repo}:${tag}"
def version = "1.0"
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
          version = readMavenPom(file: 'pom.xml').getVersion()
          tag = version
          image = "${repo}:${tag}"
          currentBuild.displayName = "${ version }+${ BUILD_ID }"

          docker.withRegistry("https://663084659937.dkr.ecr.eu-central-1.amazonaws.com", "ecr:eu-central-1:jenkins-aws-ecr") {
            docker.build(image).push()
          }
        }
      }
      post {
        success {
          writeFile(file: 'version.txt', text: tag)
          writeFile(file: 'image.txt', text: image)
          archiveArtifacts('image.txt')
          archiveArtifacts('version.txt')
        }
      }
    }
  }
  post {
    always {
      cleanWs()
    }
  }
}
