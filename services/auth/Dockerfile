FROM openjdk:17-jdk

WORKDIR /service
# copy the packaged jar file into our docker image
COPY target/auth.jar /service/auth.jar

# set the startup command to execute the jar
CMD ["java", "-jar", "/service/auth.jar", "HTTP/1.1", "http://", "gateway", "80"]