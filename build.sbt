name := "future-perfect"

organization := "com.wix"

version := "0.1.0-SNAPSHOT"

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

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/wix/future-perfect</url>
  <licenses>
    <license>
      <name>Apache 2.0</name>
      <url>http://www.opensource.org/licenses/Apache-2.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
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
