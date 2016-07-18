
enablePlugins(CloudFormation)

stackRegion := "us-east-1"

stackTemplateFile := file("vpc.template")

TaskKey[Unit]("check") := {
  require(stackTemplateFile.value.getName == "vpc.template")
}

