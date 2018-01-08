import bintray.Keys._

sbtPlugin := true

name := "sbt-cloudformation"

organization := "com.github.tptodorov"

scalaVersion := "2.12.4"
    
sbtVersion in Global := "1.0.4"

scalaCompilerBridgeSource := {
   val sv = appConfiguration.value.provider.id.version
   ("org.scala-sbt" % "compiler-interface" % sv % "component").sources
}

libraryDependencies += "com.amazonaws" % "aws-java-sdk-cloudformation" % "1.11.76"

publishTo := { 
   val v = version.value
   val scalasbt = "https://dl.bintray.com/sbt/"
   val (name, url) = if (v.contains("-SNAPSHOT"))
                       ("sbt-plugin-snapshots-publish", scalasbt+"sbt-plugin-snapshots")
                     else
                       ("sbt-plugin-releases-publish", scalasbt+"sbt-plugin-releases")
   Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

bintraySettings

packageLabels in bintray := Seq("aws", "cloudformation")

publishMavenStyle := false

repository in bintray := "sbt-plugins"

bintrayOrganization in bintray := None

ScriptedPlugin.scriptedSettings

scriptedSettings

scriptedBufferLog := false

scriptedLaunchOpts ++= Seq("-Xmx1G", "-Dplugin.version=" + version.value)

