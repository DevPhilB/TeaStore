#!/usr/bin/env bash
# h2load configuration
# HTTP/1.1 is default configuration
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
    # HTTP/3 client freeze with -c100 and/or -m10
    h2load -n1000 --npn-list h3 https://$WEB_SERVICE:$WEB_PORT/api/web/about &
    L1=$!
    h2load -n1000 --npn-list h3 https://$WEB_SERVICE:$WEB_PORT/api/web/about &
    L2=$!
    h2load -n1000 --npn-list h3 https://$WEB_SERVICE:$WEB_PORT/api/web/about &
    L4=$!
    h2load -n1000 --npn-list h3 https://$WEB_SERVICE:$WEB_PORT/api/web/about &
    L5=$!
    h2load -n1000 --npn-list h3 https://$WEB_SERVICE:$WEB_PORT/api/web/about &
    L6=$!
    h2load -n1000 --npn-list h3 https://$WEB_SERVICE:$WEB_PORT/api/web/about &
    L7=$!
    h2load -n1000 --npn-list h3 https://$WEB_SERVICE:$WEB_PORT/api/web/about &
    L8=$!
    h2load -n1000 --npn-list h3 https://$WEB_SERVICE:$WEB_PORT/api/web/about &
    L9=$!
    h2load -n1000 --npn-list h3 https://$WEB_SERVICE:$WEB_PORT/api/web/about &
    L10=$!
    wait $L1 $L2 $L3 $L4 $L5 $L6 $L7 $L8 $L9 $L10
fi