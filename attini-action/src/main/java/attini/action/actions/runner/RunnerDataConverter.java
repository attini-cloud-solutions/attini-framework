package attini.action.actions.runner;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import attini.action.actions.deploycloudformation.SfnExecutionArn;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class RunnerDataConverter {

    public static RunnerData convert(Map<String, AttributeValue> item) {
        String runnerName = item.get("runnerName").s();
        RunnerData.RunnerDataBuilder builder = RunnerData.builder()
                                                         .runnerName(runnerName)
                                                         .distributionId(DistributionId.of(item.get("distributionId")
                                                                                               .s()))
                                                         .distributionName(DistributionName.of(item.get(
                                                                 "distributionName").s()))
                                                         .environment(Environment.of(item.get("environment").s()))
                                                         .objectIdentifier(item.get("attiniObjectIdentifier").s())
                                                         .stackName(item.get("stackName").s())
                                                         .taskId(toNullableString(item.get("taskId")))
                                                         .container(toNullableString(item.get("container")))
                                                         .taskDefinitionArn(item.get("taskDefinitionArn").s())
                                                         .taskConfigurationHashCode(item.get("taskConfigHashCode") == null ? null : Integer.parseInt(
                                                                 item.get("taskConfigHashCode").s()))
                                                         .taskConfiguration(toTaskConfiguration(item))
                                                         .started(item.get("started") != null && item.get("started")
                                                                                                     .bool())
                                                         .startedByExecutionArn(item.get("startedByExecutionArn") != null ? SfnExecutionArn.create(
                                                                 item.get("startedByExecutionArn").s()) : null);

        if (item.containsKey("ec2Configuration")) {
            Map<String, AttributeValue> ec2Configuration = item.get("ec2Configuration")
                                                               .m();
            Ec2 ec2 = Ec2.builder()
                         .latestEc2InstanceId(toNullableString(item.get(
                                 "latestEc2InstanceId")))
                         .configHashCode(item.get("ec2ConfigHashCode") == null ? 0 : Integer.parseInt(item.get(
                                 "ec2ConfigHashCode").s()))
                         .ec2Config(Ec2Config.builder()
                                             .instanceType(toNoneNullString(ec2Configuration.get("instanceType"),
                                                                            "instanceType",
                                                                            runnerName))
                                             .ecsClientLogGroup(
                                                     toNoneNullString(ec2Configuration.get("ecsClientLogGroup"),
                                                                      "ecsClientLogGroup", runnerName))
                                             .instanceProfile(
                                                     toNoneNullString(ec2Configuration.get("instanceProfile"),
                                                                      "instanceProfile", runnerName))
                                             .ami(toNullableString(ec2Configuration.get("ami")))
                                             .build())
                         .build();
            builder.ec2(ec2);
        }
        return builder.build();

    }

    public static TaskConfiguration toTaskConfiguration(Map<String, AttributeValue> item) {
        String runnerName = item.get("runnerName").s();
        return TaskConfiguration.builder()
                                .roleArn(toNullableString(item.get("roleArn")))
                                .assignPublicIp(toAssignPublicIp(item.get("assignPublicIp").s(), runnerName))
                                .securityGroups(Set.copyOf(item.get("securityGroups").ss()))
                                .subnets(Set.copyOf(item.get("subnets").ss()))
                                .cluster(toNullableString(item.get("cluster")))
                                .container(toNullableString(item.get("container")))
                                .queueUrl(item.get("sqsQueueUrl").s())
                                .attiniVersion(item.get("attiniVersion") == null ? null : item.get("attiniVersion")
                                                                                              .s()) // allowed to be null for backwards compatibility
                                .installationCommands(item.get("startupCommands") == null ? null : item.get(
                                        "startupCommands").l().stream().map(
                                        AttributeValue::s).collect(
                                        Collectors.toList()))
                                .installationCommandsTimeout(nullablePositiveInteger(item.get("startupCommandsTimeout"),
                                                                                     "startupCommandsTimeout",
                                                                                     runnerName,
                                                                                     5,
                                                                                     172800))
                                .taskDefinitionArn(toNoneNullString(item.get(
                                                                            "taskDefinitionArn"),
                                                                    "TaskDefinitionArn",
                                                                    runnerName))
                                .runnerConfiguration(toRunnerConfiguration(item,
                                                                           runnerName))
                                .cpu(nullablePositiveInteger(item.get("cpu"), "cpu", runnerName, 256, 98304))
                                .memory(nullablePositiveInteger(item.get("memory"),
                                                                "memory",
                                                                runnerName,
                                                                512,
                                                                25165824))
                                .build();
    }

    public static RunnerConfiguration toRunnerConfiguration(Map<String, AttributeValue> item,
                                                            String runnerName) {

        return RunnerConfiguration.builder()
                                  .idleTimeToLive(nullablePositiveInteger(item.get("idleTimeToLive"),
                                                                          "IdleTimeToLive",
                                                                          runnerName, 60, 86400))
                                  .jobTimeout(nullablePositiveInteger(item.get("jobTimeout"),
                                                                      "JobTimeout",
                                                                      runnerName, 5, 172800))
                                  .maxConcurrentJobs(nullablePositiveInteger(item.get(
                                                                                     "maxConcurrentJobs"),
                                                                             "MaxConcurrentJobs",
                                                                             runnerName, 1, 20))
                                  .logLevel(toLogLevel(item.get("logLevel"), runnerName))
                                  .build();

    }

    private static String toNullableString(AttributeValue value) {
        return value == null ? null : value.s();

    }

    private static Integer nullablePositiveInteger(AttributeValue value,
                                                   String name,
                                                   String runnerName,
                                                   int minimum,
                                                   int maximum) {
        try {
            if (value == null) {
                return null;
            }
            int intValue = Integer.parseInt(value.n());
            if (intValue < minimum) {
                throw new CouldNotParseInputException("Illegal configuration for runner " + runnerName + ", " + name + " should be a number greater or equal to " + minimum);
            }

            if (intValue > maximum) {
                throw new CouldNotParseInputException("Illegal configuration for runner " + runnerName + ", " + name + " should be a number lesser or equal to " + maximum);
            }

            return intValue;
        } catch (NumberFormatException e) {
            throw new CouldNotParseInputException("Illegal configuration for runner " + runnerName + ", " + name + " should be a number");
        }
    }

    private static String toNoneNullString(AttributeValue value, String fieldName, String runnerName) {
        if (value == null) {
            throw new CouldNotParseInputException("Illegal configuration for runner " + runnerName + ", missing " + fieldName);
        }
        return value.s();
    }

    private static String toAssignPublicIp(String assignPublicIp, String runnerName) {
        if (assignPublicIp == null) {
            throw new CouldNotParseInputException("Illegal configuration for runner " + runnerName + ", AssignPublicIp must be set to either ENABLED or DISABLED");
        }
        try {
            return assignPublicIp;
        } catch (IllegalArgumentException e) {
            throw new CouldNotParseInputException("Illegal configuration for runner " + runnerName + ", " + assignPublicIp + " is not a valid value for for AssignPublicIp. Allowed values are ENABLED or DISABLED");
        }
    }

    private static RunnerConfiguration.LogLevel toLogLevel(AttributeValue logLvl, String runnerName) {
        if (logLvl == null) {
            return null;
        }
        try {
            return RunnerConfiguration.LogLevel.valueOf(logLvl.s());
        } catch (IllegalArgumentException e) {
            throw new CouldNotParseInputException("Illegal configuration for runner " + runnerName + ", " + logLvl + " is not a valid log level. Valid log levels are DEBUG, INFO, WARN, ERROR, OFF and ALL ");
        }
    }

}
