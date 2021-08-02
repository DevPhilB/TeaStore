#!/usr/bin/env bash
# h2load configuration
# HTTP/1.1 is the default configuration
HTTP_VERSION="HTTP/1.1"
WEB_SERVICE=localhost
WEB_PORT=80
# Run h2load against the microservice architecture
if [ $HTTP_VERSION == "HTTP/1.1" ]
then
    h2load -n10000 -c100 --h1 http://$WEB_SERVICE:$WEB_PORT/api/web/about
elif [ $HTTP_VERSION == "HTTP/2" ]
then
    h2load -n10000 -c100 -m10 --npn-list h2 https://$WEB_SERVICE:$WEB_PORT/api/web/about
elif [ $HTTP_VERSION == "HTTP/3" ]
then
    h2load -n1000 -c100 -m10 --npn-list h3 https://$WEB_SERVICE:$WEB_PORT/api/web/about
fi