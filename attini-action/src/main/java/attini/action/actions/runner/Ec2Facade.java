package attini.action.actions.runner;

import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.system.EnvironmentVariables;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.IamInstanceProfileSpecification;
import software.amazon.awssdk.services.ec2.model.InstanceStatus;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.SummaryStatus;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.VolumeType;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

public class Ec2Facade {

    private static final Logger logger = Logger.getLogger(Ec2Facade.class);


    private final Ec2Client ec2Client;
    private final EnvironmentVariables environmentVariables;
    private final SsmClient ssmClient;
    private final ObjectMapper objectMapper;

    private final static Map<String, String> imageIdMap = Map.of("AmazonLinux2",
                                                                 "/aws/service/ecs/optimized-ami/amazon-linux-2/kernel-5.10/recommended",
                                                                 "AmazonLinux2_arm64",
                                                                 "/aws/service/ecs/optimized-ami/amazon-linux-2/kernel-5.10/arm64/recommended",
                                                                 "AmazonLinux2_gpu",
                                                                 "/aws/service/ecs/optimized-ami/amazon-linux-2/gpu/recommended",
                                                                 "AmazonLinux2_inf",
                                                                 "/aws/service/ecs/optimized-ami/amazon-linux-2/inf/recommended",
                                                                 "AmazonLinux2023",
                                                                 "/aws/service/ecs/optimized-ami/amazon-linux-2023/recommended",
                                                                 "AmazonLinux2023_arm64",
                                                                 "/aws/service/ecs/optimized-ami/amazon-linux-2023/arm64/recommended",
                                                                 "AmazonLinux2023_inf",
                                                                 "/aws/service/ecs/optimized-ami/amazon-linux-2023/inf/recommended");

    public Ec2Facade(Ec2Client ec2Client,
                     EnvironmentVariables environmentVariables,
                     SsmClient ssmClient, ObjectMapper objectMapper) {
        this.ec2Client = requireNonNull(ec2Client, "ec2Client");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.ssmClient = requireNonNull(ssmClient, "ssmClient");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
    }

    public String startInstance(Ec2 ec2, RunnerData runnerData) {

        logger.info("Starting new ec2 instance");

        String imageId = getImageId(ec2.getEc2Config().ami().orElse("AmazonLinux2"));
        String deviceName = ec2Client.describeImages(DescribeImagesRequest.builder().imageIds(imageId).build())
                                     .images()
                                     .stream().findAny()
                                     .flatMap(image -> image.blockDeviceMappings()
                                                            .stream()
                                                            .findAny())
                                     .map(BlockDeviceMapping::deviceName)
                                     .orElseThrow(() -> new RuntimeException("Could not get device name for imageId: " + imageId));

        logger.info("Resolved device name: " + deviceName + " for imageId: " + imageId);

        BlockDeviceMapping blockDeviceMapping =
                BlockDeviceMapping.builder()
                                  .deviceName(deviceName)
                                  .ebs(EbsBlockDevice.builder()
                                                     .deleteOnTermination(true)
                                                     .volumeSize(50)
                                                     .volumeType(VolumeType.GP3)
                                                     .build())
                                  .build();
        RunInstancesResponse ec2Instance =
                ec2Client.runInstances(RunInstancesRequest.builder()
                                                          .blockDeviceMappings(blockDeviceMapping)
                                                          .ebsOptimized(true)
                                                          .minCount(1)
                                                          .maxCount(1)
                                                          .subnetId(runnerData.getTaskConfiguration()
                                                                              .subnets()
                                                                              .stream()
                                                                              .findAny()
                                                                              .orElseThrow(() -> new IllegalArgumentException(
                                                                                      "No subnet configured")))
                                                          .securityGroupIds(runnerData.getTaskConfiguration()
                                                                                      .securityGroups())
                                                          .instanceType(ec2.getEc2Config()
                                                                           .instanceType())
                                                          .tagSpecifications(
                                                                  TagSpecification.builder()
                                                                                  .resourceType(
                                                                                          ResourceType.INSTANCE)
                                                                                  .tags(Tag.builder()
                                                                                           .key("AttiniResourceType")
                                                                                           .value("RunnerInstance")
                                                                                           .build(),
                                                                                        Tag.builder()
                                                                                           .key("Name")
                                                                                           .value(runnerData.getRunnerName())
                                                                                           .build(),
                                                                                        Tag.builder()
                                                                                           .key("AttiniDistributionName")
                                                                                           .value(runnerData.getDistributionName().asString())
                                                                                           .build(),
                                                                                        Tag.builder()
                                                                                           .key("AttiniEnvironment")
                                                                                           .value(runnerData.getEnvironment().asString())
                                                                                           .build(),
                                                                                        Tag.builder()
                                                                                           .key("EcsCluster")
                                                                                           .value(runnerData.getTaskConfiguration().cluster())
                                                                                           .build())
                                                                                  .build())
                                                          .iamInstanceProfile(
                                                                  IamInstanceProfileSpecification.builder()
                                                                                                 .name(ec2.getEc2Config()
                                                                                                          .instanceProfile())
                                                                                                 .build())
                                                          .imageId(imageId)
                                                          .userData(
                                                                  Base64.getEncoder()
                                                                        .encodeToString(
                                                                                """
                                                                                        #!/bin/bash
                                                                                        echo "ECS_CLUSTER=attini-default" >> /etc/ecs/ecs.config
                                                                                        echo "ECS_ENABLE_CONTAINER_METADATA=true" >> /etc/ecs/ecs.config
                                                                                        echo 'ECS_INSTANCE_ATTRIBUTES={"runnerResourceName": "%s"}' >> /etc/ecs/ecs.config
                                                                                        echo "ECS_LOG_DRIVER=awslogs" >> /etc/ecs/ecs.config
                                                                                        echo 'ECS_LOG_OPTS={"awslogs-group":"%s","awslogs-region":"%s"}' >> /etc/ecs/ecs.config
                                                                                        """.formatted(runnerData.getAttiniRunnerResourceName(),
                                                                                                      ec2.getEc2Config()
                                                                                                         .ecsClientLogGroup(),
                                                                                                      environmentVariables.getRegion())
                                                                                           .getBytes(StandardCharsets.UTF_8)))
                                                          .build());

        return ec2Instance.instances().get(0).instanceId();
    }

    private String getImageId(String ami) {
        String ssmKey = imageIdMap.get(ami);

        if (ssmKey == null && !ami.startsWith("ami-")) {
            throw new IllegalArgumentException("Invalid AMI, allowed short hand values are: " + imageIdMap.keySet() + ". You can also" +
                                               " specify a valid imageId, starting with \"ami-\". Current value: " + ami);
        }

        if (ssmKey == null){
            return ami;
        }

        String paramValue = ssmClient.getParameter(GetParameterRequest.builder().name(ssmKey).build())
                                     .parameter()
                                     .value();

        try {
            return objectMapper.readTree(paramValue).path("image_id").asText();
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public boolean instanceIsRunning(String instanceId) {
        try {
            List<InstanceStatus> instanceStatuses = ec2Client.describeInstanceStatus(
                    DescribeInstanceStatusRequest.builder()
                                                 .instanceIds(instanceId)
                                                 .build()).instanceStatuses();

            if (instanceStatuses.isEmpty()) {
                return false;
            }
            return instanceStatuses.get(0)
                                   .instanceStatus()
                                   .status()
                                   .equals(SummaryStatus.OK);
        } catch (Ec2Exception e) {
            // if the instance was terminated more then a few hours ago then an Ec2Exception will be thrown.
            logger.info("Could not get instance status of latest instance. Reason: " + e.getMessage());
            return false;
        }
    }

    public void waitForStart(String instanceId) {
        ec2Client.waiter()
                 .waitUntilInstanceStatusOk(DescribeInstanceStatusRequest.builder()
                                                                         .instanceIds(instanceId)
                                                                         .build());


        logger.info("EC2 instance is running");
    }

    public void waitForStop(String instanceId) {
        ec2Client.waiter()
                 .waitUntilInstanceTerminated(DescribeInstancesRequest.builder()
                                                                      .instanceIds(instanceId)
                                                                      .build());


        logger.info("EC2 instance is stopped");
    }

    public void terminateInstance(String instanceId) {
        logger.info("Terminating ec2 instance with id: " + instanceId);
        ec2Client.terminateInstances(TerminateInstancesRequest.builder().instanceIds(instanceId).build());
    }
}
