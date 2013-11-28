import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "ssdemo-java"
    val appVersion      = "1.0"

    val appDependencies = Seq(
	    javaCore,
        "securesocial" %% "securesocial" % "master-SNAPSHOT",
        "mysql" % "mysql-connector-java" % "5.1.27",
        "org.mongodb.morphia" % "morphia" % "0.105",
        "com.google.code.gson" % "gson" % "2.2.4",
        "org.mongodb" % "mongo-java-driver" % "2.11.3",
        "org.jdbi" % "jdbi" % "2.51"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers ++= Seq(
        Resolver.mavenLocal,
        DefaultMavenRepository,
        Resolver.url("sbt-plugin-snapshots", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)
      )
    )

}
