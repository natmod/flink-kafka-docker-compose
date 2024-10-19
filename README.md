# flink-kafka-docker-compose.
 
## Setup your flink & kafka cluster via docker compose
```
1. Flink Job Manager
2. Flink Task Manager
3. Kafka.
4. Zookeeper
```

## Prereqs

- [Install Docker for Mac](https://docs.docker.com/docker-for-mac/install/)
- [Test your Docker Installation](https://docs.docker.com/docker-for-mac/#test-your-installation)
- [Docker Compose already included in Docker Desktop for Mac]
- Maven: run `brew install maven` on Mac

## Run

-  Cluster up 
```
   Run docker compose with dispatch mode on compose file
   docker-compose up -d
```
-  Validate status of cluster 

```
   Run:
   docker-compose ps
   
   All the 4 service containers should be up and running
                  Name                                Command               State                   Ports
--------------------------------------------------------------------------------------------------------------------------
flink-kafka-docker-example_jobmanager_1    /docker-entrypoint.sh jobm ...   Up      6123/tcp, 0.0.0.0:8081->8081/tcp
flink-kafka-docker-example_kafka_1         start-kafka.sh                   Up      0.0.0.0:9094->9094/tcp
flink-kafka-docker-example_taskmanager_1   /docker-entrypoint.sh task ...   Up      6121/tcp, 6122/tcp, 6123/tcp, 8081/tcp
flink-kafka-docker-example_zookeeper_1     /bin/sh -c /usr/sbin/sshd  ...   Up      2181/tcp, 22/tcp, 2888/tcp, 3888/tcp
```

## Default ports used 
```
Flink Web Client on port 8081-> http://localhost:8081/
JobManager RPC port 6123
TaskManagers RPC port 6122
TaskManagers Data port 6121
```

## Build and Run the Flink Job

### 1. Build the JAR

First, build the JAR file using Maven:
```
mvn clean package
```
Then copy the jar into your jobmanager
```
docker cp target/flink-kafka-example-1.0-SNAPSHOT.jar $(docker ps -qf name=jobmanager):/opt/flink/lib/flink-kafka-example-1.0-SNAPSHOT.jar
```
Then you can run the jar
```
docker exec -it $(docker ps -qf name=jobmanager) flink run -d /opt/flink/lib/flink-kafka-example-1.0-SNAPSHOT.jar
```
You should be able to see the job running in the Jobmanager UI and also via CLI:
```
docker exec -it $(docker ps -qf name=jobmanager) flink list
```
If you want to see job logs, you'll find them in the taskmanager. In this case, you won't see too much interesting there if job is running correctly:
```
docker logs -f $(docker ps -qf name=taskmanager)
```
Lastly, check the data in kafka. First input topic:
```
docker-compose exec kafka kafka-console-consumer --bootstrap-server kafka:29092 --topic input-topic --from-beginning
```
Then output topic:
```
docker-compose exec kafka kafka-console-consumer --bootstrap-server kafka:29092 --topic output-topic --from-beginning
```

You can follow the streams and see the words are reveresed in the output topic.
