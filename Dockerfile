FROM openjdk:8u312-jre-slim-buster
ARG JAR_FILE=./*.jar
COPY ${JAR_FILE} myJar.jar
COPY entrypoint.sh wait-for-it.sh ./
RUN chmod +x ./entrypoint.sh ./wait-for-it.sh 
ENTRYPOINT ["./entrypoint.sh"]
CMD ["java","-jar","myJar.jar"]