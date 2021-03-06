resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
  url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
    Resolver.ivyStylePatterns)

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.2")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value

