FROM openjdk:8u312-jre-slim-buster
ARG JAR_FILE=./*.jar
COPY ${JAR_FILE} entrypoint.sh ./
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
CMD ["java","-jar","*.jar"]