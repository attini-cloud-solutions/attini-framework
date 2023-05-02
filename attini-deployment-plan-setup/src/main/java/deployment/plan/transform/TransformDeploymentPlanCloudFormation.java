/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package deployment.plan.transform;

import static deployment.plan.transform.ObjectTypeUtil.isMap;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import deployment.plan.system.EnvironmentVariables;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.services.ec2.Ec2Client;

@SuppressWarnings("unchecked")
@ApplicationScoped
public class TransformDeploymentPlanCloudFormation {
    private static final Logger logger = Logger.getLogger(TransformDeploymentPlanCloudFormation.class);

    private static final String TYPE_KEY = "Type";
    public static final String ATTINI_DEPLOY_DEPLOYMENT_PLAN_TYPE = "Attini::Deploy::DeploymentPlan";
    private final EnvironmentVariables environmentVariables;
    private final Ec2Client ec2Client;
    private final ObjectMapper objectMapper;
    private final DeploymentPlanStepsCreator deploymentPlanStepsCreator;

    public TransformDeploymentPlanCloudFormation(EnvironmentVariables environmentVariables,
                                                 Ec2Client ec2Client,
                                                 ObjectMapper objectMapper,
                                                 DeploymentPlanStepsCreator deploymentPlanStepsCreator) {
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.ec2Client = requireNonNull(ec2Client, "ec2Client");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.deploymentPlanStepsCreator = deploymentPlanStepsCreator;
    }

    public Map<String, Object> transformTemplate(Map<String, Object> input) {

        Map<String, Object> fragment = (Map<String, Object>) input.get("fragment");
        if (!fragment.containsKey("Description")) {
            String distName = objectMapper.valueToTree(input)
                                          .path("templateParameterValues")
                                          .path("AttiniDistributionName")
                                          .textValue();
            fragment.put("Description",
                         distName == null ? "Attini init deploy stack" : "Attini init deploy stack for distribution: " + distName);
        }

        fragment.put("Resources", transformDeploymentPlanFragments(getResources(fragment), fragment.get("Parameters")));
        input.put("status", "success");
        return input;
    }

    private Map<String, Map<String, Object>> transformDeploymentPlanFragments(Map<String, Map<String, Object>> resources,
                                                                              Object parameters) {

        validateOnlyOneDeploymentPlan(resources);

        Optional<DeploymentPlanWrapper> deploymentPlanWrapper = getDeploymentPlanWrapper(resources);

        if (deploymentPlanWrapper.map(DeploymentPlanWrapper::shouldDeployDefaultRunner).orElse(false)) {
            logger.info("Adding default runner for steps with no runner configured");

            resources.put("AttiniDefaultRunner",
                          Resources.runner(environmentVariables.getRegion(), environmentVariables.getAccount()));
        }

        AttiniRunners attiniRunners = new AttiniRunners(resources,
                                                        ec2Client,
                                                        environmentVariables.getRegion(),
                                                        environmentVariables.getAccount(),
                                                        environmentVariables.getDefaultRunnerImage());

        return Stream.of(attiniRunners.getSqsQueues(),
                         attiniRunners.getSecurityGroups(),
                         attiniRunners.getLogGroups(),
                         attiniRunners.getTaskDefinitions(),
                         deploymentPlanWrapper.map(deploymentPlan -> transformDeploymentPlan(attiniRunners,
                                                                                             deploymentPlan,
                                                                                             parameters))
                                              .orElse(emptyMap()),
                         getNoneAttiniResources(resources,
                                                getAttiniResourceNames(deploymentPlanWrapper.stream(), attiniRunners)))
                     .flatMap(map -> map.entrySet().stream())
                     .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private Map<String, Map<String, Object>> transformDeploymentPlan(AttiniRunners attiniRunners,
                                                                     DeploymentPlanWrapper deploymentPlan,
                                                                     Object parameters) {
        String sfnResourceName = String.format("AttiniDeploymentPlanSfn%s",
                                               deploymentPlan.getDeploymentPlanName());
        return Map.of(sfnResourceName,
                      createStateMachineResource(deploymentPlan.getDeploymentPlanResource(),
                                                 deploymentPlan.containsSamSteps()),
                      String.format("%sTrigger", sfnResourceName),
                      createDeploymentPlanTrigger(sfnResourceName,
                                                  attiniRunners,
                                                  parameters,
                                                  deploymentPlan.getDeploymentPlanResource()
                                                                .getDeploymentPlanProperties()
                                                                .getPayloadDefaults()),
                      String.format("AttiniPostExecutionActions%s", deploymentPlan.getDeploymentPlanName()),
                      createStateMachinePostHook(sfnResourceName));
    }

    private Optional<DeploymentPlanWrapper> getDeploymentPlanWrapper(Map<String, Map<String, Object>> resources) {
        return resources.entrySet()
                        .stream()
                        .filter(map -> map.getValue()
                                          .get(TYPE_KEY)
                                          .equals(ATTINI_DEPLOY_DEPLOYMENT_PLAN_TYPE))
                        .findAny()
                        .map(fromValue -> new DeploymentPlanWrapper(
                                fromValue.getKey(), createDeploymentPlanResource(fromValue.getValue()),
                                objectMapper));
    }

    private static void validateOnlyOneDeploymentPlan(Map<String, Map<String, Object>> resources) {
        if (resources.values()
                     .stream()
                     .map(entry -> entry.get(TYPE_KEY))
                     .filter(ATTINI_DEPLOY_DEPLOYMENT_PLAN_TYPE::equals)
                     .count() > 1) {

            throw new TransformDeploymentPlanException("More then one deployment plan detected.");

        }
    }

    private static Set<String> getAttiniResourceNames(Stream<DeploymentPlanWrapper> deploymentPlanWrapper,
                                                      AttiniRunners attiniRunners) {
        return Stream.of(attiniRunners.getRunnerNames().stream(),
                         deploymentPlanWrapper.map(DeploymentPlanWrapper::getDeploymentPlanName))
                     .flatMap(Function.identity())
                     .collect(Collectors.toSet());
    }

    private static Map<String, Map<String, Object>> getNoneAttiniResources(Map<String, Map<String, Object>> resources,
                                                                           Set<String> attiniResources) {
        return resources.entrySet()
                        .stream()
                        .filter(stringMapEntry -> !attiniResources.contains(stringMapEntry.getKey()))
                        .collect(Collectors.toMap(Entry::getKey,
                                                  Entry::getValue));
    }

    private DeploymentPlanResource createDeploymentPlanResource(Object fromValue) {
        try {
            return objectMapper.convertValue(fromValue, DeploymentPlanResource.class);
        } catch (IllegalArgumentException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof IllegalArgumentException cause) {
                throw cause;
            }
            throw e;
        }
    }


    private Map<String, Map<String, Object>> getResources(Map<String, Object> fragments) {
        Map<String, Object> resources = (Map<String, Object>) fragments.get("Resources");
        if (resources == null) {
            throw new TransformDeploymentPlanException("The resource block in the init stack is empty or absent.");
        }
        return resources.entrySet()
                        .stream()
                        .filter(entry -> isMap(entry.getValue()))
                        .collect(Collectors.toMap(Entry::getKey, entry -> (Map<String, Object>) entry.getValue()));

    }

    private Map<String, Object> createStateMachineResource(DeploymentPlanResource deploymentPlanData,
                                                           boolean shouldAddSam) {
        SfnProperties properties = getSfnProperties(deploymentPlanData.getDeploymentPlanProperties(), shouldAddSam);
        HashMap<String, Object> metadata = new HashMap<>(deploymentPlanData.getMetadata());
        metadata.put("AttiniSteps", properties.attiniManagedSteps);
        return Map.of(TYPE_KEY, "AWS::Serverless::StateMachine",
                      "Properties", properties.sfnProperties,
                      "Metadata", metadata);
    }

    private SfnProperties getSfnProperties(DeploymentPlanProperties deploymentPlanProperties, boolean shouldAddSam) {
        Map<String, Object> sfnProperties = new HashMap<>();

        if (deploymentPlanProperties.getPolicies().isEmpty() &&
            deploymentPlanProperties.getRoleArn().isEmpty() &&
            environmentVariables.getDefaultRole() == null) {
            throw new IllegalArgumentException(
                    "No deployment plan role or policy configured and the default role is disabled, add a RoleArn or Policy to the deployment plan config");
        } else if (deploymentPlanProperties.getPolicies().isEmpty() &&
                   deploymentPlanProperties.getRoleArn().isEmpty()) {
            sfnProperties.put("Role", environmentVariables.getDefaultRole());
        } else if (deploymentPlanProperties.getRoleArn().isPresent()) {
            sfnProperties.put("Role", deploymentPlanProperties.getRoleArn().get());
        }

        DeploymentPlan deploymentPlan = deploymentPlanProperties.getDeploymentPlan();
        DeploymentPlanDefinition deploymentPlanDefinition = deploymentPlanStepsCreator.createDefinition(deploymentPlan,
                                                                                                        shouldAddSam);
        sfnProperties.put("Definition", deploymentPlanDefinition.definition());
        sfnProperties.put("Tags", Map.of("AttiniProvider", "DeploymentPlan"));

        if (!deploymentPlanProperties.getDefinitionSubstitutions().isEmpty()) {
            sfnProperties.put("DefinitionSubstitutions", deploymentPlanProperties.getDefinitionSubstitutions());
        }

        deploymentPlanProperties.getPermissionsBoundary()
                                .ifPresent(cfnString -> sfnProperties.put("PermissionsBoundary", cfnString));
        deploymentPlanProperties.getPolicies().ifPresent(object -> sfnProperties.put("Policies", object));

        return new SfnProperties(sfnProperties, deploymentPlanDefinition.attiniSteps());
    }

    private record SfnProperties(Map<String, Object> sfnProperties,
                                 List<AttiniStep> attiniManagedSteps) {
    }

    private static Map<String, Object> createStateMachinePostHook(String sfnResourceName) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("Description", "Attini deployment plan post hook");
        properties.put("EventPattern", Map.of("Fn::Sub",
                                              """
                                                      {
                                                        "source": ["aws.states"],
                                                        "detail-type": ["Step Functions Execution Status Change"],
                                                        "detail": {
                                                          "stateMachineArn": ["${%s}"],
                                                          "status": ["FAILED","SUCCEEDED","ABORTED","TIMED_OUT"]
                                                        }
                                                      }
                                                      """.formatted(sfnResourceName)));

        properties.put("Targets",
                       List.of(Map.of("Arn",
                                      Map.of("Fn::Sub",
                                             "arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:attini-action"),
                                      "Id",
                                      "post-execution-actions")));


        return Map.of(TYPE_KEY, "AWS::Events::Rule", "Properties", properties);
    }


    private static Map<String, Object> createDeploymentPlanTrigger(String sfnResourceName,
                                                                   AttiniRunners attiniRunners,
                                                                   Object parameters,
                                                                   Map<String, Object> payloadDefaults) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("ServiceToken",
                       Map.of("Fn::Sub",
                              "arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:attini-deployment-plan-setup"));
        properties.put("StackName", Map.of("Ref", "AWS::StackName"));
        properties.put("SfnName", Map.of("Fn::GetAtt", String.format("%s.Name", sfnResourceName)));
        properties.put("SfnArn", Map.of("Ref", sfnResourceName));
        properties.put("DeploymentPlanLogicalName", sfnResourceName);
        properties.put("RandomGUID",
                       UUID.randomUUID().toString()); //used to make sure the customResource is always triggered
        properties.put("Runners", attiniRunners.getRunners());
        properties.put("PayloadDefaults", payloadDefaults);

        if (parameters != null) {

            Map<String, Map<String, Map<String, Object>>> rawParameters = (Map<String, Map<String, Map<String, Object>>>) parameters;
            Map<String, Map<String, String>> transformedParams = rawParameters.entrySet()
                                                                              .stream()
                                                                              .collect(Collectors.toMap(Entry::getKey,
                                                                                                        entry -> Map.of(
                                                                                                                "Ref",
                                                                                                                entry.getKey())));
            properties.put("Parameters", transformedParams);

        }

        return Map.of(TYPE_KEY, "Custom::AttiniDeploymentPlanTrigger", "Properties", properties);

    }

}


