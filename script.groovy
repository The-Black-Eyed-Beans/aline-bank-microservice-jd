def buildApp() {
  sh "git submodule init"
  sh "git submodule update"
  sh "mvn install -DskipTests"
  sh "cp **/target/*.jar ."
}

def testApp() {
  if (params.IS_TESTING) {
    sh "mvn test"
  }
}

def postCleanup() {
  if (params.IS_CLEANWORKSPACE) {
    sh "rm -rf ./*"
    sh "docker image prune -af"
  }
}

def upstreamToECR() {
  if (params.IS_DEPLOYING) {
    env.CURRENT_HASH = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
    sh 'aws ecr get-login-password --region $ECR_REGION --profile joshua | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$ECR_REGION.amazonaws.com'
    sh "docker build -t ${DOCKER_IMAGE} ."
    sh 'docker tag $DOCKER_IMAGE:latest $AWS_ACCOUNT_ID.dkr.ecr.$ECR_REGION.amazonaws.com/$DOCKER_IMAGE-jd:$CURRENT_HASH'
    sh 'docker push $AWS_ACCOUNT_ID.dkr.ecr.$ECR_REGION.amazonaws.com/$DOCKER_IMAGE-jd:$CURRENT_HASH'
  }
}

return this