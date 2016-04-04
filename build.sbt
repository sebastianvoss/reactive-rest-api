name := "ReactiveRestApi"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.3",
  "com.typesafe.akka" %% "akka-http-core" % "2.4.3",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.3",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.3",
  "org.reactivemongo" %% "reactivemongo" % "0.11.10",
  "ch.qos.logback" % "logback-classic" % "1.1.7"
)
    