package deployment.plan.transform;

import com.fasterxml.jackson.databind.JsonNode;

public class MapInputOutput extends AbstractRequestResponse {

    @Override
    JsonNode request() {
        String request = """
                {
                    "ItemsPath": "$.ServiceConfigFiles",
                    "Iterator": {
                        "StartAt": "DeployServicesAsMap",
                        "States": {
                            "DeployServicesAsMap": {
                                "End": true,
                                "Properties": {
                                    "Variables": {
                                        "MyStackPrefix": "Yolo"
                                    }
                                },
                                "Type": "AttiniCfn"
                            }
                        }
                    },
                    "MaxConcurrency": 0,
                    "Next": "MergeMapOutput",
                    "Type": "AttiniMap"
                }
                """;

        return readValue(request);
    }

    @Override
    JsonNode expectedResponse() {
       String response = """
               {
                 "Type" : "Task",
                 "Resource" : "arn:aws:states:::lambda:invoke.waitForTaskToken",
                 "Parameters" : {
                   "FunctionName" : "attini-action",
                   "Payload" : {
                     "customData.$" : "$.customData",
                     "deploymentOriginData.$" : "$.deploymentOriginData",
                     "output.$" : "$.output",
                     "dependencies.$" : "$.dependencies",
                     "environment.$": "$.environment",
                     "stackParameters.$": "$.stackParameters",
                     "Properties" : {
                       "ConfigFile.$" : "$.ConfigFile"
                     },
                     "attiniActionType" : "DeployCfn",
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
                   "ErrorEquals" : [ "IsExecuting" ],
                   "IntervalSeconds" : 3,
                   "MaxAttempts" : 650,
                   "BackoffRate" : 1.01
                 }, {
                   "ErrorEquals" : [ "RollBackCompleteState" ],
                   "IntervalSeconds" : 4,
                   "MaxAttempts" : 1,
                   "BackoffRate" : 1
                 }, {
                   "ErrorEquals" : [ "Lambda.TooManyRequestsException" ],
                   "IntervalSeconds" : 2,
                   "MaxAttempts" : 30,
                   "BackoffRate" : 1.2
                 } ],
                 "Next" : "MergeMapOutput",
                 "ItemsPath" : "$.ServiceConfigFiles",
                 "Iterator" : {
                   "StartAt" : "DeployServicesAsMap",
                   "States" : {
                     "DeployServicesAsMap" : {
                       "End" : true,
                       "Properties" : {
                         "Variables" : {
                           "MyStackPrefix" : "Yolo"
                         }
                       },
                       "Type" : "AttiniCfn"
                     }
                   }
                 },
                 "MaxConcurrency" : 0,
                 "ResultPath" : "$.output.DeploySocksDB"
               }
               """;

       return readValue(response);
    }
}
