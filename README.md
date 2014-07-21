Sbt-CloudFormation
==================

SBT plugin for validation of CloudFormation templates and operating AWS CloudFormations.

Setup
=====

The plugin by default uses the [default credential provider supplied by AWS SDK](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html).

If you have already setup [AWS CLI](http://aws.amazon.com/cli/), you don't have to do any additional configurations.

Note that AWS_DEFAULT_REGION environment variable must also be set. For setting up AWS CLI, see [the setup instructions](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html).

In your plugins.sbt add:
    
    resolvers += "SBT release"  at "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"
    
    addSbtPlugin("com.github.tptodorov" % "sbt-cloudformation" % "0.0.3")


In your build.sbt add:

    import com.github.tptodorov.sbt.cloudformation.CloudFormation
    import com.github.tptodorov.sbt.cloudformation.Import.Keys._
    import com.github.tptodorov.sbt.cloudformation.Import.Configurations._

    CloudFormation.defaultSettings
    // or any parameters required by your template
    stackParams in Staging := Map("NumberWithRange" -> "2", "StringWithLength" -> "longstring")
    // tag your stack
    stackTags in Staging := Map("env" -> "staging", "app" -> "sample")


By default, there are two configurations: staging and production. For each configuration, there following settings are defined:

 * `stackTemplate` - source of a CloudFormation template. By default, the first `.template` found in `src/main/aws` folder.
 * `stackParams` - map of template parameters  
 * `stackTags` - map of tags assigned to new stacks
 * `stackRegion` - region where the stack is created. By default, `AWS_DEFAULT_REGION` environment is used. 
 * `stackName` - name of the stack. By default, is in the form `<artifactId>-<configuration>`
  
For full list of settings, see [Keys](https://github.com/tptodorov/sbt-cloudformation/blob/master/src/main/scala/com/github/tptodorov/sbt/cloudformation/CloudFormation.scala)

Tasks
=====

 * `stackValidate` - validates CloudFormation *.template files. Supports `~ stackValidate` command as well.
 * `stackCreate`
 * `stackUpdate`
 * `stackDelete`
 * `stackDescribe`
 * `stackStatus`  - returns the current stack status
 * `stackWait` - blocks while the stack is in any of the `PROGRESS` states (see Usage below)

All tasks, except stackWait, return immediately.

Usage Scenarios
=====

 * Template Validation - all `*.template` files in `src/main/aws` are validated during compilation. Continuous validation can be done by: 
   
    `sbt ~stackValidate`

 * Create/Update/Delete a stack and wait for it to complete. 
 
    `sbt " ; staging:stackCreate ; export staging:stackWait "`
    
    `sbt " ; staging:updateCreate ; export staging:stackWait "`
    
    `sbt " ; staging:deleteCreate ; export staging:stackWait "`
    
Sample Project
======

[Sample](https://github.com/tptodorov/sbt-cloudformation/tree/master/sample) has a simple working setup. 

