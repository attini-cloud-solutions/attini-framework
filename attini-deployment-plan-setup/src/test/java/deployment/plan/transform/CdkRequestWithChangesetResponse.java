package deployment.plan.transform;

import com.fasterxml.jackson.databind.JsonNode;

public class CdkRequestWithChangesetResponse extends AbstractRequestResponse{

    @Override
    JsonNode request(){
        String request = """
                {
                    "Next": "Step2b1",
                    "Properties": {
                        "Runner": "HelloWorldRunner",
                        "Path": "/project",
                        "ChangeSet": true,
                        "Context": {
                             "Vpc.$": "$.test"
                           },
                        "StackConfiguration": [
                            {
                              "StackName": "CarlStack",
                              "Parameters": {
                        
                              }
                            }
                          ]
                    },
                    "Type": "AttiniCdk"
                }
                """;
        return readValue(request);
    }


    @Override
    JsonNode expectedResponse() {

        String response = """
                {
                   "Step1b1CdkDeploy" : {
                     "Type" : "Task",
                     "Resource" : "arn:aws:states:::lambda:invoke.waitForTaskToken",
                     "Parameters" : {
                       "FunctionName" : "attini-action",
                       "Payload" : {
                         "customData.$" : "$.customData",
                         "deploymentOriginData.$" : "$.deploymentOriginData",
                         "output.$" : "$.output",
                         "dependencies.$" : "$.dependencies",
                         "environment.$" : "$.environment",
                         "stackParameters.$" : "$.stackParameters",
                         "attiniActionType" : "DeployCdk",
                         "deploymentPlanExecutionMetadata" : {
                           "executionArn.$" : "$$.Execution.Id",
                           "executionStartTime.$" : "$$.Execution.StartTime",
                           "sfnToken.$" : "$$.Task.Token",
                           "retryCounter.$" : "$$.State.RetryCount",
                           "stepName.$" : "$$.State.Name"
                         },
                         "Properties" : {
                           "Runner" : "HelloWorldRunner",
                           "Path" : "./project",
                           "ChangeSet" : true,
                           "Context" : {
                             "Vpc.$" : "$.test"
                           },
                           "StackConfiguration" : [ {
                             "StackName" : "CarlStack",
                             "Parameters" : { }
                           } ]
                         }
                       }
                     },
                     "Retry" : [ {
                       "ErrorEquals" : [ "Lambda.TooManyRequestsException" ],
                       "IntervalSeconds" : 2,
                       "MaxAttempts" : 30,
                       "BackoffRate" : 1.2
                     } ],
                     "Next" : "Step2b1",
                     "ResultPath" : "$.output.Step1b1CdkDeploy"
                   },
                   "Step1b1CdkChoice" : {
                     "Type" : "Choice",
                     "Choices" : [ {
                       "Variable" : "$.output.Step1b1.result",
                       "StringEquals" : "change-detected",
                       "Next" : "Step1b1CdkApproval"
                     } ],
                     "Default" : "Step2b1"
                   },
                   "Step1b1CdkApproval" : {
                     "Type" : "Task",
                     "Resource" : "arn:aws:states:::lambda:invoke.waitForTaskToken",
                     "Parameters" : {
                       "FunctionName" : "attini-action",
                       "Payload" : {
                         "customData.$" : "$.customData",
                         "deploymentOriginData.$" : "$.deploymentOriginData",
                         "output.$" : "$.output",
                         "dependencies.$" : "$.dependencies",
                         "attiniActionType" : "ManualApproval",
                         "environment.$" : "$.environment",
                         "stackParameters.$" : "$.stackParameters",
                         "deploymentPlanExecutionMetadata" : {
                           "executionArn.$" : "$$.Execution.Id",
                           "executionStartTime.$" : "$$.Execution.StartTime",
                           "sfnToken.$" : "$$.Task.Token",
                           "retryCounter.$" : "$$.State.RetryCount",
                           "stepName.$" : "$$.State.Name"
                         },
                         "Properties" : {
                           "Runner" : "HelloWorldRunner",
                           "Path" : "./project",
                           "ChangeSet" : true,
                           "Context" : {
                             "Vpc.$" : "$.test"
                           },
                           "StackConfiguration" : [ {
                             "StackName" : "CarlStack",
                             "Parameters" : { }
                           } ]
                         }
                       }
                     },
                     "Retry" : [ {
                       "ErrorEquals" : [ "Lambda.TooManyRequestsException" ],
                       "IntervalSeconds" : 2,
                       "MaxAttempts" : 30,
                       "BackoffRate" : 1.2
                     } ],
                     "Next" : "Step1b1CdkDeploy",
                     "ResultPath" : "$.output.Step1b1CdkApproval"
                   },
                   "Step1b1" : {
                     "Type" : "Task",
                     "Resource" : "arn:aws:states:::lambda:invoke.waitForTaskToken",
                     "Parameters" : {
                       "FunctionName" : "attini-action",
                       "Payload" : {
                         "customData.$" : "$.customData",
                         "deploymentOriginData.$" : "$.deploymentOriginData",
                         "output.$" : "$.output",
                         "dependencies.$" : "$.dependencies",
                         "environment.$" : "$.environment",
                         "stackParameters.$" : "$.stackParameters",
                         "attiniActionType" : "DeployCdkChangeset",
                         "deploymentPlanExecutionMetadata" : {
                           "executionArn.$" : "$$.Execution.Id",
                           "executionStartTime.$" : "$$.Execution.StartTime",
                           "sfnToken.$" : "$$.Task.Token",
                           "retryCounter.$" : "$$.State.RetryCount",
                           "stepName.$" : "$$.State.Name"
                         },
                         "Properties" : {
                           "Runner" : "HelloWorldRunner",
                           "Path" : "./project",
                           "ChangeSet" : true,
                           "Context" : {
                             "Vpc.$" : "$.test"
                           },
                           "StackConfiguration" : [ {
                             "StackName" : "CarlStack",
                             "Parameters" : { }
                           } ]
                         }
                       }
                     },
                     "Retry" : [ {
                       "ErrorEquals" : [ "Lambda.TooManyRequestsException" ],
                       "IntervalSeconds" : 2,
                       "MaxAttempts" : 30,
                       "BackoffRate" : 1.2
                     } ],
                     "Next" : "Step1b1CdkChoice",
                     "ResultPath" : "$.output.Step1b1"
                   }
                 }
                """;

        return readValue(response);
    }

}
