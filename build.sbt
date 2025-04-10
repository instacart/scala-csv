import scala.sys.process._

Global / onChangedBuildSource := ReloadOnSourceChanges

name := "scala-csv"

version := "1.3.8"

scalaVersion := "2.12.10"

crossScalaVersions := Seq("2.12.15", "2.11.12", "2.10.7", "2.13.7", "3.1.0")

TaskKey[Unit]("checkScalariform") := {
  val diff = "git diff".!!
  if(diff.nonEmpty){
    sys.error("Working directory is dirty!\n" + diff)
  }
}

organization := "com.github.tototoshi"

libraryDependencies ++= {
  Seq(
    "org.scalatest" %% "scalatest" % "3.2.10" % Test,
    if (scalaVersion.value.startsWith("2.")) "org.scalacheck" %% "scalacheck" % "1.14.3" % Test
    else "org.scalacheck" %% "scalacheck" % "1.15.4" % Test
  )
}

val enableScalameter = settingKey[Boolean]("")

enableScalameter := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) =>
      11 <= v && v <= 13
    case _ =>
      false
  }
}

libraryDependencies ++= {
  if (enableScalameter.value) {
    Seq("com.storm-enroute" %% "scalameter" % "0.19" % "test")
  } else {
    Nil
  }
}

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:implicitConversions"
)

scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)){
  case Some((2, v)) if v >= 11 => Seq("-Ywarn-unused")
}.toList.flatten

Test / sources := {
  val s = (Test / sources).value
  val exclude = Set("CsvBenchmark.scala")
  if (enableScalameter.value) {
    s
  } else {
    s.filterNot(f => exclude(f.getName))
  }
}

testFrameworks += new TestFramework(
  "org.scalameter.ScalaMeterFramework"
)

Test / parallelExecution := false

logBuffered := false

compile / javacOptions += "-Xlint"

compile / javacOptions ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v <= 11 =>
      Seq("-target", "6", "-source", "6")
    case _ =>
      if (scala.util.Properties.isJavaAtLeast("9")) {
        // if Java9
        Nil
      } else {
        Seq("-target", "8")
      }
  }
}

initialCommands := """
                     |import com.github.tototoshi.csv._
                   """.stripMargin

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

Test / publishArtifact := false

pomExtra := <url>http://github.com/tototoshi/scala-csv</url>
<licenses>
  <license>
    <name>Apache License, Version 2.0</name>
    <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
    <distribution>repo</distribution>
  </license>
</licenses>
<scm>
  <url>git@github.com:tototoshi/scala-csv.git</url>
  <connection>scm:git:git@github.com:tototoshi/scala-csv.git</connection>
</scm>
<developers>
  <developer>
    <id>tototoshi</id>
    <name>Toshiyuki Takahashi</name>
    <url>http://tototoshi.github.com</url>
  </developer>
</developers>

Compile / unmanagedSourceDirectories += {
  val dir = CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v <= 12 =>
      "scala-2.13-"
    case _ =>
      "scala-2.13+"
  }
  baseDirectory.value / "src" / "main" / dir
}
