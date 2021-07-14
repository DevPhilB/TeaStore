FROM openjdk:17-jdk

WORKDIR /service
# copy the packaged jar file into our docker image
COPY target/recommender.jar /service/recommender.jar

# set the startup command to execute the jar
CMD ["java", "-jar", "/service/recommender.jar", "HTTP/1.1", "http://", "gateway", "80"]