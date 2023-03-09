package deployment.plan.transform;

import com.fasterxml.jackson.databind.JsonNode;

public class MergeOutputRequestResponse extends AbstractRequestResponse{
    @Override
    JsonNode request() {
        String request = """
                {
                    "Next": "GetConfigFiles",
                    "Type": "AttiniMergeOutput"
                }
                """;

        return readValue(request);
    }

    @Override
    JsonNode expectedResponse() {
       String response  = """
               {
                 "Type" : "Task",
                 "Resource" : "arn:aws:states:::lambda:invoke",
                 "OutputPath" : "$.Payload",
                 "Parameters" : {
                   "FunctionName" : "attini-action",
                   "Payload" : {
                     "attiniActionType" : "AttiniMergeOutput",
                     "InputsToMerge.$" : "$"
                   }
                 },
                 "Retry" : [ {
                   "ErrorEquals" : [ "NotFirstInQueue" ],
                   "IntervalSeconds" : 3,
                   "MaxAttempts" : 20,
                   "BackoffRate" : 1.5
                 }, {
                   "ErrorEquals" : [ "Lambda.TooManyRequestsException" ],
                   "IntervalSeconds" : 2,
                   "MaxAttempts" : 30,
                   "BackoffRate" : 1.2
                 } ],
                 "Next" : "GetConfigFiles"
               }
               """;

       return readValue(response);
    }
}
