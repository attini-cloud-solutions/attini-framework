package deployment.plan.transform;

import com.fasterxml.jackson.databind.JsonNode;

public class SamRequestResponse extends AbstractRequestResponse {

    @Override
    JsonNode request() {
        String request = """
                {
                   "Next":"Step2b1",
                   "Properties":{
                      "StackName":"sam-lambda",
                      "Project":{
                         "Path":"node-sam-app-image"
                      },
                      "Parameters":{
                         "TableName.$":"$.output.DeployDatabase.TableName"
                      }
                   },
                   "Type":"AttiniSam"
                }
                """;
        return readValue(request);
    }


    @Override
    JsonNode expectedResponse() {

        String response = """
                {
                   "Step1b1PackageSam":{
                      "Type":"Task",
                      "Resource":"arn:aws:states:::lambda:invoke.waitForTaskToken",
                      "Parameters":{
                         "FunctionName":"attini-action",
                         "Payload":{
                            "customData.$":"$.customData",
                            "deploymentOriginData.$":"$.deploymentOriginData",
                            "output.$":"$.output",
                            "dependencies.$":"$.dependencies",
                            "environment.$":"$.environment",
                            "stackParameters.$":"$.stackParameters",
                            "attiniActionType":"PackageSam",
                            "deploymentPlanExecutionMetadata":{
                               "executionArn.$":"$$.Execution.Id",
                               "executionStartTime.$":"$$.Execution.StartTime",
                               "sfnToken.$":"$$.Task.Token",
                               "retryCounter.$":"$$.State.RetryCount",
                               "stepName.$":"$$.State.Name"
                            },
                            "Properties":{
                               "StackName":"sam-lambda",
                                "Project": {
                                           "Path": "node-sam-app-image"
                                },
                               "Parameters":{
                                  "TableName.$":"$.output.DeployDatabase.TableName"
                               },
                               "Runner":"DefaultRunner"
                            }
                         }
                      },
                      "Retry":[
                         {
                            "ErrorEquals":[
                               "Lambda.TooManyRequestsException"
                            ],
                            "IntervalSeconds":2,
                            "MaxAttempts":30,
                            "BackoffRate":1.2
                         }
                      ],
                      "Next":"c0e8021dffea9af8e9c48265f5e646af",
                      "ResultPath":"$.output.Step1b1PackageSam"
                   },
                   "Step1b1":{
                      "Type":"Task",
                      "Resource":"arn:aws:states:::lambda:invoke.waitForTaskToken",
                      "Parameters":{
                         "FunctionName":"attini-action",
                         "Payload":{
                            "customData.$":"$.customData",
                            "deploymentOriginData.$":"$.deploymentOriginData",
                            "output.$":"$.output",
                            "dependencies.$":"$.dependencies",
                            "attiniActionType":"DeployCfn",
                            "environment.$":"$.environment",
                            "stackParameters.$":"$.stackParameters",
                            "deploymentPlanExecutionMetadata":{
                               "executionArn.$":"$$.Execution.Id",
                               "executionStartTime.$":"$$.Execution.StartTime",
                               "sfnToken.$":"$$.Task.Token",
                               "retryCounter.$":"$$.State.RetryCount",
                               "stepName.$":"$$.State.Name"
                            },
                            "Properties":{
                               "StackName":"sam-lambda",
                               "Project": {
                                           "Path": "node-sam-app-image"
                                },
                               "Parameters":{
                                  "TableName.$":"$.output.DeployDatabase.TableName"
                               },
                               "Template.$" : "$.output.Step1b1PackageSam.result"
                            }
                         }
                      },
                      "Retry":[
                         {
                            "ErrorEquals":[
                               "IsExecuting"
                            ],
                            "IntervalSeconds":3,
                            "MaxAttempts":650,
                            "BackoffRate":1.01
                         },
                         {
                            "ErrorEquals":[
                               "RollBackCompleteState"
                            ],
                            "IntervalSeconds":4,
                            "MaxAttempts":1,
                            "BackoffRate":1
                         },
                         {
                            "ErrorEquals":[
                               "Lambda.TooManyRequestsException"
                            ],
                            "IntervalSeconds":2,
                            "MaxAttempts":30,
                            "BackoffRate":1.2
                         }
                      ],
                      "Next":"Step2b1",
                      "ResultPath":"$.output.Step1b1"
                   }
                }
                """;

        return readValue(response);
    }

}
