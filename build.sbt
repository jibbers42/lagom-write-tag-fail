import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

organization in ThisBuild := "com.example"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"
val akkaPersCassVersion = "0.84"

val akkaPersCass          = "com.typesafe.akka"          %% "akka-persistence-cassandra"          % akkaPersCassVersion
val akkaPersCassLauncher  = "com.typesafe.akka"          %% "akka-persistence-cassandra-launcher" % akkaPersCassVersion % Test
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test

lazy val `hello` = (project in file("."))
  .aggregate(`hello-api`, `hello-impl`)

lazy val `hello-api` = (project in file("hello-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `hello-impl` = (project in file("hello-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      akkaPersCass,
      lagomScaladslPersistenceCassandra,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    ),
    dockerBaseImage := "openjdk:8-jdk-slim-stretch",
    dockerRepository := Some("write-tag-fail"),
    dockerEntrypoint ++= (
      """ -Dhttp.address="$(eval "echo $SERVICE_BIND_ADDRESS")"""" +
      """ -Dhttp.port="$(eval "echo $SERVICE_BIND_PORT")"""" +
      """ -Dakka.remote.netty.tcp.hostname="$(eval "echo $AKKA_REMOTING_HOST")"""" +
      """ -Dakka.remote.netty.tcp.port="$(eval "echo $AKKA_REMOTING_PORT")"""" +
      """ -Dakka.remote.netty.tcp.bind-hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")"""" +
      """ -Dakka.remote.netty.tcp.bind-port="$(eval "echo $AKKA_REMOTING_BIND_PORT")""""
      ).split(" ").toSeq,
    dockerCommands := dockerCommands.value.flatMap {
      case ExecCmd("ENTRYPOINT", args @ _*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
      case v => Seq(v)
    },
    javaOptions in Universal += "-Dpidfile.path=/dev/null" /* fixes play RUNNING_PID issues, https://stackoverflow.com/a/29244028/3705269 */
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`hello-api`)
