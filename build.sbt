import SimpleSettings._

primarySettings := primary(
    name             = "future-perfect"
  , companyName      = "Wix.com"
  , organization     = "com.wix"
  , homepage         = "https://github.com/wix/future-perfect"
  , vcsSpecification = "git@github.com:wix/future-perfect.git"
)

compilerSettings := compiling(
    scalaVersion  = "2.10.3"
  , scalacOptions = Seq("-deprecation", "-unchecked", "-feature")
)

mavenSettings := maven(
  license(
      name  = "The Apache Software License, Version 2.0"
    , url   = "http://www.apache.org/licenses/LICENSE-2.0.txt"
  ),
  developer(
      id              = "electricmonk"
    , name            = "Shai Yallin"
    , email           = "shai.yallin@gmail.com"
    , url             = "http://www.shaiyallin.com/"
    , organization    = "Wix.com"
    , organizationUri = "http://www.wix.com/"
    , roles           = Seq("Architect")
  )
)

publishSettings := publishing(
    signArtifacts = true
  , releaseCredentialsID  = "sonatype-nexus-staging"
  , releaseRepository     = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  , snapshotCredentialsID = "sonatype-nexus-snapshots"
  , snapshotRepository    = "https://oss.sonatype.org/content/repositories/snapshots"
)

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.slf4j"   %   "slf4j-api"   % "1.7.5",
  "com.twitter" %%  "util-core"   % "6.3.6",
  "org.specs2"  %%  "specs2"      % "2.3.6" % "test",
  "org.jmock"   %  "jmock"       % "2.6.0" % "test"
)

releaseSettings