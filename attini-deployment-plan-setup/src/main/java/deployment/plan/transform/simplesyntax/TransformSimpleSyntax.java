package deployment.plan.transform.simplesyntax;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TransformSimpleSyntax {


    private static final String IS_TRUE_KEY = "IsTrue";
    private static final String IS_FALSE_KEY = "IsFalse";
    private static final String NAME_KEY = "Name";
    private static final String TYPE_KEY = "Type";
    private static final String NEXT_KEY = "Next";
    private static final String END_KEY = "End";
    private static final String BRANCHES_KEY = "Branches";
    private final ObjectMapper objectMapper;

    public TransformSimpleSyntax(ObjectMapper objectMapper) {
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
    }

    public JsonNode transform(JsonNode steps) {


        List<JsonNode> stepsList = toList(steps);
        if (stepsList.isEmpty()){
            throw new IllegalArgumentException(
                    "Error in DeploymentPlan. The DeploymentPlan can not be empty");

        }

        if (stepsList.get(0).path(NAME_KEY).isMissingNode()) {
            throw new IllegalArgumentException(
                    "Error in DeploymentPlan. Field \""+NAME_KEY+"\" is missing in the first step");

        }
        return toStates(objectMapper, stepsList, new HashSet<>());
    }

    private static Stream<JsonNode> toStream(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false);
    }


    private static List<JsonNode> toList(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false).toList();
    }

    private static ObjectNode toStates(ObjectMapper objectMapper,
                                       List<JsonNode> result,
                                       Set<String> names) {

        JsonNode startAt = result.get(0).path(NAME_KEY);
        ObjectNode objectNode = objectMapper.createObjectNode();

        objectNode.put("StartAt", startAt.asText());

        ObjectNode states = objectMapper.createObjectNode();

        transformSteps(objectMapper,
                       result,
                       names, null)
                .forEach(step -> {
                    if (!names.add(step.name())) {
                        throw new IllegalArgumentException(
                                "Duplicated step name detected. Step names must be unique. Name: " + step.name());
                    }
                    states.set(step.name(), step.jsonNode());
                });
        return objectNode.set("States", states);
    }

    private static List<Step> transformSteps(ObjectMapper objectMapper,
                                             List<JsonNode> result,
                                             Set<String> names,
                                             Step optionalNextStep) {

        List<Step> allSteps = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) {
            Step nextStep = getNextStep(i, result);
            allSteps.addAll(transformStep(objectMapper,
                                          result.get(i),
                                          names,
                                          nextStep == null ? optionalNextStep : nextStep));
        }

        return allSteps;
    }

    private static Step getNextStep(int currentIndex, List<JsonNode> steps) {
        if (currentIndex == steps.size() - 1) {
            return null;
        } else {
            JsonNode step = steps.get(currentIndex + 1);
            JsonNode name = step.path(NAME_KEY);
            if (name.isMissingNode()) {
                throw new IllegalArgumentException(
                        "Error when transforming step: %s. Field \"%s\" is missing in the following step".formatted(steps.get(
                                currentIndex).path(NAME_KEY).asText(), NAME_KEY));

            }
            if (!name.isTextual()) {
                throw new IllegalArgumentException(
                        "Error when transforming step: %s. Field \"%s\" in the following step is not a string".formatted(steps.get(
                                currentIndex).path(NAME_KEY).asText(), NAME_KEY));

            }
            return new Step( name.asText(),step);
        }
    }


    private static List<Step> transformStep(ObjectMapper objectMapper,
                                            JsonNode value,
                                            Set<String> names,
                                            Step nextStep) {
        if (!value.path(NEXT_KEY).isMissingNode() || !value.path(END_KEY).isMissingNode()) {
            throw new IllegalArgumentException(
                    "\"Next\" and \"End\" is not allowed when using the deployment plan simple syntax. Step name: " + value.path(
                            NAME_KEY).asText());
        }
        if ("Parallel".equals(value.path(TYPE_KEY).asText())) {
            return transformParallel(objectMapper, value, names, nextStep);
        }
        if ("Choice".equals(value.path(TYPE_KEY).asText())) {
            return transformChoiceStep(objectMapper, value, names, nextStep);
        }
        return List.of(transformGenericStep(value, nextStep));
    }

    private static List<Step> transformChoiceStep(ObjectMapper objectMapper,
                                                  JsonNode value,
                                                  Set<String> names,
                                                  Step nextStep) {
        ObjectNode node = objectMapper.createObjectNode();
        ObjectNode condition = value.get("Condition").deepCopy();
        JsonNode trueBranch = value.path(IS_TRUE_KEY);
        List<String> allowedKeys = List.of(NAME_KEY,TYPE_KEY,IS_TRUE_KEY, IS_FALSE_KEY, "Condition");
        value.fieldNames().forEachRemaining(s -> {
            if (!allowedKeys.contains(s)) {
                throw new IllegalArgumentException("Error in step " + value.get(NAME_KEY)
                                                                           .asText() + ": Key " + s + " is not allowed for Choice. Allowed keys are: " + String.join(
                        ",",
                        allowedKeys));
            }
        });
        if (trueBranch.isMissingNode()) {
            throw new IllegalArgumentException("Error in step " + value.get(NAME_KEY)
                                                                       .asText() + ": No \""+IS_TRUE_KEY+"\" field specified for Choice.");
        }

        if (!trueBranch.isArray()) {
            throw new IllegalArgumentException("Error in step " + value.get(NAME_KEY)
                                                                       .asText() + ": \""+IS_TRUE_KEY+"\" field in Choice should be an array");
        }

        if (trueBranch.get(0).path(NAME_KEY).isMissingNode()) {
            throw new IllegalArgumentException("Error in step " + value.get(NAME_KEY)
                                                                       .asText() + ": First step in \""+IS_TRUE_KEY+"\" branch in choice is missing \"Name\" ");
        }
        condition.set(NEXT_KEY, trueBranch.get(0).get(NAME_KEY));
        node.put(TYPE_KEY, "Choice");
        node.set("Choices", objectMapper.createArrayNode().add(condition));
        ArrayList<Step> steps = new ArrayList<>();
        steps.add(new Step(value.get(NAME_KEY).asText(), node));
        steps.addAll(transformSteps(objectMapper, toList(trueBranch), names, nextStep));
        JsonNode falseBranch = value.path(IS_FALSE_KEY);

        if (!falseBranch.isMissingNode() && !falseBranch.isArray()) {
            throw new IllegalArgumentException("Error in step " + value.get(NAME_KEY)
                                                                       .asText() + ": \""+IS_FALSE_KEY+"\" field in Choice should be an array");
        }
        if (!falseBranch.isMissingNode()) {
            JsonNode name = falseBranch.get(0).path(NAME_KEY);
            if (name.isMissingNode()) {
                throw new IllegalArgumentException("Error in step " + value.get(NAME_KEY)
                                                                           .asText() + ": First step in \""+IS_FALSE_KEY+"\" branch in choice is missing \"Name\" ");
            }
            steps.addAll(transformSteps(objectMapper, toList(falseBranch), names, nextStep));

            node.set("Default", name);
        } else if (nextStep != null) {
            node.put("Default", nextStep.name());
        } else {
            String name = value.get(NAME_KEY).asText() + "-End";
            node.put("Default", name);
            steps.add(new Step(name,
                               objectMapper.createObjectNode().put(TYPE_KEY, "Pass").put(END_KEY, true)));

        }
        return steps;
    }

    private record Step(String name, JsonNode jsonNode) {
    }

    private static Step transformGenericStep(JsonNode value, Step nextStep) {
        ObjectNode node = value.deepCopy();
        node.remove(NAME_KEY);
        if (nextStep != null) {
            node.put(NEXT_KEY, nextStep.name());
        } else {
            node.put(END_KEY, true);
        }
        return new Step(value.get(NAME_KEY).asText(), node);
    }

    private static List<Step> transformParallel(ObjectMapper objectMapper,
                                          JsonNode value,
                                          Set<String> names,
                                          Step nextStep) {
        List<ObjectNode> branches = toStream(value.path(BRANCHES_KEY))
                .map(branch -> {
                    if (!branch.isArray()){
                        throw new IllegalArgumentException(
                                "Error in parallel step " + value.get(NAME_KEY) + ", each branch should be an array.");
                    }
                    if (branch.get(0).path(NAME_KEY).isMissingNode()) {
                        throw new IllegalArgumentException(
                                "Error in a branch in parallel step " + value.get(NAME_KEY) + ", field \""+NAME_KEY+"\" is missing in the first step");

                    }
                    return toStates(objectMapper, toList(branch), names);
                })
                .toList();
        ObjectNode parallelStep = objectMapper.createObjectNode();
        parallelStep.put(TYPE_KEY, "Parallel");
        parallelStep.set(BRANCHES_KEY, objectMapper.createArrayNode().addAll(branches));

        if (nextStep == null || !nextStep.jsonNode().path(TYPE_KEY).asText().equals("AttiniMergeOutput")){
            Step mergeStep = createMergeStep(objectMapper, value.get(NAME_KEY).asText(), nextStep);
            parallelStep.put("Next", mergeStep.name());
            return List.of(new Step(value.get(NAME_KEY).asText(), parallelStep),mergeStep);
        }
        parallelStep.put(NEXT_KEY, nextStep.name());
        return List.of(new Step(value.get(NAME_KEY).asText(), parallelStep));

    }
    private static Step createMergeStep(ObjectMapper objectMapper,
                                        String namePrefix,
                                        Step nextStep){
        ObjectNode mergeStep = objectMapper.createObjectNode();
        mergeStep.put("Type", "AttiniMergeOutput");
        if (nextStep == null){
            mergeStep.put(END_KEY, true);
        }else {
            mergeStep.put(NEXT_KEY, nextStep.name());
        }
        return new Step(namePrefix + "-Merge", mergeStep);
    }

}
