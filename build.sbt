import aether.Aether._

name := "future-perfect"

organization := "com.wix"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.slf4j"   %   "slf4j-api"   % "1.7.5",
  "com.twitter" %%  "util-core"   % "6.3.6",
  "org.specs2"  %%  "specs2"      % "2.3.6" % "test",
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

Seq(aetherSettings: _*)

aetherArtifact <<= (coordinates, Keys.`package` in Compile, makePom in Compile, com.typesafe.sbt.pgp.PgpKeys.signedArtifacts in Compile) map {
  (coords: aether.MavenCoordinates, mainArtifact: File, pom: File, artifacts: Map[Artifact, File]) =>
    aether.Aether.createArtifact(artifacts, pom, coords, mainArtifact)
}