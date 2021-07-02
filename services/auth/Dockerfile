FROM openjdk:17-jdk

# copy the packaged jar file into our docker image
COPY target/auth.jar /auth.jar

# set the startup command to execute the jar
CMD ["java", "-jar", "/auth.jar"]