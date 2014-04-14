sbt-cloudformation
==================

SBT plugin for working with AWS CloudFormation

Tasks
=====

 * validate - validates CloudFormation *.template files. Supports `~ validate` command as well.


Configurations
==============

The plugin by default uses the [default credential provider supplied by AWS SDK](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html)

	
	AWS credentials provider chain that looks for credentials in this order:
	Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY
	Java System Properties - aws.accessKeyId and aws.secretKey
	Instance profile credentials delivered through the Amazon EC2 metadata service


Plugin settings:

 * templatesSourceFolder - location of CloudFormation  templates (by default src/main/aws)
 
 * 
 
 
In your build.sbt add:

	import sbtcloudformation.CloudFormationPlugin._
	
	defaultCloudFormationSettings

