def gv

pipeline {
  agent any

  tools {
    maven 'Maven'
  }

  parameters {
    booleanParam(name: "IS_CLEANWORKSPACE", defaultValue: "true", description: "Set to false to disable folder cleanup, default true.")
    booleanParam(name: "IS_DEPLOYING", defaultValue: "true", description: "Set to false to skip deployment, default true.")
    booleanParam(name: "IS_TESTING", defaultValue: "true", description: "Set to false to skip testing, default true!")
  }

  environment {
    AWS_ACCOUNT_ID = credentials("AWS-ACCOUNT-ID")
    DOCKER_IMAGE = "bank-microservice"
    ECR_REGION = "us-east-2"
  }

  stages {
    stage("init") {
      steps {
          script {
          gv = load "script.groovy"
        }
      }
    }
    stage("Build") {
      steps {
        script {
          gv.buildApp()
        }
      }
    }
    stage("Test") {
      steps {
        script {
          gv.testApp()
        }
      } 
    }    
    stage("SonarQube") {
      steps {
        withSonarQubeEnv("us-west-1-sonar") {
            sh "mvn sonar:sonar"
        }
      }
    }
    stage("Await Quality Gate") {
      steps {
          waitForQualityGate abortPipeline: true
      }
    }
    stage("Upstream to ECR") {
      steps {
        script {
          gv.upstreamToECR()
        }
      }
    }
  }
  post {
    cleanup {
      script {
          gv.postCleanup()
        }
    }
  }
}