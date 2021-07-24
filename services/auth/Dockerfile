FROM openjdk:17-jdk

WORKDIR /service

# copy shell script
COPY start.sh /service/start.sh
RUN chmod +x /service/start.sh

# copy the packaged jar file into our docker image
COPY target/auth.jar /service/auth.jar

# run the shell script
CMD ["/service/start.sh"]