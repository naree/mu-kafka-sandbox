Playing with Mu + Avro + fs2-kafka 

To start the consumer 

```
sbt "runMain foo.main.Consumer"
```

To start a producer and send a message with User in version 1

```
sbt "runMain foo.main.Producer"
```

To start a producer and send a message with User in version 2

```
sbt "runMain foo.main.Producer2"
```

