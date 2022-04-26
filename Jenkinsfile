pipeline {
  agent {
    node {
      label "worker-one"
    }
  }
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
    AWS_PROFILE = credentials("AWS_PROFILE")
    CLUSTER_NAME = credentials("CLUSTER_NAME")
    COMMIT_HASH = "${sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()}"
    DOCKER_IMAGE = "bank"
    ECR_REGION = credentials("AWS_REGION")
    IS_ECS = getIsECS()
  }

  stages {
    stage("Fetch Submodules") {
      steps {
        sh "git submodule init"
        sh "git submodule update"
      }
    }
    stage("Test") {
      steps {
        script {
          if (params.IS_TESTING) {
            sh "mvn clean test"
          }
        }
      } 
    }   
    stage("Package Artifact") {
      steps {
        sh "mvn package -DskipTests"
      }
    } 
    stage("SonarQube") {
      steps {
        withSonarQubeEnv("us-west-1-sonar") {
          sh "mvn verify sonar:sonar -Dmaven.test.failure.ignore=true"
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
        upstreamToECR()
      }
    }
    stage("Fetch Environment Variables"){
      steps {
        script {
          if (env.IS_ECS.toBoolean()) {
            sh "aws lambda invoke --function-name getServiceEnv env --profile $AWS_PROFILE"
            createEnvFile()
          }
        }
      }
    }
    stage("Update Cluster"){
      steps {
        script {
          if (env.IS_ECS.toBoolean()) {
            sh "docker context use prod-jd"
            sh "docker compose -p $DOCKER_IMAGE-jd --env-file service.env up -d"
          } else  {
            sh "aws eks update-kubeconfig --name=$CLUSTER_NAME --region=us-east-2"
            sh "kubectl rollout restart deploy $DOCKER_IMAGE-deployment -n backend"
          }
        }
      }
    }
  }
  post {
    cleanup {
      script {
        sh "docker context use default"
        sh "rm -rf ./*"
        sh "docker image prune -af"
      }
    }
  }
}

def createEnvFile() {
  def env = sh(returnStdout: true, script: """cat ./env | jq -r '.["body"]' | base64 --decode""").trim()
  writeFile file: 'service.env', text: env
}

def getIsECS() {
    return sh(returnStdout: true, script: """aws secretsmanager  get-secret-value --secret-id prod/infrastructure/config --region us-east-2 | jq -r '.["SecretString"]' | jq -r '.["is_ecs"]'""").trim()
}

def upstreamToECR() {
  if (params.IS_DEPLOYING) {
    sh "cp $DOCKER_IMAGE-microservice/target/*.jar ."
    sh "docker context use default"
    sh 'aws ecr get-login-password --region $ECR_REGION --profile joshua | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$ECR_REGION.amazonaws.com'
    sh "docker build -t ${DOCKER_IMAGE} ."
    sh 'docker tag $DOCKER_IMAGE:latest $AWS_ACCOUNT_ID.dkr.ecr.$ECR_REGION.amazonaws.com/$DOCKER_IMAGE-microservice-jd:$COMMIT_HASH'
    sh 'docker tag $DOCKER_IMAGE:latest $AWS_ACCOUNT_ID.dkr.ecr.$ECR_REGION.amazonaws.com/$DOCKER_IMAGE-microservice-jd:latest'
    sh 'docker push $AWS_ACCOUNT_ID.dkr.ecr.$ECR_REGION.amazonaws.com/$DOCKER_IMAGE-microservice-jd:$COMMIT_HASH'
    sh 'docker push $AWS_ACCOUNT_ID.dkr.ecr.$ECR_REGION.amazonaws.com/$DOCKER_IMAGE-microservice-jd:latest'
  }
}
