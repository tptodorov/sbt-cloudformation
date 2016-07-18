import bintray.Keys._

sbtPlugin := true

name := "sbt-cloudformation"

organization := "com.github.tptodorov"

version := "0.7.0-SNAPSHOT"

scalaVersion := "2.10.5"

libraryDependencies += "com.amazonaws" % "aws-java-sdk-cloudformation" % "1.11.14"

publishTo <<= (version) { version: String =>
   val scalasbt = "http://scalasbt.artifactoryonline.com/scalasbt/"
   val (name, url) = if (version.contains("-SNAPSHOT"))
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

