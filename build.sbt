import sbt._
import Keys._
import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._

name := "future-perfect"

organization := "com.wix"

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4", "2.11.2")


scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:experimental.macros")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.slf4j"   %   "slf4j-api"   % "1.7.5",
  "com.twitter" %%  "util-core"   % "6.20.0",
  "org.specs2"  %%  "specs2"      % "2.3.12" % "test",
  "org.jmock"   %  "jmock"       % "2.6.0" % "test"
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

licenses := Seq("Apache 2.0" -> url("http://www.opensource.org/licenses/Apache-2.0"))

homepage := Some(url("https://github.com/wix/future-perfect"))

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <scm>
    <url>git@github.com:wix/future-perfect.git</url>
    <connection>scm:git:git@github.com:wix/future-perfect.git</connection>
  </scm>
  <developers>
    <developer>
      <id>electricmonk</id>
      <name>Shai Yallin</name>
      <url>http://www.shaiyallin.com</url>
    </developer>
  </developers>
)

lazy val publishSignedAction = { st: State =>
  val extracted = Project.extract(st)
  val ref = extracted.get(thisProjectRef)
  extracted.runAggregated(com.typesafe.sbt.pgp.PgpKeys.publishSigned in Global in ref, st)
}

releaseSettings

releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies       //
  , runTest                         //
  , inquireVersions                 //
  , setReleaseVersion               //
  , commitReleaseVersion            //performs the initial git checks
  , tagRelease                      //
  , publishArtifacts.copy(action =  //uses publish-signed instead of publish if configured.
      publishSignedAction
    )
  , setNextVersion                  //
  , commitNextVersion               //
  , pushChanges                     //also checks that an upstream branch is properly configured
)
