#!/usr/bin/env bash
# Sleep for 10 seconds so that database is ready
sleep 10
# Run jar
java -jar /service/image.jar HTTP/1.1 http:// gateway 80