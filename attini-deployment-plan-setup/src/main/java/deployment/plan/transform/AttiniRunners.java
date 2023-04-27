package deployment.plan.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    private final String region;
    private final String accountId;

    private final ObjectMapper objectMapper;
    public AttiniRunners(Map<String, Map<String, Object>> resources,
                         Ec2Client ec2Client,
                         String region,
                         String accountId,
                         String defaultRunnerImage) {
        this.region = region;
        this.accountId = accountId;
        this.objectMapper = new ObjectMapper();

        this.runners = resources.entrySet()
                                .stream()
                                .filter(entry -> "Attini::Deploy::Runner".equals(entry.getValue().get("Type")))
                                .map(entry -> {
                                    String resourceName = entry.getKey();
                                    JsonNode properties = objectMapper.valueToTree(entry.getValue()).path("Properties");
                                    checkImageAndTaskDefinition(resourceName, properties);

                                    Runner.RunnerBuilder runnerBuilder = initBuilder(resourceName, properties);

                                    if (properties.path("AwsVpcConfiguration").isMissingNode()) {
                                        securityGroups.put(resourceName, Resources.securityGroups(resourceName));
                                        runnerBuilder.subnets(CfnString.create(String.join(",", getSubnets(ec2Client))));
                                        runnerBuilder.securityGroups(createIntrinsicFunction("Fn::GetAtt",
                                                                                             getSecurityGroupName(
                                                                                                     resourceName) + ".GroupId"));

                                        runnerBuilder.assignPublicIp(CfnString.create(objectMapper.valueToTree("ENABLED")));
                                    } else {
                                        JsonNode awsVpcConfiguration = properties.path("AwsVpcConfiguration");
                                        validateVpcConfiguration(entry, awsVpcConfiguration);
                                        runnerBuilder.subnets(CfnString.create(awsVpcConfiguration.path("Subnets")))
                                                     .securityGroups(CfnString.create(awsVpcConfiguration.path(
                                                             "SecurityGroups")))
                                                     .assignPublicIp(CfnString.create(awsVpcConfiguration.path(
                                                             "AssignPublicIp")));

                                    }

                                    if (!properties.path("Image").isMissingNode()) {
                                        String taskDefinitionName = resourceName + "TaskDefinition";
                                        String logGroupName = resourceName + "LogGroup";
                                        runnerBuilder.taskDefinitionArn(createIntrinsicFunction("Ref",
                                                                                                taskDefinitionName));
                                        CfnString roleArn = createRoleArnRef(properties.path("RoleArn"));
                                        if (properties.path("Ec2Configuration").isMissingNode()) {
                                            taskDefinitions.put(taskDefinitionName,
                                                                Resources.taskDefinition(CfnString.create(properties.get(
                                                                        "Image")), roleArn, logGroupName));
                                        } else {
                                            taskDefinitions.put(taskDefinitionName,
                                                                Resources.ec2taskDefinition(CfnString.create(properties.get(
                                                                        "Image")), roleArn, logGroupName));
                                        }
                                        logGroups.put(logGroupName, Resources.logGroup());
                                    }


                                    if (!properties.path("Ec2Configuration").isMissingNode()) {
                                        String ecsClientLogGroupName = resourceName + "EcsClientLogGroup";
                                        taskDefinitions.put(ecsClientLogGroupName, Resources.logGroup());
                                        runnerBuilder
                                                .ec2Configuration(createEc2Configuration(ecsClientLogGroupName,
                                                                                         properties.path(
                                                                                                 "Ec2Configuration")));

                                        if (properties.path("Image").isMissingNode() && properties.path(
                                                "TaskDefinitionArn").isMissingNode()) {
                                            String taskDefinitionName = resourceName + "TaskDefinition";
                                            String logGroupName = resourceName + "LogGroup";
                                            taskDefinitions.put(taskDefinitionName,
                                                                Resources.ec2taskDefinition(CfnString.create(
                                                                                                    defaultRunnerImage),
                                                                                            createRoleArnRef(
                                                                                                    properties.path(
                                                                                                            "RoleArn")),
                                                                                            logGroupName));
                                            runnerBuilder.taskDefinitionArn(createIntrinsicFunction("Ref",
                                                                                                    taskDefinitionName));
                                            logGroups.put(logGroupName, Resources.logGroup());
                                        }

                                    }
                                    return runnerBuilder.build();
                                })
                                .collect(Collectors.toMap(Runner::getName, Function.identity()));
    }

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


    private static void checkImageAndTaskDefinition(String resourceName, JsonNode properties) {
        if (!properties.path("Image").isMissingNode() && !properties.path("TaskDefinitionArn")
                                                                    .isMissingNode()) {
            throw new IllegalArgumentException(
                    "Both Image and TaskDefinitionArn is specified for Runner: " + resourceName);
        }
    }

    private Runner.RunnerBuilder initBuilder(String resourceName,
                                             JsonNode jsonNode) {
        return Runner.builder()
                     .name(resourceName)
                     .installationCommands(createStartupCommands(resourceName, jsonNode))
                     .installationCommandsTimeout(CfnString.create(
                             jsonNode.path("Startup")
                                     .path("CommandsTimeout")))
                     .containerName(CfnString.create(jsonNode.path(
                             "ContainerName")))
                     .cluster(CfnString.create(jsonNode.path(
                             "EcsCluster")))
                     .roleArn(CfnString.create(jsonNode.path(
                             "RoleArn")))
                     .taskDefinitionArn(getTaskDefinitionArn(jsonNode))
                     .idleTimeToLive(CfnString.create(jsonNode.path("RunnerConfiguration")
                                                              .path("IdleTimeToLive")))
                     .jobTimeout(CfnString.create(jsonNode.path("RunnerConfiguration")
                                                          .path("JobTimeout")))
                     .logLevel(CfnString.create(jsonNode.path("RunnerConfiguration")
                                                        .path("LogLevel")))
                     .maxConcurrentJobs(CfnString.create(jsonNode.path(
                                                                         "RunnerConfiguration")
                                                                 .path("MaxConcurrentJobs")))
                     .queueUrl(createIntrinsicFunction("Fn::GetAtt", getQueueName(resourceName) + ".QueueUrl"))

                     .memory(CfnString.create(jsonNode.path(
                             "Memory")))
                     .cpu(CfnString.create(jsonNode.path("Cpu")));
    }

    private CfnString getTaskDefinitionArn(JsonNode jsonNode) {
        return jsonNode.path("TaskDefinitionArn")
                       .isMissingNode() ? CfnString.create(Resources.getDefaultTaskDefinitionArn(
                region,
                accountId)) : CfnString.create(jsonNode.path("TaskDefinitionArn"));
    }

    private static List<CfnString> createStartupCommands(String resourceName, JsonNode jsonNode) {
        JsonNode installationCommandsNode = jsonNode.path("Startup").path("Commands");
        if (!installationCommandsNode.isMissingNode() && !installationCommandsNode.isArray()) {
            throw new IllegalArgumentException(
                    "Illegal format for installations commands for runner" + resourceName + ". Installation commands should be a list");
        }

        return StreamSupport.stream(installationCommandsNode.spliterator(),
                                    false)
                            .map(CfnString::create)
                            .collect(Collectors.toList());
    }

    private Ec2Configuration createEc2Configuration(String ecsClientLogGroup,
                                                    JsonNode ec2Configuration) {
        return Ec2Configuration
                .builder()
                .ami(CfnString.create(ec2Configuration
                                              .path("Ami")))
                .ecsClientLogGroup(CfnString.create(
                        objectMapper.valueToTree(
                                Map.of("Ref",
                                       ecsClientLogGroup))))
                .instanceProfile(
                        createDefaultInstanceProfileRef(
                                ec2Configuration
                                        .path("InstanceProfileName")))
                .instanceType(CfnString.create(
                        ec2Configuration
                                .path("InstanceType")))
                .build();
    }

    private void validateVpcConfiguration(Map.Entry<String, Map<String, Object>> entry, JsonNode awsVpcConfiguration) {
        validateVpcConfigProperty(awsVpcConfiguration.path("Subnets"),
                                  "Subnets",
                                  entry.getKey());
        validateVpcConfigProperty(awsVpcConfiguration.path("SecurityGroups"),
                                  "SecurityGroups",
                                  entry.getKey());
        validateVpcConfigProperty(awsVpcConfiguration.path("AssignPublicIp"),
                                  "AssignPublicIp",
                                  entry.getKey());
    }

    private CfnString createIntrinsicFunction(String function, String value) {
        return CfnString.create(objectMapper.createObjectNode().put(function, value));
    }

    private CfnString createRoleArnRef(JsonNode roleArnNode) {

        if (roleArnNode.isMissingNode()) {
            return CfnString.create(objectMapper.valueToTree(
                    Map.of("Fn::Sub",
                           "arn:aws:iam::${AWS::AccountId}:role/attini/attini-default-runner-role-${AWS::Region}")));
        }
        return CfnString.create(roleArnNode);
    }

    private CfnString createDefaultInstanceProfileRef(JsonNode instanceProfileNode) {

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
