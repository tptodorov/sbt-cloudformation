import com.github.tptodorov.sbt.cloudformation.CloudFormation
import com.github.tptodorov.sbt.cloudformation.Import.Keys._
import com.github.tptodorov.sbt.cloudformation.Import.Configurations._

CloudFormation.defaultSettings

stackParams in Staging := Map("NumberWithRange" -> "10", "StringWithLength" -> "longstring")
