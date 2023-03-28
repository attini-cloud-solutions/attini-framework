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
                        "Diff": {"Enabled": true},
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
                           "Diff" : {"Enabled": true},
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
                     "ResultPath" : "$.output.Step1b1"
                   },
                   "Step1b1ApproveChanges?" : {
                     "Type" : "Choice",
                     "Choices" : [ {
                       "Variable" : "$.output.Step1b1CdkDiff.result",
                       "StringEquals" : "change-detected",
                       "Next" : "Step1b1ApproveChanges"
                     } ],
                     "Default": "c0e8021dffea9af8e9c48265f5e646af"
                   },
                   "Step1b1ApproveChanges" : {
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
                         }
                       }
                     },
                     "Retry" : [ {
                       "ErrorEquals" : [ "Lambda.TooManyRequestsException" ],
                       "IntervalSeconds" : 2,
                       "MaxAttempts" : 30,
                       "BackoffRate" : 1.2
                     } ],
                     "Next" : "c0e8021dffea9af8e9c48265f5e646af",
                     "ResultPath" : "$.output.Step1b1ApproveChanges"
                   },
                   "Step1b1CdkDiff" : {
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
                           "Diff" : {"Enabled": true},
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
                     "Next" : "Step1b1ApproveChanges?",
                     "ResultPath" : "$.output.Step1b1CdkDiff"
                   }
                 }
                """;

        return readValue(response);
    }

}
