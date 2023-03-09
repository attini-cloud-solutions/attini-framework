package deployment.plan.transform;

import com.fasterxml.jackson.databind.JsonNode;

public class LambdaInvokeRequestResponse extends AbstractRequestResponse{

    @Override
    JsonNode request(){
        String request = """
                {
                    "Next": "FixOutput",
                    "Parameters": {
                        "FunctionName.$": "$.output.DeployCustomerService.LambdaName"
                    },
                    "Type": "AttiniLambdaInvoke"
                }
                """;
        return readValue(request);
    }

    @Override
    JsonNode expectedResponse() {

        String response = """
                {
                  "Type" : "Task",
                  "Resource" : "arn:aws:states:::lambda:invoke",
                  "InputPath" : "$",
                  "ResultPath" : "$.output.InvokeMyLambda",
                  "OutputPath" : "$",
                  "ResultSelector" : {
                    "result.$" : "$.Payload"
                  },
                  "Next" : "FixOutput",
                  "Parameters" : {
                    "FunctionName.$" : "$.output.DeployCustomerService.LambdaName",
                    "Payload.$" : "$"
                  }
                }
                """;

        return readValue(response);
    }


}
