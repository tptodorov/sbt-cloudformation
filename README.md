Sbt-CloudFormation
==================

[ ![Codeship Status for tptodorov/sbt-cloudformation](https://codeship.com/projects/372faba0-ebf9-0132-15a4-1a6982ed746d/status?branch=master)](https://codeship.com/projects/83578)

SBT plugin for validation of CloudFormation templates and operating AWS CloudFormations.

Setup
=====

The plugin by default uses the [default credential provider supplied by AWS SDK](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html).

If you have already setup [AWS CLI](http://aws.amazon.com/cli/), you don't have to do any additional configurations.

Note that `AWS_DEFAULT_REGION` environment variable must also be set. For setting up AWS CLI, see [the setup instructions](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html).

In your plugins.sbt add:
    
    resolvers += "SBT release"  at "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"
    
    addSbtPlugin("com.github.tptodorov" % "sbt-cloudformation" % "0.5.0")

In your build.sbt add:

    // or any parameters required by your template
    stackParams in Staging := Map("NumberWithRange" -> "2", "StringWithLength" -> "longstring")
    // tag your stack
    stackTags in Staging := Map("env" -> "staging", "app" -> "sample")


By default, there are two configurations: staging and production. For each configuration, the following settings are defined and used for task execution:

 * `stackTemplate` - source of a CloudFormation template. By default, the first `.template` found in `src/main/aws` folder.
 * `stackParams` - map of template parameters  
 * `stackTags` - map of tags assigned to new stacks
 * `stackRegion` - region where the stack is created. By default, `AWS_DEFAULT_REGION` environment is used. 
 * `stackName` - name of the stack. By default, is in the form `<artifactId>-<configuration>`
  
For full list of settings, see [Keys](https://github.com/tptodorov/sbt-cloudformation/blob/master/src/main/scala/com/github/tptodorov/sbt/cloudformation/CloudFormation.scala)

Tasks
=====

 * `stackValidate` validates CloudFormation *.template files. Supports `~ stackValidate` command as well.
 * `stackCreate` creates a stack. **Does not block until create is complete**
 * `stackUpdate` updates a stack.
 * `stackDelete` deletes a stack.
 * `stackDescribe` describes a stack.
 * `stackStatus`  returns the current stack status
 * `stackWait` blocks while the stack is in any of the `PROGRESS` states (see Usage below)

All tasks, except stackWait, return immediately.

Usage Scenarios
=====

 * Template Validation - all `*.template` files in `src/main/aws` are validated during compilation. Continuous validation while you are developing your templates can be done by: 
   
    `sbt ~stackValidate`

    You can also specify where your templates are:
    
    `templatesSourceFolder <<= baseDirectory { _ / "src/main/resources" }`
    
    Most of the time, it is useful to add the template validation as part of the compilation process:
    
    `compile in Compile <<= (compile in Compile) dependsOn stackValidate`

 * Create/Update/Delete a stack and wait for it to complete. 
 
    `sbt " ; staging:stackCreate ; export staging:stackWait "`
    
    `sbt " ; staging:stackUpdate ; export staging:stackWait "`
    
    `sbt " ; staging:stackDelete ; export staging:stackWait "`
    
Sample Project
======

[Sample](https://github.com/tptodorov/sbt-cloudformation/tree/master/sample) has a simple working setup. 

