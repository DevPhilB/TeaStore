#!/usr/bin/env bash
# Run jar depending on configured HTTP version
# HTTP/1.1 is the fallback configuration
#   JAR HTTP_VERSION GATEWAY_HOST WEB_PORT (PERSISTENCE_PORT AUTH_PORT IMAGE_PORT RECOMMENDER_PORT | for HTTP/3 only)
if [ $HTTP_VERSION == "HTTP/1.1" ]
then
    java -jar /service/web.jar $HTTP_VERSION $GATEWAY_HOST $WEB_PORT
elif [ $HTTP_VERSION == "HTTP/2" ]
then
    java -jar /service/web.jar $HTTP_VERSION $GATEWAY_HOST $WEB_PORT
elif [ $HTTP_VERSION == "HTTP/3" ]
then
    java -jar /service/web.jar $HTTP_VERSION $GATEWAY_HOST $WEB_PORT $PERSISTENCE_PORT $AUTH_PORT $IMAGE_PORT $RECOMMENDER_PORT
else
    java -jar /service/web.jar HTTP/1.1 $GATEWAY_HOST 80
fi