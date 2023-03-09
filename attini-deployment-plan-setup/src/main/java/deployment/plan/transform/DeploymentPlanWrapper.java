package deployment.plan.transform;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeploymentPlanWrapper {

    private final String deploymentPlanName;
    private final DeploymentPlanResource deploymentPlanResource;
    private final boolean containsSamSteps;

    private final boolean containsRunnerStepsWithoutRunner;


    public DeploymentPlanWrapper(String deploymentPlanName,
                                 DeploymentPlanResource deploymentPlanResource,
                                 ObjectMapper objectMapper) {
        this.deploymentPlanName = requireNonNull(deploymentPlanName, "name");
        this.deploymentPlanResource = requireNonNull(deploymentPlanResource, "deploymentPlanResource");
        JsonNode source = objectMapper.valueToTree(deploymentPlanResource.getDeploymentPlanProperties()
                                                                         .getDeploymentPlan());
        this.containsSamSteps = findSteps(source,
                                          "AttiniSam")
                .findAny()
                .isPresent();
        this.containsRunnerStepsWithoutRunner = containsStepWithoutRunner(source);
    }


    private boolean containsStepWithoutRunner(JsonNode source) {
        return Stream.of(findSteps(source, "AttiniRunnerJob"), findSteps(source, "AttiniCdk"))
                     .flatMap(Function.identity())
                     .anyMatch(entry -> entry.getValue()
                                             .path("Properties")
                                             .path("Runner")
                                             .isMissingNode());
    }

    public String getDeploymentPlanName() {
        return deploymentPlanName;
    }

    public DeploymentPlanResource getDeploymentPlanResource() {
        return deploymentPlanResource;
    }

    public boolean containsSamSteps() {
        return containsSamSteps;
    }

    public boolean shouldDeployDefaultRunner() {
        return containsSamSteps || containsRunnerStepsWithoutRunner;
    }

    private static Stream<Map.Entry<String, JsonNode>> findSteps(JsonNode source, String type) {

        Iterable<Map.Entry<String, JsonNode>> iterable = getFieldIterable(source.path("States"));

        return StreamSupport.stream(iterable.spliterator(), false)
                            .flatMap(entry -> {
                                JsonNode node = entry.getValue();
                                if (node.path("Type").asText().equals("Parallel")) {
                                    return StreamSupport.stream(node.path("Branches").spliterator(), false)
                                                        .flatMap(jsonNode -> findSteps(jsonNode, type));

                                } else if (type.equals(node.path("Type").asText())) {
                                    return Stream.of(entry);
                                }
                                return Stream.empty();
                            });
    }

    private static Iterable<Map.Entry<String, JsonNode>> getFieldIterable(JsonNode source) {
        return source::fields;
    }

}
