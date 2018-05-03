## TagWriter fails if lagom service started before Cassandra

You'll need to have docker installed for this reproduction
> note: each section below continues from the previous

#### bring up services
- clone repository
- $ `sbt docker:publishLocal`
- $ `cd docker-compose/write-tag-fail/`
- wait for hello
  ```
  hello_1      | 2018-05-03T02:50:55.541Z [info] play.core.server.AkkaHttpServer [] - Listening for HTTP on /172.26.0.20:9000
  ```
- wait for cassandra
  ```
  cassandra_1  | INFO  [main] 2018-05-03 02:51:09,853 Server.java:156 - Starting listening for CQL clients on /0.0.0.0:9042 (unencrypted)...
  ```
- in another terminal
- $ `http 172.26.0.20:9000/api/hello/Alice message=Hi`
  > note: the above uses [http](https://httpie.org/), feel free to translate to curl
- repeated errors in first terminal:
  ```
  hello_1      | 2018-05-03T03:16:28.977Z [warn] akka.persistence.cassandra.journal.TagWriter [sourceThread=application-
  lagom.persistence.dispatcher-43, akkaTimestamp=03:16:28.976UTC, akkaSource=akka.tcp://application@172.26.0.20:2552/system
  /cassandra-journal/tagWrites/com.example.hello.impl.HelloEvent, sourceActorSystem=application] - Writing tags has failed. 
  This means that any eventsByTag query will be out of date. The write will be retried. Reason 
  com.datastax.driver.core.exceptions.NoHostAvailableException: All host(s) tried for query failed (tried: /172.26.0.10:9042
  (com.datastax.driver.core.exceptions.TransportException: [/172.26.0.10:9042] Cannot connect))
  ```
- Ctrl-c

#### bringing up cassandra first works
- $ `docker-compose start cassandra`
- $ `docker-compose logs -f cassandra`
- wait for ready
- Ctrl-c
- $ `docker-compose start hello`
- $ `docker-compose logs -f hello`
- wait for ready
- in another terminal
- $ `http 172.26.0.20:9000/api/hello/Alice message=Hi`
- no errors in first terminal
- $ `docker-compose stop`

#### bringing up hello first fails
- $ `docker-compose start hello`
- $ `docker-compose logs -f hello`
- wait for ready
- Ctrl-c
- $ `docker-compose start cassandra`
- $ `docker-compose logs -f hello cassandra`
- wait for ready
- in another terminal
- $ `http 172.26.0.20:9000/api/hello/Alice message=Hi`
- errors in first terminal
- Ctrl-c

#### restarting hello brings it back to a working state
- $ `docker-compose restart hello`
- $ `docker-compose logs -f hello cassandra`
- wait for ready
- in another terminal
- $ `http 172.26.0.20:9000/api/hello/Alice message=Hi`
- no errors in first terminal

#### tear down services
- $ `docker-compose down -v`
