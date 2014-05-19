
import AssemblyKeys._ // put this at the top of the file

assemblySettings

name := "diss"

version := "0.1"

scalaVersion := "2.10.3"

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

exportJars := true

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies +=  "com.typesafe.akka" % "akka-actor_2.10" % "2.3.1"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.1.0" % "test"

libraryDependencies += "org.mashupbots.socko" %% "socko-webserver" % "0.4.2"

//libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.6.0-RC0"

mainClass in Compile := Some("hu.frankdavid.diss.network.NetworkHandlerTest")

unmanagedBase <<= baseDirectory { base => base / "lib" }