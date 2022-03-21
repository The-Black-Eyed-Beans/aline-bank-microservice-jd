def createEnv() {
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

def buildApp() {
  sh "git submodule init"
  sh "git submodule update"
  sh "mvn install -DskipTests"
  sh "cp **/target/*.jar ."
}

def deployECS() {
  sh "aws s3 cp s3://beb-bucket-jd/cluster/aline/docker-compose.yaml . --profile joshua"
  sh "aws secretsmanager get-secret-value prod/services secrets.json --profile joshua | jq -r '.["SecretString"]' | jq '.' > secrets.json"
  def jsonSlurper = new JsonSlurper()
  data = jsonSlurper.parse(new File(filename))
}

def getSecrets() {
  sh "aws s3 cp s3://beb-bucket-jd/cluster/aline/docker-compose.yaml . --profile joshua"
  sh """aws secretsmanager  get-secret-value --secret-id prod/services --region us-east-2 --profile joshua | jq -r '.["SecretString"]' | jq '.' > secrets"""
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
