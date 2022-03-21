import groovy.json.JsonSlurper

def data = ""
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
    AWS_ACCOUNT_ID = credentials("AWS_ACCOUNT_ID")
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
            sh "mvn verify sonar:sonar"
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
    stage("Get Secrets"){
      steps {
        sh "aws s3 cp s3://beb-bucket-jd/cluster/aline/docker-compose.yaml . --profile joshua"
        sh """aws secretsmanager  get-secret-value --secret-id prod/services --region us-east-2 --profile joshua | jq -r '.["SecretString"]' | jq '.' > secrets"""
      }
    }
    stage("Create Deployment Environment"){
      steps {
        script {
          secretKeys = """${sh(script: 'cat secrets | jq "keys"', returnStdout: true).trim()}"""
          secretValues = """${sh(script: 'cat secrets | jq "values"', returnStdout: true).trim()}"""
          def parser = new JsonSlurper()
          def keys = parser.parseText(secretKeys)
          def values = parser.parseText(secretValues)
          for (key in keys) {
              def val="${key}=${values[key]}"
              data += "${val}\n"
          }
        }
        script {
          sh "rm -f .env && touch .env"
          writeFile(file: '.env', text: data)
        }
      }
    }
    stage("Deploy to ECS"){
      steps {
        sh "docker context use prod-jd"
        sh "docker compose -p $DOCKER_IMAGE --env-file .env up -d"
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