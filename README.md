Playing with Mu + Avro + fs2-kafka 

To start the Kafka consumer 

```
sbt "runMain foo.main.Consumer"
```

To start the Kafka producer and send a message with User in version 1

```
sbt "runMain foo.main.Producer"
```

To start the Kafka producer and send a message with User in version 2

```
sbt "runMain foo.main.Producer2"
```

To start the RPC server & producer combined 

```
sbt "runMain foo.main.UserV1Server"
```

To start the RPC client

```
sbt "runMain foo.main.UserV1Client"
```

