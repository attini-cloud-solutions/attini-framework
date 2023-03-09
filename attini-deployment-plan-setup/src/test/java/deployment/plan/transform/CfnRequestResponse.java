package deployment.plan.transform;

import com.fasterxml.jackson.databind.JsonNode;

public class CfnRequestResponse extends AbstractRequestResponse{

    @Override
    JsonNode request(){
        String request = """
                {
                    "Next": "DeploySocksInventoryService",
                    "Properties": {
                        "Action": "Deploy",
                        "ConfigFile": "/${ConfigFolder}/config.json",
                        "EnableTerminationProtection": "true",
                        "Parameters": {
                            "Ram": 512
                        },
                        "StackName": "SocksInventoryDB",
                        "Template": "/socks-inventory/inventory-db.yaml",
                        "Variables": {
                            "ConfigSubDir": "subdir",
                            "ParamKey": "paramTag"
                        }
                    },
                    "Type": "AttiniCfn"
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
                      "attiniActionType" : "DeployCfn",
                      "deploymentPlanExecutionMetadata" : {
                        "executionArn.$" : "$$.Execution.Id",
                        "executionStartTime.$" : "$$.Execution.StartTime",
                        "sfnToken.$" : "$$.Task.Token",
                        "retryCounter.$" : "$$.State.RetryCount",
                        "stepName.$" : "$$.State.Name"
                      },
                      "Properties" : {
                        "Variables" : {
                          "ConfigSubDir" : "subdir",
                          "ParamKey" : "paramTag"
                        },
                        "Action" : "Deploy",
                        "Parameters" : {
                          "Ram" : 512
                        },
                        "ConfigFile" : "/${ConfigFolder}/config.json",
                        "EnableTerminationProtection" : "true",
                        "StackName" : "SocksInventoryDB",
                        "Template" : "/socks-inventory/inventory-db.yaml"
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
                  "Next" : "DeploySocksInventoryService",
                  "ResultPath" : "$.output.DeploySocksDB"
                }
                """;

        return readValue(response);
    }
}
