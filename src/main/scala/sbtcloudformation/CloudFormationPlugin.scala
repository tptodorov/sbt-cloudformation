package sbtcloudformation

import sbt._
import sbt.Keys._
import scala.util.Try
import com.amazonaws.auth.{DefaultAWSCredentialsProviderChain, AWSCredentials}
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model._
import scala.collection.convert.WrapAsScala._
import scala.collection.convert.WrapAsJava._
import scala.util.Failure
import sbt.Configuration
import com.amazonaws.regions._

object CloudFormationPlugin extends sbt.Plugin {


  object Configurations {
    lazy val Production = config("production")
    lazy val Staging = config("staging")
  }

  object Keys {

    // for all configurations
    val templatesSourceFolder = settingKey[File]("folder where CloudFormation templates are")
    val awsCredentials = settingKey[AWSCredentials]("AWS credentials")
    val templates = settingKey[Seq[File]]("template sources")
    val validate = taskKey[Seq[File]]("validate templates")
    val startParameters = settingKey[Map[String, String]]("parameters for starting CloudFormation")

    // stack operations
    val stackTemplate = settingKey[String]("default template to use for this configuration")
    val stackParams = settingKey[Map[String, String]]("Parameters applied to the template for this configuration")
    val stackCapabilities = settingKey[Seq[String]]("The list of capabilities that you want to allow in the stack . E.g.[CAPABILITY_IAM]")
    val stackRegion = settingKey[String]("The region where the stacks are deployed. E.g. eu-west-1 ")
    val client = settingKey[AmazonCloudFormationClient]("AWS CF client")

    val stackName = settingKey[String]("stack name")
    val describe = taskKey[Unit]("describe stack completely")
    val describeStatus = taskKey[Unit]("describe stack status")
    val create = taskKey[String]("create a stack and returns its stackId")
    val delete = taskKey[Unit]("delete a stack")
  }


  import Keys._
  import Configurations._

  private lazy val awsCredentialsProvider = new DefaultAWSCredentialsProviderChain()

  private val commonSettings = Seq(
    templatesSourceFolder <<= baseDirectory {
      base => base / "src/main/aws"
    },
    templates := {
      val templates = templatesSourceFolder.value ** GlobFilter("*.template")
      templates.get
    },
    awsCredentials := {
      awsCredentialsProvider.getCredentials
    }
  )

  val validationSettings = commonSettings ++ Seq(

    watchSources <++= templates map identity,
    validate <<= (awsCredentials, templates, streams) map {
      (credentials, files, s) =>


        def validateTemplate(client: AmazonCloudFormationClient, log: Logger)(template: sbt.File): (File, Try[List[String]]) = {
          (template, Try {
            val request: ValidateTemplateRequest = new ValidateTemplateRequest()
            request.setTemplateBody(IO.read(template))
            val result = client.validateTemplate(request)
            log.debug(s"result from validating $template : $result")
            log.info(s"validated $template")
            result.getParameters.toList.map(_.getParameterKey)
          })
        }

        val client = new AmazonCloudFormationClient(credentials)
        val results: Seq[(File, Try[List[String]])] = files.map(validateTemplate(client, s.log))
        results.foreach {
          tr =>
            tr._2 match {
              case Failure(e) => s.log.error(s"validation of ${tr._1} failed with: \n ${e.getMessage}")
              case _ =>
            }
        }

        if (results.exists(_._2.isFailure)) {
          sys.error("some AWS CloudFormation templates failed to validate!")
        }

        files
    }
  )

  val operationSettings = commonSettings ++ Seq(
    stackRegion := "eu-west-1",
    stackTemplate <<= templates {
      files =>
        IO.read(files.head)
    },
    stackName <<= normalizedName,
    stackCapabilities := Seq()
  ) ++ makeOperationConfig(Staging) ++ makeOperationConfig(Production)

  val defaultCloudFormationSettings = validationSettings ++ operationSettings


  def makeOperationConfig(config: Configuration) = Seq(

    stackTemplate in config <<= stackTemplate,
    stackParams in config := Map(),
    stackName in config <<= stackName {
      normName =>
        s"${config.name}-$normName"
    },
    stackRegion in config <<= stackRegion,
    stackCapabilities in config <<= stackCapabilities,
    client in config <<= (stackRegion in config, awsCredentials) {
      (region, credentials) =>
        val client = new AmazonCloudFormationClient(credentials)
        client.setRegion(Region.getRegion(Regions.fromName(region)))
        client
    },
    describe in config <<= (client in config, stackName in config, streams) map {
      (cl, stack, s) =>

        val request: DescribeStacksRequest = new DescribeStacksRequest()
        request.setStackName(stack)
        val response = cl.describeStacks(request)
        response.getStacks.toList.foreach(stack => s.log.info(s"${stack.toString}"))
    },
    describeStatus in config <<= (client in config, stackName in config, streams) map {
      (cl, stack, s) =>

        val request: DescribeStacksRequest = new DescribeStacksRequest()
        request.setStackName(stack)
        val response = cl.describeStacks(request)
        response.getStacks.toList.foreach(stack => s.log.info(s"${stack.getStackStatus} - ${stack.getStackStatusReason}"))
    },
    create in config <<= (client in config, stackName in config, stackTemplate in config, stackParams in config, stackCapabilities in config, streams) map {
      (cl, stack, template, params, capabilities, s) =>

        val request = new CreateStackRequest
        request.setStackName(stack)
        request.setTemplateBody(template)
        request.setCapabilities(capabilities)

        val reqParams = for {
          (k, v) <- params
          p = new Parameter()
        } yield {
          p.setParameterKey(k)
          p.setParameterValue(v)
          p
        }

        request.setParameters(reqParams.toList)

        val result = cl.createStack(request)

        s.log.info(s"created stack ${request.getStackName} / ${result.getStackId}")
        result.getStackId
    },
    delete in config <<= (client in config, stackName in config, streams) map {
      (cl, stack, s) =>

        val request = new DeleteStackRequest
        request.setStackName(stack)

        cl.deleteStack(request)

        s.log.info(s"deleting stack ${request.getStackName} ")

    }
  )


}
