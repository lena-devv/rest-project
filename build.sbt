name := "rest-project"

version := "0.1"

scalaVersion := "2.11.12"


libraryDependencies ++= {
  val akkaVersion       = "10.1.11"
  Seq(
    // http
    "com.typesafe.akka" %% "akka-http" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % "2.5.26",
    "de.heikoseeberger" %% "akka-http-json4s" % "1.5.2",
    "org.json4s"        %% "json4s-native" % "3.2.11",
    
    // logging
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
  )
}