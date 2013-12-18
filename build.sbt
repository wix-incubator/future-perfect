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
