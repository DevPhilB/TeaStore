# TeaStore v2 (HTTP/1.1 | HTTP/2 | HTTP/3) #  

TeaStore v2 is a rework of the original [TeaStore](https://se.informatik.uni-wuerzburg.de/software-engineering-group/tools/teastore/) from Descartes Research Group at the University of Würzburg.  
TeaStore emulates a basic web store for automatically generated, tea and tea supplies.   
For more details visit the base [repository](https://github.com/DescartesResearch/TeaStore).

## Getting Started
### Prerequisites
Install dependencies (second command can be ignored for docker-compose run).
 ```sh
mvn clean install
docker build -t teastore-db:v2 database
 ```
### Run locally
You have to start the database first.
 ```sh
docker run -p 3306:3306 teastore-db:v2
 ```
- HttpPersistenceServer
- HttpAuthServer
- HttpWebServer

Wait until database is ready before starting the next services.

- HttpImageServer
- HttpRecommenderServer

### Run with docker-compose
Waiting for the database is already included.
 ```sh
 docker-compose up --build
 ```

### Run with DockerHub images (WIP)
- [Database](https://hub.docker.com/r/tvsjsdock/teastore-db)
- [Persistence](https://hub.docker.com/r/tvsjsdock/teastore-persistence)
- [Auth](https://hub.docker.com/r/tvsjsdock/teastore-auth)
- [Web](https://hub.docker.com/r/tvsjsdock/teastore-web)
- [Image](https://hub.docker.com/r/tvsjsdock/teastore-image)
- [Recommender](https://hub.docker.com/r/tvsjsdock/teastore-recommender)
 ```sh
docker-compose -f ./examples/docker/docker-compose_http.yaml up -d
 ```

## Changes (WIP)
- Registry replaced with [Traefik](https://github.com/traefik/traefik), a HTTP reverse proxy and load balancer 
- Netty HTTP servers (and client) instead of Jetty servlets
- Support for HTTP/1.1, HTTP/2 and HTTP/3
- Full JSON-API instead of JSPs
- Optimized API paths
- The database setup includes the data population
- Java 17 (LTS) as target environment

## Architecture (WIP)
TBD

API documentation is available in the [api](api) folder.

## Testing / Evaluation (WIP)
TBD

[Kieker](http://kieker-monitoring.net) and [RabbitMQ](https://www.rabbitmq.com/) may be removed.

## License
Distributed under the Apache-2.0 License. See `LICENSE` for more information.
If you use the TeaStore in a scientific context, please consider the [citation rules](https://github.com/DescartesResearch/TeaStore#cite-us).
