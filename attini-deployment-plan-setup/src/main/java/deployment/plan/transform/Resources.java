package deployment.plan.transform;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

public class Resources {

    public static Map<String, Object> sqsQueue() {
        return Map.of("Type",
                      "AWS::SQS::Queue",
                      "Properties",
                      Map.of("FifoQueue",
                             "True",
                             "KmsDataKeyReusePeriodSeconds",
                             "86400",
                             "ReceiveMessageWaitTimeSeconds",
                             "20",
                             "VisibilityTimeout",
                             "15",
                             "MessageRetentionPeriod",
                             "43200"));
    }

    public static Map<String, Object> runner(String region, String account) {
        return Map.of("Type",
                      "Attini::Deploy::Runner",
                      "Properties",
                      Map.of("EcsCluster",
                             "attini-default",
                             "TaskDefinitionArn",
                             getDefaultTaskDefinitionArn(region, account)));
    }

    public static String getDefaultTaskDefinitionArn(String region, String account) {
        return "arn:aws:ecs:%s:%s:task-definition/attini-default-runner".formatted(
                region,
                account);
    }


    public static Map<String, Object> securityGroups(String runnerName) {
        return Map.of("Type",
                      "AWS::EC2::SecurityGroup",
                      "Properties",
                      Map.of("GroupDescription",
                             new AbstractMap.SimpleEntry<>("Fn::Sub",
                                                           "${AWS::StackName} [ " + runnerName + " ] Security group")));
    }


    public static Map<String, Object> logGroup() {
        return Map.of("Type",
                      "AWS::Logs::LogGroup",
                      "Properties",
                      Map.of("RetentionInDays", 90));
    }

    public static Map<String, Object> taskDefinition(CfnString image, CfnString roleArn, String logGroup) {

        Map<String, Object> logConfiguration = Map.of("LogDriver",
                                                      "awslogs",
                                                      "Options",
                                                      Map.of("awslogs-group",
                                                             Map.of("Ref", logGroup),
                                                             "awslogs-region",
                                                             Map.of("Ref", "AWS::Region"),
                                                             "awslogs-stream-prefix", "logs"));
        List<Map<String, Object>> containers = List.of(Map.of("Name",
                                                              "Container",
                                                              "Image",
                                                              image,
                                                              "LogConfiguration",
                                                              logConfiguration));

        return Map.of("Type", "AWS::ECS::TaskDefinition",
                      "Properties", Map.of("ContainerDefinitions",
                                           containers,
                                           "Cpu", 512,
                                           "ExecutionRoleArn", roleArn,
                                           "TaskRoleArn", roleArn,
                                           "Memory", 3072,
                                           "NetworkMode", "awsvpc",
                                           "RequiresCompatibilities", List.of("FARGATE")));
    }

    public static Map<String, Object> ec2taskDefinition(CfnString image, CfnString roleArn, String logGroup) {

        Map<String, Object> logConfiguration = Map.of("LogDriver",
                                                      "awslogs",
                                                      "Options",
                                                      Map.of("awslogs-group",
                                                             Map.of("Ref", logGroup),
                                                             "awslogs-region",
                                                             Map.of("Ref", "AWS::Region"),
                                                             "awslogs-stream-prefix", "logs"));
        List<Map<String, Object>> containers = List.of(Map.of("Name",
                                                              "Container",
                                                              "Image",
                                                              image,
                                                              "Privileged", true,
                                                              "LogConfiguration",
                                                              logConfiguration));

        return Map.of("Type", "AWS::ECS::TaskDefinition",
                      "Properties", Map.of("ContainerDefinitions",
                                           containers,
                                           "Cpu", 512,
                                           "ExecutionRoleArn", roleArn,
                                           "TaskRoleArn", roleArn,
                                           "Memory", 3072,
                                           "NetworkMode", "bridge",
                                           "RequiresCompatibilities", List.of("EC2")));
    }
}
