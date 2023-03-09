package deployment.plan.transform;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;

import deployment.plan.system.EnvironmentVariables;

public class DeploymentPlanStepsCreator {

    private static final Logger logger = Logger.getLogger(DeploymentPlanStepsCreator.class);


    private static final String GET_DEPLOY_DATA_KEY = "AttiniPrepareDeployment";

    private static final String TYPE_KEY = "Type";
    private static final String SAM_PACKAGE_CHOICE_KEY = "AttiniSamPackage?";
    private static final String SAM_PACKAGE_KEY = "AttiniSamPackage";

    private final AttiniStepLoader attiniStepLoader;
    private final DeployData deployData;
    private final ObjectMapper objectMapper;
    private final EnvironmentVariables environmentVariables;

    public DeploymentPlanStepsCreator(AttiniStepLoader attiniStepLoader,
                                      DeployData deployData,
                                      ObjectMapper objectMapper,
                                      EnvironmentVariables environmentVariables) {
        this.attiniStepLoader = requireNonNull(attiniStepLoader, "attiniStepLoader");
        this.deployData = requireNonNull(deployData, "deployData");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    public DeploymentPlanDefinition createDefinition(DeploymentPlan deploymentPlan, boolean shouldAddSam) {

        ObjectNode states = getDeployDataState(deploymentPlan, shouldAddSam);

        DeploymentPlanStates deploymentPlanSteps = transformStates(objectMapper.valueToTree(deploymentPlan.getStates()),
                                                                   false, "DeploymentPlan");
        states.setAll((ObjectNode) deploymentPlanSteps.states());

        return new DeploymentPlanDefinition(Map.of("StartAt", GET_DEPLOY_DATA_KEY, "States", states),
                                            deploymentPlanSteps.attiniSteps());
    }

    private ObjectNode getDeployDataState(DeploymentPlan deploymentPlan,
                                          boolean shouldAddSam) {
        ObjectNode states = objectMapper.valueToTree(deploymentPlan.getStates());
        if (shouldAddSam) {
            logger.info("Adding sam package to deployment plan");
            states.set(GET_DEPLOY_DATA_KEY, deployData.getDeployData(SAM_PACKAGE_CHOICE_KEY));

            Map<String, Object> runner = Map.of("Next",
                                                deploymentPlan.getStartAt(),
                                                "Properties",
                                                Map.of("Runner",
                                                       "AttiniDefaultRunner",
                                                       "Commands",
                                                       List.of("chmod +x attini_data/sam-package.sh",
                                                               "./attini_data/sam-package.sh")));
            JsonNode samPackage = attiniStepLoader.getAttiniRunner(objectMapper.valueToTree(runner),
                                                                   SAM_PACKAGE_KEY);
            states.set(SAM_PACKAGE_CHOICE_KEY, getSamStuffChoice(deploymentPlan.getStartAt()));
            states.set(SAM_PACKAGE_KEY, samPackage);

        } else {
            logger.info("Not adding sam package to deployment plan");
            states.set(GET_DEPLOY_DATA_KEY, deployData.getDeployData(deploymentPlan.getStartAt()));

        }
        return states;
    }

    private DeploymentPlanStates transformStates(JsonNode originalStates,
                                                 boolean isMap,
                                                 String stepName) {
        List<AttiniStep> attiniMangedSteps = new ArrayList<>();

        ObjectNode states = originalStates.deepCopy();

        Iterator<Map.Entry<String, JsonNode>> fields = states.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode step = entry.getValue();
            if (step == null) {
                throw new TransformDeploymentPlanException("Step with name " + entry.getKey() + " is empty");
            }
            if (!step.has(TYPE_KEY)) {
                throw new TransformDeploymentPlanException("Type is missing for step " + entry.getKey());
            }

            switch (step.get(TYPE_KEY).textValue()) {
                case "AttiniCfn" -> {
                    if (isMap) {
                        states.set(entry.getKey(), attiniStepLoader.getAttiniMapCfn(step, entry.getKey()));
                    } else {
                        states.set(entry.getKey(), attiniStepLoader.getAttiniCfn(step, entry.getKey()));
                    }
                    attiniMangedSteps.add(new AttiniStep(entry.getKey(), "AttiniCfn"));

                }
                case "AttiniSam" -> {
                    JsonNode newStep = step.deepCopy();
                    ObjectNode properties =  (ObjectNode) newStep.path("Properties");
                    JsonNode project = properties.path("Project");
                    if (project.isMissingNode()){
                        throw new IllegalArgumentException("Project is missing in Sam step: " + entry.getKey());
                    }
                    if (project.path("Path").isMissingNode()){
                        throw new IllegalArgumentException("Project.Path is missing in Sam step: " + entry.getKey());
                    }

                    if (!properties.path("Region").isMissingNode() && !environmentVariables.getRegion().equals(properties.path("Region").asText())){
                        throw new IllegalArgumentException("Cross region deployment is not supported for AttiniSam steps. Step: " + entry.getKey());

                    }

                    if (!project.path("Path").isTextual()){
                        throw new IllegalArgumentException("Project.Path in AttiniSam step should be a string, step: " + entry.getKey());
                    }

                    String rawPath = project.path("Path").textValue();
                    String path = rawPath.startsWith("/") ? rawPath : "/" + rawPath;
                    properties.put("Template",
                                   "../.sam-source" + path + "/template.yaml");
                    properties.remove("Project");
                    attiniMangedSteps.add(new AttiniStep(entry.getKey(), "AttiniSam"));

                    states.set(entry.getKey(), attiniStepLoader.getAttiniCfn(newStep, entry.getKey()));
                }
                case "AttiniMergeOutput" -> {
                    attiniMangedSteps.add(new AttiniStep(entry.getKey(), "AttiniMergeOutput"));
                    states.set(entry.getKey(), attiniStepLoader.getAttiniMergeOutput(step));
                }
                case "Parallel" -> {
                    ArrayNode branches = Streams.stream(getBranches(step, stepName))
                                                .map(branch -> {
                                                    DeploymentPlanStates states1 = transformStates(branch.get("States"),
                                                                                                   false,
                                                                                                   entry.getKey());
                                                    attiniMangedSteps.addAll(states1.attiniSteps());
                                                    ObjectNode value = branch.deepCopy();
                                                    value.set("States", states1.states());
                                                    return value;
                                                }).collect(Collector.of(objectMapper::createArrayNode,
                                                                        ArrayNode::add, ArrayNode::addAll));


                    ObjectNode newStep = step.deepCopy();
                    newStep.set("Branches", branches);
                    states.set(entry.getKey(), newStep);
                }
                case "AttiniMap" -> {
                    ObjectNode newStep = step.deepCopy();
                    newStep.put(TYPE_KEY, "Map");
                    newStep.put("InputPath", "$");
                    newStep.set("Parameters", getAttiniMapParameters());
                    ObjectNode iterator = getIterator(step, stepName);
                    DeploymentPlanStates deploymentPlanStates = transformStates(iterator.get("States"),
                                                                                true,
                                                                                entry.getKey());
                    iterator.set("States", deploymentPlanStates.states());
                    newStep.set("Iterator", iterator);
                    states.set(entry.getKey(), newStep);
                    attiniMangedSteps.addAll(deploymentPlanStates.attiniSteps());
                }
                case "Map" -> {
                    ObjectNode iterator = getIterator(step, stepName);
                    DeploymentPlanStates deploymentPlanStates = transformStates(iterator.get("States"),
                                                                                true,
                                                                                entry.getKey());
                    ObjectNode newStep = step.deepCopy();
                    iterator.set("States", deploymentPlanStates.states());
                    newStep.set("Iterator", iterator);
                    states.set(entry.getKey(), newStep);
                    attiniMangedSteps.addAll(deploymentPlanStates.attiniSteps());
                }
                case "AttiniLambdaInvoke" -> {
                    attiniMangedSteps.add(new AttiniStep(entry.getKey(), "AttiniLambdaInvoke"));
                    states.set(entry.getKey(),
                               attiniStepLoader.getAttiniLambdaInvoke(step, entry.getKey()));
                }
                case "AttiniRunnerJob" -> {
                    attiniMangedSteps.add(new AttiniStep(entry.getKey(), "AttiniRunnerJob"));
                    states.set(entry.getKey(),
                               attiniStepLoader.getAttiniRunner(step, entry.getKey()));
                }
                case "AttiniCdk" -> {
                    attiniMangedSteps.add(new AttiniStep(entry.getKey(), "AttiniCdk"));
                    states.set(entry.getKey(),
                               attiniStepLoader.getAttiniCdk(step, entry.getKey()));
                }
                case "AttiniImport" -> {
                    attiniMangedSteps.add(new AttiniStep(entry.getKey(), "AttiniImport"));
                    states.set(entry.getKey(), attiniStepLoader.getAttiniImport(step, entry.getKey()));
                }
                case "AttiniManualApproval" -> {
                    attiniMangedSteps.add(new AttiniStep(entry.getKey(), "AttiniManualApproval"));
                    states.set(entry.getKey(),
                               attiniStepLoader.getAttiniManualApproval(step, entry.getKey()));
                }

            }

        }
        return new DeploymentPlanStates(attiniMangedSteps, states);

    }

    private ObjectNode getAttiniMapParameters() {
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("ConfigFile.$", "$$.Map.Item.Value");
        parameters.put("customData.$", "$.customData");
        parameters.put("dependencies.$", "$.dependencies");
        parameters.put("output.$", "$.output");
        parameters.put("deploymentOriginData.$", "$.deploymentOriginData");
        parameters.put("environment.$", "$.environment");
        parameters.put("stackParameters.$", "$.stackParameters");

        return parameters;
    }

    private ObjectNode getIterator(JsonNode step, String stepName) {
        final String iterator = "Iterator";
        if (step.path(iterator).isMissingNode()) {
            throw new TransformDeploymentPlanException("Iterator is missing in map step " + stepName);

        }
        try {
            return step.get(iterator).deepCopy();
        } catch (ClassCastException e) {
            throw new TransformDeploymentPlanException("Illegal format of iterator in step " + stepName);

        }
    }

    private ArrayNode getBranches(JsonNode step, String stepName) {
        JsonNode branches = step.path("Branches");
        if (branches.isMissingNode()) {
            throw new TransformDeploymentPlanException("Branches is missing in parallel step " + stepName);
        }
        if (!branches.isArray()) {
            throw new TransformDeploymentPlanException("Illegal format of branches in step " + stepName);
        }

        return (ArrayNode) branches;
    }

    private JsonNode getSamStuffChoice(String defaultNext) {
        return objectMapper.valueToTree(Map.of("Type",
                                               "Choice",
                                               "Choices",
                                               List.of(Map.of("Variable",
                                                              "$.deploymentOriginData.samPackaged",
                                                              "BooleanEquals",
                                                              false,
                                                              "Next",
                                                              SAM_PACKAGE_KEY)),
                                               "Default", defaultNext));
    }
}
