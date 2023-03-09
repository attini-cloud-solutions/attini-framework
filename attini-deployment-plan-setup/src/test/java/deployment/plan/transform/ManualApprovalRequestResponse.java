package deployment.plan.transform;

import com.fasterxml.jackson.databind.JsonNode;

public class ManualApprovalRequestResponse extends AbstractRequestResponse{
    @Override
    JsonNode request() {
        String request = """
                {
                    "Next": "DeployServices",
                    "Type": "AttiniManualApproval"
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
                      "attiniActionType" : "ManualApproval",
                      "environment.$": "$.environment",
                      "stackParameters.$": "$.stackParameters",
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
                  "Next" : "DeployServices",
                  "ResultPath" : "$.output.GetSocksDbArn"
                }
                """;

        return readValue(response);
    }
}
