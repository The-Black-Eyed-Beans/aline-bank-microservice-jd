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
    booleanParam(name: "IS_TESTING", defaultValue: "false", description: "Set to false to skip testing, default true!")
  }

  environment {
    AWS_ACCOUNT_ID = credentials("AWS_ACCOUNT_ID")
    DOCKER_IMAGE = "bank"
    ECR_REGION = "us-east-2"
    COMMIT_HASH = "${sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()}"
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
        sh "rm -f .env && touch .env"
        writeFile(file: '.env', text: data)
        sh "echo 'BUILD_TAG=$COMMIT_HASH' >> .env"
        sh "echo 'APP_PORT=80' >> .env"
        sh "echo 'WAIT_TIME=1000' >> .env"
        sh "cat .env"
      }
    }
    stage("Deploy to ECS"){
      steps {
        sh "cat docker-compose.yaml"
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