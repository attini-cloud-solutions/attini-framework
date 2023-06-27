package deployment.plan.transform.simplesyntax;

public class SimpleSyntaxPlan {

    public final static String INPUT = """
                        
            - Name: HelloWorldParameter
              Type: AttiniCfn
              Properties:
                StackName: my-hello-world-parameter
                Template: /ssm-parameter.yaml
                ExecutionRoleArn: !GetAtt DeploymentPlanExecutionRole.Arn
                Parameters:
                  ParameterValue: ParameterValue
            - Name: HelloWorldLambda
              Type: AttiniCfn
              Properties:
                StackName: my-hello-world-lambda
                Template: /lambda.yaml
                ExecutionRoleArn: DeploymentPlanExecutionRole.Arn
                Parameters:
                  SsmParameterKey.$: $.output.HelloWorldParameter.SsmParameterKey
                        
            """;


    public final static String EXPECTED_RESULT = """
            
            StartAt: "HelloWorldParameter"
            States:
              HelloWorldParameter:
                Type: "AttiniCfn"
                Properties:
                  StackName: "my-hello-world-parameter"
                  Template: "/ssm-parameter.yaml"
                  ExecutionRoleArn: "DeploymentPlanExecutionRole.Arn"
                  Parameters:
                    ParameterValue: "ParameterValue"
                Next: "HelloWorldLambda"
              HelloWorldLambda:
                Type: "AttiniCfn"
                Properties:
                  StackName: "my-hello-world-lambda"
                  Template: "/lambda.yaml"
                  ExecutionRoleArn: "DeploymentPlanExecutionRole.Arn"
                  Parameters:
                    SsmParameterKey.$: "$.output.HelloWorldParameter.SsmParameterKey"
                End: true
            """;

}
