FROM openjdk:8u312-jre-slim-buster
ARG JAR_FILE=./*.jar
COPY ${JAR_FILE} myJar.jar
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
CMD ["java","-jar","myJar.jar"]
