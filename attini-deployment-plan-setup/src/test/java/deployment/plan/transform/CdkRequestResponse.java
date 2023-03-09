package deployment.plan.transform;

import com.fasterxml.jackson.databind.JsonNode;

public class CdkRequestResponse extends AbstractRequestResponse{

    @Override
    JsonNode request(){
        String request = """
                {
                    "Next": "Step2b1",
                    "Properties": {
                        "Runner": "HelloWorldRunner",
                        "Path": "/project",
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
                  "Type" : "Task",
                  "Resource" : "arn:aws:states:::lambda:invoke.waitForTaskToken",
                  "Parameters" : {
                    "FunctionName" : "attini-action",
                    "Payload" : {
                      "customData.$" : "$.customData",
                      "deploymentOriginData.$" : "$.deploymentOriginData",
                      "output.$" : "$.output",
                      "dependencies.$" : "$.dependencies",
                      "attiniActionType" : "DeployCdk",
                      "environment.$": "$.environment",
                      "stackParameters.$": "$.stackParameters",
                      "deploymentPlanExecutionMetadata" : {
                        "executionArn.$" : "$$.Execution.Id",
                        "executionStartTime.$" : "$$.Execution.StartTime",
                        "sfnToken.$" : "$$.Task.Token",
                        "retryCounter.$" : "$$.State.RetryCount",
                        "stepName.$" : "$$.State.Name"
                      },
                      "Properties" : {
                        "Runner": "HelloWorldRunner",
                        "Path": "./project",
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
                }
                """;

        return readValue(response);
    }

}
