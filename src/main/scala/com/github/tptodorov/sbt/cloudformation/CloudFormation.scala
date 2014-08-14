package com.github.tptodorov.sbt.cloudformation

import java.io.FileNotFoundException
import java.util

import com.amazonaws.auth.{AWSCredentials, DefaultAWSCredentialsProviderChain}
import com.amazonaws.regions._
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model._
import sbt.Keys._
import sbt._

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.immutable.Iterable
import scala.util.{Failure, Try}

object Import {

  object Configurations {
    lazy val Production = config("production")
    lazy val Staging = config("staging")
  }

  object Keys {

    type Parameters = Map[String, String]
    type Tags = Map[String, String]

    // for all configurations
    val awsCredentials = settingKey[AWSCredentials]("AWS credentials")
    val templatesSourceFolder = settingKey[File]("folder where CloudFormation templates are")
    val templates = settingKey[Seq[File]]("template sources")

    // in each configuration
    val stackTemplate = taskKey[String]("default template to use for this configuration")
    val stackParams = taskKey[Parameters]("Parameters applied to the template for this configuration")
    val stackTags = settingKey[Tags]("Tags of this stack")
    val stackCapabilities = settingKey[Seq[String]]("The list of capabilities that you want to allow in the stack . E.g.[CAPABILITY_IAM]")
    val stackRegion = settingKey[String]("The region where the stacks are deployed. E.g. eu-west-1 ")
    val stackName = settingKey[String]("stack name")


    // stack operations
    val stackValidate = taskKey[Seq[File]]("validate templates")
    val stackStatus = taskKey[Option[String]]("describe stack status")
    val stackWait = taskKey[Option[String]]("evaluates the stack until it becomes COMPLETED or FAILED, returns last status")

    val stackDescribe = taskKey[Option[Stack]]("describe stack completely")
    val stackCreate = taskKey[String]("create a stack and returns its stackId")
    val stackDelete = taskKey[Unit]("delete a stack")
    val stackUpdate = taskKey[String]("update a stack")

    val stackClient = settingKey[AmazonCloudFormationClient]("AWS CloudFormation Client")
  }

}

object CloudFormation extends sbt.Plugin {


  import com.github.tptodorov.sbt.cloudformation.Import.Configurations._
  import com.github.tptodorov.sbt.cloudformation.Import.Keys._

  private lazy val awsCredentialsProvider = new DefaultAWSCredentialsProviderChain()

  lazy val validationSettings = Seq(

    templatesSourceFolder <<= baseDirectory {
      base => base / "src/main/aws"
    },
    templates := {
      val templates = templatesSourceFolder.value ** GlobFilter("*.template")
      templates.get
    },
    awsCredentials := {
      awsCredentialsProvider.getCredentials
    },

    watchSources <++= templates map identity,
    stackValidate <<= (awsCredentials, templates, streams) map {
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

  lazy val defaultSettings = validationSettings ++ Seq(
    stackRegion := System.getenv("AWS_DEFAULT_REGION"),
    stackTemplate <<= templates map {
      files =>
        IO.read(files.headOption.getOrElse(throw new FileNotFoundException("*.template not found in this project")))
    },
    stackName <<= normalizedName,
    stackCapabilities := Seq()
  ) ++ makeOperationConfig(Staging) ++ makeOperationConfig(Production)

  implicit private def parametersToList(params: Parameters): util.Collection[Parameter] = {
    val ps: Iterable[Parameter] = for {
      (k, v) <- params
    } yield {
      val p = new Parameter()
      p.setParameterKey(k)
      p.setParameterValue(v)
      p
    }
    ps.toList
  }

  implicit private def tagsToList(tags: Tags): util.Collection[Tag] = {
    val ps: Iterable[Tag] = for {
      (k, v) <- tags
    } yield {
      val p = new Tag()
      p.setKey(k)
      p.setValue(v)
      p
    }
    ps.toList
  }

  private def fetchStatus(stack: String, cl: AmazonCloudFormationClient): Option[String] = {
    Try {
      val request: DescribeStacksRequest = new DescribeStacksRequest()
      request.setStackName(stack)
      val response = cl.describeStacks(request)
      response.getStacks.toList.headOption.map(stack => stack.getStackStatus)
    }.toOption.flatten
  }

  def makeOperationConfig(config: Configuration) = Seq(
    awsCredentials in config <<= awsCredentials,
    stackTemplate in config <<= stackTemplate,
    stackParams in config := Map(),
    stackTags in config := Map(),
    stackName in config <<= stackName {
      normName =>
        s"$normName-${config.name}"
    },
    stackRegion in config <<= stackRegion,
    stackCapabilities in config <<= stackCapabilities,
    stackClient in config <<= (stackRegion in config, awsCredentials in config) {
      (region, credentials) =>
        if (region == null)
          throw new IllegalArgumentException("stackRegion must be set")

        val client = new AmazonCloudFormationClient(credentials)
        client.setRegion(Region.getRegion(Regions.fromName(region)))
        client
    },
    stackDescribe in config <<= (stackClient in config, stackName in config, streams) map {
      (cl, stack, s) =>

        val request: DescribeStacksRequest = new DescribeStacksRequest()
        request.setStackName(stack)
        val response = cl.describeStacks(request)
        val stacks: List[Stack] = response.getStacks.toList
        stacks.foreach(stack => s.log.debug(s"${stack.toString}"))
        stacks.headOption
    },
    stackStatus in config <<= (stackClient in config, stackName in config, streams) map {
      (cl, stack, s) =>
        fetchStatus(stack, cl)
    },
    stackWait in config <<= (stackClient in config, stackName in config, streams) map {
      (cl, stack, s) =>

        def statuses: Stream[String] = Stream.cons(fetchStatus(stack, cl).orNull, statuses)

        val progressStatuses: Stream[String] = statuses.takeWhile(s => s != null && s.endsWith("_PROGRESS"))

        progressStatuses foreach {
          s =>
            Thread.sleep(10000)
        }

        statuses.headOption
    },
    stackCreate in config <<= (stackClient in config, stackName in config, stackTemplate in config, stackParams in config, stackTags in config, stackCapabilities in config, streams) map {
      (cl, stack, template, params, tags, capabilities, s) =>

        val request = new CreateStackRequest
        request.setStackName(stack)
        request.setTemplateBody(template)
        request.setCapabilities(capabilities)
        request.setParameters(params)
        request.setTags(tags)

        val result = cl.createStack(request)

        s.log.info(s"created stack ${request.getStackName} / ${result.getStackId}")
        result.getStackId
    },
    stackDelete in config <<= (stackClient in config, stackName in config, streams) map {
      (cl, stack, s) =>

        val request = new DeleteStackRequest
        request.setStackName(stack)

        cl.deleteStack(request)

        s.log.info(s"deleting stack ${request.getStackName} ")

    },
    stackUpdate in config <<= (stackClient in config, stackName in config, stackTemplate in config, stackParams in config, stackCapabilities in config, streams) map {
      (cl, stack, template, params, capabilities, s) =>

        val request = new UpdateStackRequest
        request.setStackName(stack)
        request.setTemplateBody(template)
        request.setCapabilities(capabilities)
        request.setParameters(params)

        val result = cl.updateStack(request)

        s.log.info(s"updated stack ${request.getStackName} / ${result.getStackId}")
        result.getStackId
    }
  )


}
