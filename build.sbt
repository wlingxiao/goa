import Dependencies._

lazy val commonSettings = Seq(
  organization := "com.github.wlingxiao",
  version := "0.0.3-SNAPSHOT",
  scalaVersion := "2.13.1",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:implicitConversions",
    "-unchecked"
  )
)

lazy val core = (project in file("core"))
  .configs(IntegrationTest)
  .settings(commonSettings, Defaults.itSettings)
  .settings(
    libraryDependencies ++= Seq(
      scalaLogging,
      nettyHandler,
      nettyCodecHttp,
      logbackClassic % Test,
      scalatest % "it,test",
      mockitoCore % Test,
      junit % Test,
      junitInterface % Test,
    ))

/*lazy val swagger = Project(id = "bleak-swagger", base = file("swagger"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      swaggerCore,
      jacksonModuleScala,
      logbackClassic % Test,
      swaggerUi % Test,
      scalatest % Test,
      mockitoCore % Test,
    ))
  .dependsOn(core)*/

parallelExecution in core := false
