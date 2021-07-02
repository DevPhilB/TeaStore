FROM openjdk:17-jdk

# copy the packaged jar file into our docker image
COPY target/persistence.jar /persistence.jar

# set the startup command to execute the jar
CMD ["java", "-jar", "/persistence.jar"]