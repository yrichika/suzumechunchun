import play.core.PlayVersion.akkaVersion

name := """SuzumeChunChun"""
organization := "com.suzumechunchun"

version := "0.1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.5"

// Only for Arm Mac: https://github.com/playframework/playframework/issues/10372
// Comment this out if you use Windows or Intel Mac.
PlayKeys.fileWatchService := play.dev.filewatch.FileWatchService.jdk7(play.sbt.run.toLoggerProxy(sLog.value))


cancelable in Global := true
// For silhouette repo
resolvers += "Atlassian's Maven Public Repository" at "https://packages.atlassian.com/maven-public/"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
// Need this for websocket testing. To manually create JWT cookie for session
libraryDependencies += "com.pauldijou" %% "jwt-play-json" % "4.2.0"
// parallelExecution in Test := false

// Adds additional packages into Twirl
// TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.2.0",
  "org.scalatest" %% "scalatest" % "3.2.0" % "test"
)

// evolutions
libraryDependencies += evolutions

// Slick
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "5.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
  // H2
  "com.h2database" % "h2" % "1.4.199",
  // MySQL
  "mysql" % "mysql-connector-java" % "8.0.21",
  // postgres
  "org.postgresql" % "postgresql" % "42.2.18"

)

// Caffeine
libraryDependencies += caffeine

// Tink
libraryDependencies ++= Seq(
  "com.google.crypto.tink" % "tink" % "1.4.0",
  // For AWS-KMS
  "com.google.crypto.tink" % "tink-awskms" % "1.4.0"
)


// silhouette
libraryDependencies ++= Seq(
  "com.mohiva" %% "play-silhouette" % "7.0.0",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "7.0.0",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "7.0.0",
  "com.mohiva" %% "play-silhouette-persistence" % "7.0.0",
  "com.mohiva" %% "play-silhouette-testkit" % "7.0.0" % "test"
)

// Used only in modules.EvolutionsModule.
libraryDependencies += "commons-io" % "commons-io" % "2.7"

// Rate Limiting
libraryDependencies += "com.digitaltangible" %% "play-guard" % "2.5.0"

// WebSocket
libraryDependencies ++= Seq(
  // For logging
  "net.logstash.logback" % "logstash-logback-encoder" % "6.2",
  // InputSanitizer
  "org.jsoup" % "jsoup" % "1.12.1",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
)
