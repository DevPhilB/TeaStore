#!/usr/bin/env bash
# Sleep for 15 seconds so that database is ready
sleep 15
# Run jar
java -jar /service/recommender.jar HTTP/1.1 http:// gateway 80