package deployment.plan.transform;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;

public class DeploymentPlanStepsCreator {

    private static final Logger logger = Logger.getLogger(DeploymentPlanStepsCreator.class);


    private static final String GET_DEPLOY_DATA_KEY = "AttiniPrepareDeployment";

    private static final String TYPE_KEY = "Type";
    private final AttiniStepLoader attiniStepLoader;
    private final DeployData deployData;
    private final ObjectMapper objectMapper;
    public DeploymentPlanStepsCreator(AttiniStepLoader attiniStepLoader,
                                      DeployData deployData,
                                      ObjectMapper objectMapper) {
        this.attiniStepLoader = requireNonNull(attiniStepLoader, "attiniStepLoader");
        this.deployData = requireNonNull(deployData, "deployData");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
    }

    public DeploymentPlanDefinition createDefinition(DeploymentPlanProperties deploymentPlanProperties) {


        ObjectNode states = objectMapper.createObjectNode();

        states.setAll(getDeployDataState(deploymentPlanProperties));
        states.setAll((ObjectNode) objectMapper.valueToTree(deploymentPlanProperties.getDeploymentPlan().getStates()));
        DeploymentPlanStates deploymentPlanSteps = transformStates(states,
                                                                   false,
                                                                   "DeploymentPlan",
                                                                   deploymentPlanProperties.getDefaultRunner(),
                                                                   GET_DEPLOY_DATA_KEY);

        return new DeploymentPlanDefinition(Map.of("StartAt",
                                                   deploymentPlanSteps.startAt(),
                                                   "States",
                                                   deploymentPlanSteps.states()),
                                            deploymentPlanSteps.attiniSteps());
    }

    private ObjectNode getDeployDataState(DeploymentPlanProperties deploymentPlanProperties) {

        DeploymentPlan deploymentPlan = deploymentPlanProperties.getDeploymentPlan();
        ObjectNode states = objectMapper.valueToTree(deploymentPlan.getStates());
        states.set(GET_DEPLOY_DATA_KEY, deployData.getDeployData(deploymentPlan.getStartAt()));
        return states;
    }

    private DeploymentPlanStates transformStates(JsonNode originalStates,
                                                 boolean isMap,
                                                 String stepName,
                                                 String defaultRunner,
                                                 String startAt) {
        logger.info("Transforming states for branch");

        List<AttiniStep> attiniMangedSteps = new ArrayList<>();

        ObjectNode states = objectMapper.createObjectNode();

        Iterator<Map.Entry<String, JsonNode>> fields = originalStates.fields();

        Map<String, String> nextReplacements = new HashMap<>();

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

                    Map<AttiniStep, JsonNode> attiniSamSteps = attiniStepLoader.getAttiniSam(step,
                                                                                             entry.getKey(),
                                                                                             nextReplacements,
                                                                                             defaultRunner);
                    attiniMangedSteps.addAll(attiniSamSteps.keySet());
                    states.setAll(attiniSamSteps.entrySet()
                                                .stream()
                                                .collect(Collectors.toMap(t -> t.getKey().name(),
                                                                          Map.Entry::getValue)));

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
                                                                                                   entry.getKey(),
                                                                                                   defaultRunner, branch.get("StartAt")
                                                                                                                        .asText());
                                                    attiniMangedSteps.addAll(states1.attiniSteps());
                                                    ObjectNode value = branch.deepCopy();
                                                    value.put("StartAt", states1.startAt());
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
                                                                                entry.getKey(), defaultRunner,
                                                                                iterator.get("StartAt").asText());
                    iterator.set("States", deploymentPlanStates.states());
                    newStep.set("Iterator", iterator);
                    states.set(entry.getKey(), newStep);
                    attiniMangedSteps.addAll(deploymentPlanStates.attiniSteps());
                }
                case "Map" -> {
                    ObjectNode iterator = getIterator(step, stepName);
                    DeploymentPlanStates deploymentPlanStates = transformStates(iterator.get("States"),
                                                                                true,
                                                                                entry.getKey(),
                                                                                defaultRunner,
                                                                                iterator.get("StartAt").asText());
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
                               attiniStepLoader.getAttiniRunner(step, entry.getKey(), defaultRunner));
                }
                case "AttiniCdk" -> {
                    Map<AttiniStep, JsonNode> attiniCdkSteps = attiniStepLoader.getAttiniCdk(step,
                                                                                             entry.getKey(),
                                                                                             nextReplacements,
                                                                                             defaultRunner);
                    attiniMangedSteps.addAll(attiniCdkSteps.keySet());
                    states.setAll(attiniCdkSteps.entrySet()
                                                .stream()
                                                .collect(Collectors.toMap(t -> t.getKey().name(),
                                                                          Map.Entry::getValue)));
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
                default -> states.set(entry.getKey(), step);

            }

        }

        states.fields()
              .forEachRemaining(entry -> {
                  if (nextReplacements.containsKey(entry.getValue().path("Next").asText())) {
                      ObjectNode objectNode = (ObjectNode) entry.getValue();
                      objectNode.put("Next", nextReplacements.get(entry.getValue().path("Next").asText()));
                  }
                  if (nextReplacements.containsKey(entry.getValue().path("Default").asText())) {
                      ObjectNode objectNode = (ObjectNode) entry.getValue();
                      objectNode.put("Default", nextReplacements.get(entry.getValue().path("Default").asText()));
                  }
              });

        if (nextReplacements.containsKey(startAt)){
            logger.info("Replacing startAt for branch, new value: " + nextReplacements.get(startAt));
            return new DeploymentPlanStates(attiniMangedSteps, states,nextReplacements.get(startAt));
        }
        return new DeploymentPlanStates(attiniMangedSteps, states,startAt);
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
}
