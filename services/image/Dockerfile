FROM openjdk:17-jdk

WORKDIR /service
# copy setup images
COPY src/main/resources/image/setup/categoryimg /service/categoryimg
COPY src/main/resources/image/setup/existingimg /service/existingimg

# copy shell script
COPY start.sh /service/start.sh
RUN chmod +x /service/start.sh

# copy the packaged jar file into our docker image
COPY target/image.jar /service/image.jar

# run the shell script
CMD ["/service/start.sh"]