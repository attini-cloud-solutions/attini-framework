package deployment.plan.transform.simplesyntax;

public class SimpleSyntaxChoicePlan {

    public final static String INPUT = """
                        
            - Name: HelloWorldParameter
              Type: AttiniCfn
              Properties:
                StackName: my-hello-world-parameter
                Template: /ssm-parameter.yaml
                ExecutionRoleArn: !GetAtt DeploymentPlanExecutionRole.Arn
                Parameters:
                  ParameterValue: ParameterValue
            - Name: IsDev?
              Type: Choice
              Condition:
                And:
                - Variable: $.foo.bar
                  StringEquals: dev
                - Variable: $.foo.hej
                  StringEquals: svej
              IsTrue:
                - Name: LoadTest
                  Type: AttiniRunnerJob
                  Properties:
                    Commands:
                      - echo “Fuck yeaa”
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
                Next: "IsDev?"
              IsDev?:
                Type: "Choice"
                Choices:
                  - And:
                    - Variable: $.foo.bar
                      StringEquals: dev
                    - Variable: $.foo.hej
                      StringEquals: svej
                    Next: LoadTest
                Default: HelloWorldLambda
              LoadTest:
                Type: AttiniRunnerJob
                Properties:
                  Commands:
                    - echo “Fuck yeaa”
                Next: HelloWorldLambda
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
