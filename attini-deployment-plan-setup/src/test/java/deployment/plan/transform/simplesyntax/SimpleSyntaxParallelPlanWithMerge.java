package deployment.plan.transform.simplesyntax;

public class SimpleSyntaxParallelPlanWithMerge {

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
                ExecutionRoleArn: !GetAtt DeploymentPlanExecutionRole.Arn
                Parameters:
                  SsmParameterKey.$: $.output.HelloWorldParameter.SsmParameterKey
            - Name: my-branches
              Type: Parallel
              Branches:
                -
                  - Name: HelloWorldLambda2
                    Type: AttiniCfn
                    Properties:
                      StackName: hello-world-lambda2
                      Template: /lambda.yaml
                      ExecutionRoleArn: DeploymentPlanExecutionRole.Arn
                      Parameters:
                        SsmParameterKey.$: $.output.HelloWorldParameter.SsmParameterKey #outputs from stacks in previous AttiniCfn steps can be read from the payload under output.<stepName>.<outputName> using standard StepFunction syntax
                      ConfigFile: /config/${ConfigEnv}-lambda-config.yaml
                  - Name: HelloWorldLambda3
                    Type: AttiniCfn
                    Properties:
                      StackName: ${AttiniEnvironmentName}-hello-world-lambda3
                      Template: /lambda.yaml
                      ExecutionRoleArn: !GetAtt DeploymentPlanExecutionRole.Arn
                      Parameters:
                        FunctionName: ${AttiniEnvironmentName}-hello-world-lambda3
                        SsmParameterKey.$: $.output.HelloWorldParameter.SsmParameterKey #outputs from stacks in previous AttiniCfn steps can be read from the payload under output.<stepName>.<outputName> using standard StepFunction syntax
                      ConfigFile: /config/${ConfigEnv}-lambda-config.yaml
                -
                  - Name: HelloWorldLambda4
                    Type: AttiniCfn
                    Properties:
                      StackName: ${AttiniEnvironmentName}-hello-world-lambda4
                      Template: /lambda.yaml
                      ExecutionRoleArn: !GetAtt DeploymentPlanExecutionRole.Arn
                      Parameters:
                        FunctionName: ${AttiniEnvironmentName}-hello-world-lambda4
                        SsmParameterKey.$: $.output.HelloWorldParameter.SsmParameterKey #outputs from stacks in previous AttiniCfn steps can be read from the payload under output.<stepName>.<outputName> using standard StepFunction syntax
                      ConfigFile: /config/${ConfigEnv}-lambda-config.yaml
                  - Name: HelloWorldLambda5
                    Type: AttiniCfn
                    Properties:
                      StackName: ${AttiniEnvironmentName}-hello-world-lambda5
                      Template: /lambda.yaml
                      ExecutionRoleArn: !GetAtt DeploymentPlanExecutionRole.Arn
                      Parameters:
                        FunctionName: ${AttiniEnvironmentName}-hello-world-lambda5
                        SsmParameterKey.$: $.output.HelloWorldParameter.SsmParameterKey #outputs from stacks in previous AttiniCfn steps can be read from the payload under output.<stepName>.<outputName> using standard StepFunction syntax
                      ConfigFile: /config/${ConfigEnv}-lambda-config.yaml
            - Name: MergeOutput
              Type: AttiniMergeOutput
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
                Next: "my-branches"
              my-branches:
                Type: "Parallel"
                Branches:
                - StartAt: "HelloWorldLambda2"
                  States:
                    HelloWorldLambda2:
                      Type: "AttiniCfn"
                      Properties:
                        StackName: "hello-world-lambda2"
                        Template: "/lambda.yaml"
                        ExecutionRoleArn: "DeploymentPlanExecutionRole.Arn"
                        Parameters:
                          SsmParameterKey.$: "$.output.HelloWorldParameter.SsmParameterKey"
                        ConfigFile: "/config/${ConfigEnv}-lambda-config.yaml"
                      Next: "HelloWorldLambda3"
                    HelloWorldLambda3:
                      Type: "AttiniCfn"
                      Properties:
                        StackName: "${AttiniEnvironmentName}-hello-world-lambda3"
                        Template: "/lambda.yaml"
                        ExecutionRoleArn: "DeploymentPlanExecutionRole.Arn"
                        Parameters:
                          FunctionName: "${AttiniEnvironmentName}-hello-world-lambda3"
                          SsmParameterKey.$: "$.output.HelloWorldParameter.SsmParameterKey"
                        ConfigFile: "/config/${ConfigEnv}-lambda-config.yaml"
                      End: true
                - StartAt: "HelloWorldLambda4"
                  States:
                    HelloWorldLambda4:
                      Type: "AttiniCfn"
                      Properties:
                        StackName: "${AttiniEnvironmentName}-hello-world-lambda4"
                        Template: "/lambda.yaml"
                        ExecutionRoleArn: "DeploymentPlanExecutionRole.Arn"
                        Parameters:
                          FunctionName: "${AttiniEnvironmentName}-hello-world-lambda4"
                          SsmParameterKey.$: "$.output.HelloWorldParameter.SsmParameterKey"
                        ConfigFile: "/config/${ConfigEnv}-lambda-config.yaml"
                      Next: "HelloWorldLambda5"
                    HelloWorldLambda5:
                      Type: "AttiniCfn"
                      Properties:
                        StackName: "${AttiniEnvironmentName}-hello-world-lambda5"
                        Template: "/lambda.yaml"
                        ExecutionRoleArn: "DeploymentPlanExecutionRole.Arn"
                        Parameters:
                          FunctionName: "${AttiniEnvironmentName}-hello-world-lambda5"
                          SsmParameterKey.$: "$.output.HelloWorldParameter.SsmParameterKey"
                        ConfigFile: "/config/${ConfigEnv}-lambda-config.yaml"
                      End: true
                Next: MergeOutput
              MergeOutput:
                Type: AttiniMergeOutput
                End: true
            """;

}
