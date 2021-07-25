#!/usr/bin/env bash
# Sleep for 15 seconds so that database is ready
sleep 15
# Run jar depending on configured HTTP version
# HTTP/1.1 is the fallback configuration
if [ $VERSION == "HTTP/1.1" ]
then
    java -jar /service/recommender.jar HTTP/1.1 $GATEWAY_HOST 80
elif [ $VERSION == "HTTP/2" ]
then
    java -jar /service/recommender.jar HTTP/2 $GATEWAY_HOST 443
elif [ $VERSION == "HTTP/3" ]
then
    java -jar /service/recommender.jar HTTP/3 $GATEWAY_HOST 4433
else
    java -jar /service/recommender.jar HTTP/1.1 $GATEWAY_HOST 80
fi