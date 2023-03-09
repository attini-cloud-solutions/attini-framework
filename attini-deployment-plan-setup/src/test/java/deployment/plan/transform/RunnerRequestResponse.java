package deployment.plan.transform;

import com.fasterxml.jackson.databind.JsonNode;

public class RunnerRequestResponse extends AbstractRequestResponse{

    @Override
    JsonNode request(){
        String request = """
                {
                    "Next": "Step2b1",
                    "Properties": {
                        "Commands": [
                            "echo Oscar",
                            "yum install nc",
                            "echo '#2ca02c gunna'",
                            "echo gonna"
                        ],
                        "Runner": "HelloWorldRunner"
                    },
                    "Type": "AttiniRunnerJob"
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
                      "attiniActionType" : "ExecuteRunner",
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
                        "Commands" : [ "echo Oscar", "yum install nc", "echo '#2ca02c gunna'", "echo gonna" ],
                        "Runner" : "HelloWorldRunner"
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
