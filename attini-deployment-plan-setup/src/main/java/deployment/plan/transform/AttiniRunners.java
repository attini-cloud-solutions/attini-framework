package deployment.plan.transform;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Vpc;

public class AttiniRunners {

    private final Map<String, Runner> runners;
    private final Map<String, Map<String, Object>> securityGroups = new HashMap<>();

    private final Map<String, Map<String, Object>> taskDefinitions = new HashMap<>();

    private final Map<String, Map<String, Object>> logGroups = new HashMap<>();

    public Map<String, Map<String, Object>> getSecurityGroups() {
        return securityGroups.entrySet()
                             .stream()
                             .collect(Collectors.toMap(entry -> AttiniRunners.getSecurityGroupName(entry.getKey()),
                                                       Map.Entry::getValue));
    }

    public Map<String, Map<String, Object>> getTaskDefinitions() {
        return taskDefinitions;
    }

    public Map<String, Map<String, Object>> getLogGroups() {
        return logGroups;
    }

    public AttiniRunners(Map<String, Map<String, Object>> resources,
                         Ec2Client ec2Client,
                         String region,
                         String accountId,
                         String defaultRunnerImage) {
        ObjectMapper objectMapper = new ObjectMapper();


        this.runners = resources.entrySet()
                                .stream()
                                .filter(entry -> "Attini::Deploy::Runner".equals(entry.getValue().get("Type")))
                                .map(entry -> {
                                    JsonNode jsonNode = objectMapper.valueToTree(entry.getValue()).path("Properties");
                                    JsonNode installationCommandsNode = jsonNode.path("Startup").path("Commands");
                                    if (!installationCommandsNode.isMissingNode() && !installationCommandsNode.isArray()) {
                                        throw new IllegalArgumentException(
                                                "Illegal format for installations commands for runner" + entry.getKey() + ". Installation commands should be a list");
                                    }
                                    List<CfnString> commands = StreamSupport.stream(installationCommandsNode.spliterator(),
                                                                                    false)
                                                                            .map(CfnString::create)
                                                                            .collect(Collectors.toList());
                                    CfnString taskDefinitionArn = jsonNode.path("TaskDefinitionArn")
                                                                          .isMissingNode() ? CfnString.create(Resources.getDefaultTaskDefinitionArn(
                                            region,
                                            accountId)) : CfnString.create(jsonNode.path("TaskDefinitionArn"));
                                    Runner.RunnerBuilder runnerBuilder =
                                            Runner.builder()
                                                  .name(entry.getKey())
                                                  .installationCommands(commands)
                                                  .installationCommandsTimeout(CfnString.create(
                                                          jsonNode.path("Startup")
                                                                  .path("CommandsTimeout")))
                                                  .containerName(CfnString.create(jsonNode.path(
                                                          "ContainerName")))
                                                  .cluster(CfnString.create(jsonNode.path(
                                                          "EcsCluster")))
                                                  .roleArn(CfnString.create(jsonNode.path(
                                                          "RoleArn")))
                                                  .taskDefinitionArn(taskDefinitionArn)
                                                  .idleTimeToLive(CfnString.create(jsonNode.path("RunnerConfiguration")
                                                                                           .path("IdleTimeToLive")))
                                                  .jobTimeout(CfnString.create(jsonNode.path("RunnerConfiguration")
                                                                                       .path("JobTimeout")))
                                                  .logLevel(CfnString.create(jsonNode.path("RunnerConfiguration")
                                                                                     .path("LogLevel")))
                                                  .maxConcurrentJobs(CfnString.create(jsonNode.path(
                                                                                                      "RunnerConfiguration")
                                                                                              .path("MaxConcurrentJobs")))
                                                  .queueUrl(CfnString.create(createJsonNode(
                                                          "{\"Fn::GetAtt\" : \"" + getQueueName(
                                                                  entry.getKey()) + ".QueueUrl\"}",
                                                          objectMapper)))
                                                  .memory(CfnString.create(jsonNode.path(
                                                          "Memory")))
                                                  .cpu(CfnString.create(jsonNode.path("Cpu")));

                                    if (jsonNode.path("AwsVpcConfiguration").isMissingNode()) {
                                        List<String> subnets = getSubnets(ec2Client);
                                        securityGroups.put(entry.getKey(), Resources.securityGroups(entry.getKey()));
                                        runnerBuilder.subnets(CfnString.create(objectMapper.valueToTree(String.join(",",
                                                                                                                    subnets))));
                                        runnerBuilder.securityGroups(CfnString.create(createJsonNode(
                                                "{\"Fn::GetAtt\" : \"" + getSecurityGroupName(entry.getKey()) + ".GroupId\"}",
                                                objectMapper)));
                                        runnerBuilder.assignPublicIp(CfnString.create(objectMapper.valueToTree("ENABLED")));
                                    } else {
                                        JsonNode awsVpcConfiguration = jsonNode.path("AwsVpcConfiguration");
                                        validateVpcConfigProperty(awsVpcConfiguration.path("Subnets"),
                                                                  "Subnets",
                                                                  entry.getKey());
                                        validateVpcConfigProperty(awsVpcConfiguration.path("SecurityGroups"),
                                                                  "SecurityGroups",
                                                                  entry.getKey());
                                        validateVpcConfigProperty(awsVpcConfiguration.path("AssignPublicIp"),
                                                                  "AssignPublicIp",
                                                                  entry.getKey());
                                        runnerBuilder.subnets(CfnString.create(awsVpcConfiguration.path("Subnets")))
                                                     .securityGroups(CfnString.create(awsVpcConfiguration.path(
                                                             "SecurityGroups")))
                                                     .assignPublicIp(CfnString.create(awsVpcConfiguration.path(
                                                             "AssignPublicIp")));

                                    }

                                    if (!jsonNode.path("Image").isMissingNode() && !jsonNode.path("TaskDefinitionArn")
                                                                                            .isMissingNode()) {
                                        throw new IllegalArgumentException(
                                                "Both Image and TaskDefinitionArn is specified for Runner: " + entry.getKey());
                                    }

                                    if (!jsonNode.path("Image").isMissingNode()) {
                                        String taskDefinitionName = entry.getKey() + "TaskDefinition";
                                        String logGroupName = entry.getKey() + "LogGroup";
                                        runnerBuilder.taskDefinitionArn(CfnString.create(objectMapper.valueToTree(Map.of(
                                                "Ref",
                                                taskDefinitionName))));
                                        CfnString roleArn = createRoleArnRef(objectMapper, jsonNode.path("RoleArn"));
                                        if (jsonNode.path("Ec2Configuration").isMissingNode()) {
                                            taskDefinitions.put(taskDefinitionName,
                                                                Resources.taskDefinition(CfnString.create(jsonNode.get(
                                                                        "Image")), roleArn, logGroupName));
                                        } else {
                                            taskDefinitions.put(taskDefinitionName,
                                                                Resources.ec2taskDefinition(CfnString.create(jsonNode.get(
                                                                        "Image")), roleArn, logGroupName));
                                        }
                                        logGroups.put(logGroupName, Resources.logGroup());
                                    }


                                    if (!jsonNode.path("Ec2Configuration").isMissingNode()) {
                                        String ecsClientLogGroup = entry.getKey() + "EcsClientLogGroup";
                                        taskDefinitions.put(ecsClientLogGroup, Resources.logGroup());

                                        JsonNode ec2Configuration = jsonNode.path("Ec2Configuration");
                                        runnerBuilder
                                                .ec2Configuration(Ec2Configuration
                                                                          .builder()
                                                                          .imageId(CfnString.create(ec2Configuration
                                                                                                            .path("ImageId")))
                                                                          .ecsClientLogGroup(CfnString.create(
                                                                                  objectMapper.valueToTree(
                                                                                          Map.of("Ref",
                                                                                                 ecsClientLogGroup))))
                                                                          .instanceProfile(
                                                                                  createDefaultInstanceProfileRef(
                                                                                          objectMapper,
                                                                                          ec2Configuration
                                                                                                  .path("InstanceProfileName")))
                                                                          .instanceType(CfnString.create(
                                                                                  ec2Configuration
                                                                                          .path("InstanceType")))
                                                                          .build());
                                        if (jsonNode.path("Image").isMissingNode() && jsonNode.path("TaskDefinitionArn").isMissingNode()) {
                                            String taskDefinitionName = entry.getKey() + "TaskDefinition";
                                            taskDefinitions.put(taskDefinitionName,
                                                                Resources.ec2taskDefinition(CfnString.create(
                                                                                                    defaultRunnerImage),
                                                                                            createRoleArnRef(
                                                                                                    objectMapper,
                                                                                                    jsonNode.path(
                                                                                                            "RoleArn")),
                                                                                            entry.getKey() + "LogGroup"));
                                            runnerBuilder.taskDefinitionArn(CfnString.create(objectMapper.valueToTree(
                                                    Map.of(
                                                            "Ref",
                                                            taskDefinitionName))));
                                        }

                                    }
                                    return runnerBuilder.build();
                                })
                                .collect(Collectors.toMap(Runner::getName, Function.identity()));
    }

    private static CfnString createRoleArnRef(ObjectMapper objectMapper, JsonNode roleArnNode) {

        if (roleArnNode.isMissingNode()) {
            return CfnString.create(objectMapper.valueToTree(
                    Map.of("Fn::Sub",
                           "arn:aws:iam::${AWS::AccountId}:role/attini/attini-default-runner-role-${AWS::Region}")));
        }
        return CfnString.create(roleArnNode);
    }

    private static CfnString createDefaultInstanceProfileRef(ObjectMapper objectMapper, JsonNode instanceProfileNode) {

        if (instanceProfileNode.isMissingNode()) {
            return CfnString.create(objectMapper.valueToTree(
                    Map.of("Fn::Sub",
                           "attini-runner-default-instance-profile-${AWS::Region}")));
        }
        return CfnString.create(instanceProfileNode);
    }

    private void validateVpcConfigProperty(JsonNode jsonNode, String name, String runnerName) {
        if (jsonNode.isMissingNode()) {
            throw new IllegalArgumentException(name + " is missing from AwsVpcConfiguration for runner" + runnerName);

        }
    }

    private List<String> getSubnets(Ec2Client ec2Client) {
        return ec2Client.describeVpcs(DescribeVpcsRequest.builder()
                                                         .filters(Filter.builder()
                                                                        .name("is-default")
                                                                        .values("true")
                                                                        .build())
                                                         .build())
                        .vpcs()
                        .stream()
                        .findAny()
                        .map(Vpc::vpcId)
                        .map(vpcId -> ec2Client.describeSubnets(DescribeSubnetsRequest.builder()
                                                                                      .filters(Filter.builder()
                                                                                                     .name("vpc-id")
                                                                                                     .values(vpcId)
                                                                                                     .build())
                                                                                      .build())
                                               .subnets()
                                               .stream()
                                               .map(Subnet::subnetId)
                                               .collect(Collectors.toList()))
                        .orElseThrow(() -> new IllegalStateException(
                                "no default VPC found, AwsVpcConfiguration for Attini::Deploy::Runner is required"));

    }

    public Map<String, Map<String, Object>> getSqsQueues() {
        return runners.entrySet()
                      .stream()
                      .collect(Collectors.toMap(entry -> AttiniRunners.getQueueName(entry.getKey()),
                                                o -> Resources.sqsQueue()));
    }

    private JsonNode createJsonNode(String value, ObjectMapper objectMapper) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Set<String> getRunnerNames() {
        return runners != null ? runners.keySet() : Collections.emptySet();
    }


    public List<Runner> getRunners() {
        return new ArrayList<>(runners.values());
    }

    public static String getQueueName(String name) {
        return "AttiniRunnerQueue" + name;
    }

    private static String getSecurityGroupName(String name) {
        return "AttiniRunnerSecurityGroup" + name;
    }

    @Override
    public String toString() {
        return "AttiniRunners{" + "runners=" + runners + ", securityGroups=" + securityGroups + '}';
    }
}
