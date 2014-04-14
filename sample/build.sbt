import sbtcloudformation.CloudFormationPlugin

import CloudFormationPlugin.Keys._
import CloudFormationPlugin.Configurations._

CloudFormationPlugin.defaultCloudFormationSettings

stackParams in Staging := Map("NumberWithRange" -> "10", "StringWithLength" -> "longstring")
