# Attini Framework

The Attini Framework is a deployment framework for Infrastructure as Code (IaC)
on AWS, find more information about Attini on our [website](https://attini.io/) and in
our [documentation](https://docs.attini.io/).

If you just want to use Attini, we recommend using the prebuilt framework
and CLI, find more information in the [getting started](https://attini.io/guides/get-started/) guide.



## Compile and deploy the framework

Attini is an AWS SAM Application written in Java with Quarkus. 

To deploy and test the framework, you will need AWS CLI Credentials and AWS SAM CLI.

Before you start, open the `Makefile` and configure it appropriately.


#### Build:

```bash
make
```

#### Build native images (takes roughly 10 to 15 min):

```bash
make build-native
```

#### Build and deploy Attini to your AWS account:

 ```bash
make build-and-deploy
```

#### If you have built everything already and just want to deploy:

 ```bash
make deploy
```

## Working with and individual lambda

If you have built and deployed the framework, you can write changes and deploy 
individual Lambda functions with the following commands.

 ```bash
cd attini-action # or whatever function you are working with
```

#### Build:
 ```bash
make build
```

#### Deploy:
 ```bash
make deploy
```

#### Build & Deploy:
 ```bash
make
```

## Project structure


#### template.yaml

Here you will find all Attini CloudFormation.

#### attini-domain
Java classes that are shared between the Lambda functions.

#### auto-update
A Python Lambda function that can automatically update the Attini framework on a schedule that you configure via attini-setup.

#### attini-deploy-origin 
This is Lambda is responsible for initiating deployments, on a high level it will:

1. Be triggerd by the attini distribution when it's uploaded to the ``attini-deployment-origin`` S3 bucket.
2. It will download the distribution, unpack it, and put all the files in the ``attini-artifact-store`` S3 bucket.
3. Find the initDeployConfig in attini-config and deploy the deployment plan (init deploy stack).

#### attini-deployment-plan-setup
CloudFormation macro and custom resource Lambda function that will transform the init deployment template so 
that all Attini resources (Attini::Deploy::DeploymentPlan, Attini::Deploy::Runner) are 
converted into valid CloudFormation.

#### attini-step-guard
Lambda function that reacts to different events in the framework. For example, 
it will trigger the deployment plan when the init-deploy is ready, or 
respond to the deployment plan step function when a CloudFormation stack (AttiniCfn or AttiniSam)
is finished.

#### attini-action
Lambda function that performs all deployment plan actions, for example, deploying CloudFormation
stacks, starting Attini Runners (ECS tasks) etc. 


## CI/CD

This repository does not have its own CI/CD configured. This is
because Lambda functions in a SAM Application need to be packaged, signed, 
and uploaded to S3 buckets in every AWS region that the Attini 
should work in.

These S3 buckets also need to be made available to the AWS accounts that
want to use Attini in. 

Because of this, the deployment process requires
a lot of AWS Access. Therefore, we keep the CI/CD process for the 
official Attini releases private. 

But what it essentially does is: 
1. Sign the Lambda zip files with the Attini signing profile.  
2. Run integration tests
3. Upload the CloudFormation templates and the Lambda zip files
   to public S3 buckets in every AWS Region. 