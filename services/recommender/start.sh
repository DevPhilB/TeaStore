#!/usr/bin/env bash
# Sleep for 60 seconds so that database is ready
sleep 60
# Run jar depending on configured HTTP version
# HTTP/1.1 is the fallback configuration
#   JAR HTTP_VERSION GATEWAY_HOST RECOMMENDER_PORT (PERSISTENCE_PORT | for HTTP/3 only)
if [ $HTTP_VERSION == "HTTP/1.1" ]
then
    java -jar /service/recommender.jar $HTTP_VERSION $GATEWAY_HOST $RECOMMENDER_PORT
elif [ $HTTP_VERSION == "HTTP/2" ]
then
    java -jar /service/recommender.jar $HTTP_VERSION $GATEWAY_HOST $RECOMMENDER_PORT
elif [ $HTTP_VERSION == "HTTP/3" ]
then
    java -jar /service/recommender.jar $HTTP_VERSION $GATEWAY_HOST $RECOMMENDER_PORT $PERSISTENCE_PORT
else
    java -jar /service/recommender.jar HTTP/1.1 $GATEWAY_HOST 80
fi