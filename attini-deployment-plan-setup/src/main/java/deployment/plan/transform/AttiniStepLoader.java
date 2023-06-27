/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package deployment.plan.transform;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import deployment.plan.system.EnvironmentVariables;

public class AttiniStepLoader {

    private static final Logger logger = Logger.getLogger(AttiniStepLoader.class);
    private final TemplateFileLoader templateFileLoader;
    private final ObjectMapper objectMapper;
    private final EnvironmentVariables environmentVariables;


    public AttiniStepLoader(TemplateFileLoader templateFileLoader,
                            ObjectMapper objectMapper,
                            EnvironmentVariables environmentVariables) {
        this.templateFileLoader = requireNonNull(templateFileLoader, "templateFileLoader");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    public JsonNode getAttiniMapCfn(JsonNode originalStep, String stepName) {

        return getAttiniMapStep(originalStep, templateFileLoader.getAttiniMapCfnTemplate(), stepName);
    }


    public JsonNode getAttiniCfn(JsonNode originalStep, String stepName) {

        return getAttiniCfnStep(originalStep, templateFileLoader.getAttiniCfnTemplate(), stepName);
    }

    public JsonNode getAttiniLambdaInvoke(JsonNode originalStep, String stepName) {
        return getAttiniLambdaInvokeStep(originalStep, templateFileLoader.getAttiniLambdaInvokeTemplate(), stepName);

    }

    public JsonNode getAttiniRunner(JsonNode originalStep, String stepName, String defaultRunner) {
        return getAttiniRunnerStep(originalStep, templateFileLoader.getAttiniRunnerTemplate(), stepName, defaultRunner);

    }

    public Map<AttiniStep, JsonNode> getAttiniSam(JsonNode originalStep,
                                                  String stepName,
                                                  Map<String, String> nextReplacements,
                                                  String defaultRunner) {

        String packageStepName = stepName + "PackageSam";

        nextReplacements.put(stepName, packageStepName);
        String tempKey = DigestUtils.md5Hex(stepName);
        nextReplacements.put(tempKey, stepName);

        JsonNode newStep = originalStep.deepCopy();

        ObjectNode properties = (ObjectNode) newStep.path("Properties");
        JsonNode project = properties.path("Project");
        if (project.isMissingNode()) {
            throw new IllegalArgumentException("Project is missing in Sam step: " + stepName);
        }
        if (project.path("Path").isMissingNode() && project.path("Path.$").isMissingNode()) {
            throw new IllegalArgumentException("Project.Path is missing in Sam step: " + stepName);
        }

        if (!properties.path("Region").isMissingNode() && !environmentVariables.getRegion()
                                                                               .equals(properties.path(
                                                                                                         "Region")
                                                                                                 .asText())) {
            throw new IllegalArgumentException(
                    "Cross region deployment is not supported for AttiniSam steps. Step: " + stepName);

        }

        if (!project.path("Path").isTextual() && !project.path("Path.$").isTextual()) {
            throw new IllegalArgumentException("Project.Path in AttiniSam step should be a string, step: " + stepName);
        }

        Map<AttiniStep, JsonNode> result = new HashMap<>();

        ObjectNode packageSamStep = (ObjectNode) getAttiniRunnerStep(newStep,
                                                      templateFileLoader.getAttiniSamTemplate(),
                                                      packageStepName,
                                                      defaultRunner);

        packageSamStep.remove("End");

        result.put(new AttiniStep(packageStepName, "AttiniPackageSam"), packageSamStep.put("Next", tempKey));

        properties.put("Template.$",
                       "$.output."+packageStepName+".result");
        result.put(new AttiniStep(stepName, "AttiniCfn"), getAttiniCfn(newStep, stepName));

        return result;

    }

    private String getPathKey(JsonNode jsonNode){
        if (!jsonNode.path("Properties").path("Path.$").isMissingNode()){
            return "Path.$";
        }

        return "Path";
    }

    public Map<AttiniStep, JsonNode> getAttiniCdk(JsonNode originalStep,
                                                  String stepName,
                                                  Map<String, String> nextReplacements, String defaultRunner) {
        String pathKey = getPathKey(originalStep);
        JsonNode path = originalStep.path("Properties").path(pathKey);
        if (path.isMissingNode() || path.asText().isBlank()) {
            ObjectNode properties = (ObjectNode) originalStep.get("Properties");
            properties.put(pathKey, "./");
        } else if (path.asText().startsWith("/")) {
            ObjectNode properties = (ObjectNode) originalStep.get("Properties");
            properties.put(pathKey, "." + properties.get(pathKey).asText());
        }


        JsonNode diffNode = originalStep.path("Properties").path("Diff");

        if (!diffNode.isMissingNode() && diffNode.isValueNode()) {
            throw new IllegalArgumentException("\"Diff\" in the AttiniCdk step should be an object.");

        }
        JsonNode diffEnabled = diffNode.path("Enabled");

        if (!diffNode.isMissingNode() && diffEnabled.isMissingNode()) {
            throw new IllegalArgumentException("\"Diff.Enabled\" is required in the CDK step if \"Diff\" is specified.");

        }

        if (!diffEnabled.isMissingNode() && !diffEnabled.isBoolean()) {
            throw new IllegalArgumentException("\"Diff.Enabled\" in the AttiniCdk step should be a boolean.");
        }
        if (diffEnabled.booleanValue()) {
            String diffStepName = stepName + "CdkDiff";
            String choiceName = stepName + "ApproveChanges?";
            String approvalName = stepName + "ApproveChanges";
            nextReplacements.put(stepName, diffStepName);
            String tempKey = DigestUtils.md5Hex(stepName);
            nextReplacements.put(tempKey, stepName);


            Map<AttiniStep, JsonNode> steps = new HashMap<>();
            // add diff check
            ObjectNode diffStep = originalStep.deepCopy();
            diffStep.put("Next", choiceName);

            steps.put(new AttiniStep(diffStepName, "AttiniRunnerJob"), getAttiniRunnerStep(diffStep,
                                                                                           templateFileLoader.getAttiniCdkChangesetTemplate(),
                                                                                           diffStepName,
                                                                                           defaultRunner));

            // add choice
            ObjectNode choice = objectMapper.createObjectNode();
            choice.put("Type", "Choice")
                  .set("Choices", objectMapper.createArrayNode()
                                              .add(objectMapper.createObjectNode()
                                                               .put("Variable",
                                                                    "$.output." + diffStepName + ".result")
                                                               .put("StringEquals", "change-detected")
                                                               .put("Next", approvalName)));


            choice.put("Default", tempKey);

            steps.put(new AttiniStep(choiceName, "Choice"), choice);


            ObjectNode manualApprovalStep = objectMapper.createObjectNode()
                                                        .put("Next", tempKey);

            steps.put(new AttiniStep(approvalName, "AttiniManualApproval"),
                      getAttiniManualApproval(manualApprovalStep, approvalName));

            //add cdk step
            steps.put(new AttiniStep(stepName, "AttiniCdk"), getAttiniRunnerStep(originalStep,
                                                                                 templateFileLoader.getAttiniCdkTemplate(),
                                                                                 stepName, defaultRunner));
            return steps;
        }

        return Map.of(new AttiniStep(stepName, "AttiniCdk"),
                      getAttiniRunnerStep(originalStep,
                                          templateFileLoader.getAttiniCdkTemplate(),
                                          stepName,
                                          defaultRunner));
    }

    public JsonNode getAttiniImport(JsonNode originalStep, String stepName) {
        return getAttiniImportStep(originalStep, templateFileLoader.getAttiniImportTemplate(), stepName);

    }

    public JsonNode getAttiniManualApproval(JsonNode originalStep, String stepName) {
        return getAttiniManualApprovalStep(originalStep,
                                           templateFileLoader.getAttiniManualApprovalTemplate(),
                                           stepName);

    }

    public JsonNode getAttiniMergeOutput(JsonNode originalStep) {

        return getAttiniMergeStep(originalStep, templateFileLoader.getAttiniMergeOutputTemplate());
    }

    private JsonNode getAttiniLambdaInvokeStep(JsonNode originalStep,
                                               File template, String stepName) {
        try {

            if (originalStep.get("Parameters") == null) {
                throw new IllegalArgumentException(
                        "Invalid step definition: Parameters with at least a FunctionName field is mandatory for AttiniLambdaInvoke steps, stepName = " + stepName);
            }

            if (!(originalStep.get("Parameters").isObject())) {
                throw new IllegalArgumentException(
                        "Invalid step definition: Parameters in AttiniLambdaInvoke needs to be an object, stepName = " + stepName);
            }


            ObjectNode parameters = objectMapper.valueToTree(originalStep.get("Parameters"));

            if (parameters.path("Payload").isMissingNode() && parameters.path("Payload.$").isMissingNode()) {

                parameters.put("Payload.$", "$");
            }

            JsonNode node = initNewStep(originalStep, template, stepName);

            ((ObjectNode) node).set("Parameters", parameters);

            return node;
        } catch (IOException e) {
            logger.error("could not parse Attini cfn json", e);
            throw new RuntimeException(e);
        }
    }

    private JsonNode getAttiniManualApprovalStep(JsonNode originalStep,
                                                 File template, String stepName) {
        try {
            return initNewStep(originalStep, template, stepName);
        } catch (IOException e) {
            logger.error("could not parse Attini manual approval json", e);
            throw new UncheckedIOException(e);
        }
    }


    private JsonNode getAttiniRunnerStep(JsonNode originalStep,
                                         File template,
                                         String stepName, String defaultRunner) {
        try {

            JsonNode nodeCopy = originalStep.deepCopy();

            if (originalStep.path("Properties").isMissingNode()) {
                throw new IllegalArgumentException("No Properties specified for step: " + stepName);
            }
            if (originalStep
                    .path("Properties")
                    .path("Runner")
                    .isMissingNode()) {
                ObjectNode properties = (ObjectNode) nodeCopy.path("Properties");
                properties.put("Runner", defaultRunner);
                return initNewStep(nodeCopy, template, stepName);
            }

            return initNewStep(nodeCopy, template, stepName);
        } catch (IOException e) {
            logger.error("could not parse Attini cfn json", e);
            throw new UncheckedIOException(e);
        }
    }

    private JsonNode getAttiniImportStep(JsonNode originalStep,
                                         File template,
                                         String stepName) {
        try {
            if (!originalStep.path("Properties").isMissingNode()) {
                JsonNode properties = objectMapper.valueToTree(originalStep)
                                                  .path("Properties");

                if (properties.path("Source").isMissingNode() || properties.path("Source").isEmpty()) {
                    throw new IllegalArgumentException("No Source specified for AttiniImport step. Step name: " + stepName);
                }
                if (properties.path("SourceType").isMissingNode()) {
                    throw new IllegalArgumentException("No SourceType specified for AttiniImport step. Step name: " + stepName);
                }

                if (properties.path("Mapping").isMissingNode() || properties.path("Mapping").isEmpty()) {
                    throw new IllegalArgumentException("No Mapping specified for AttiniImport step. Step name: " + stepName);
                }

            } else {
                throw new IllegalArgumentException("No Properties specified for step: " + stepName);

            }
            return initNewStep(originalStep, template, stepName);
        } catch (IOException e) {
            logger.error("could not parse Attini cfn json", e);
            throw new UncheckedIOException(e);
        }
    }


    private JsonNode getAttiniMergeStep(JsonNode originalStep,
                                        File template) {
        try {
            ObjectNode node = (ObjectNode) objectMapper.readTree(template);

            if (!originalStep.path("Next").isMissingNode()) {
                node.set("Next", originalStep.get("Next"));
            } else {
                node.put("End", Boolean.TRUE);
            }
            return node;
        } catch (IOException e) {
            logger.error("could not parse Attini merge json", e);
            throw new UncheckedIOException(e);
        }
    }


    private JsonNode getAttiniMapStep(JsonNode originalStep,
                                      File template, String stepName) {
        try {
            return initNewStep(originalStep, template, stepName);
        } catch (IOException e) {
            logger.error("could not parse Attini cfn json", e);
            throw new UncheckedIOException(e);
        }
    }

    private JsonNode getAttiniCfnStep(JsonNode originalStep,
                                      File template, String stepName) {
        try {

            if (originalStep.path("Properties").isMissingNode()) {
                throw new IllegalArgumentException("No Properties found for AttiniCfn step: " + stepName);
            }
            if (originalStep.path("Properties").path("StackName").isMissingNode() && originalStep.path("Properties")
                                                                                                 .path("ConfigFile")
                                                                                                 .isMissingNode()) {
                throw new IllegalArgumentException("No StackName or ConfigFile configured for AttiniCfn step: " + stepName);
            }

            return initNewStep(originalStep, template, stepName);
        } catch (IOException e) {
            logger.error("could not parse Attini cfn json", e);
            throw new UncheckedIOException(e);
        }
    }

    private JsonNode initNewStep(JsonNode originalStep,
                                 File template,
                                 String stepName) throws IOException {
        ObjectNode newStep = (ObjectNode) objectMapper.readTree(template);
        if (!originalStep.path("Next").isMissingNode()) {
            newStep.set("Next", originalStep.get("Next"));
        } else {
            newStep.put("End", Boolean.TRUE);
        }

        originalStep.fields()
                    .forEachRemaining(field -> {
                                          if (!field.getKey().equals("Type") &&
                                              !field.getKey().equals("Next") &&
                                              !field.getKey().equals("End") &&
                                              !field.getKey().equals("Properties")) {
                                              newStep.set(field.getKey(), field.getValue());
                                          }
                                      }
                    );

        newStep.put("ResultPath", "$.output." + stepName);


        JsonNode payload = newStep.path("Parameters")
                                  .path("Payload");

        if (!originalStep.path("Properties").isMissingNode() && !payload.isMissingNode()) {
            if (!payload.path("Properties").isMissingNode()) {
                ObjectNode properties = (ObjectNode) payload.path("Properties");
                properties.setAll((ObjectNode) originalStep.path("Properties"));
            } else {
                ObjectNode mutablePayload = (ObjectNode) payload;
                mutablePayload.set("Properties", originalStep.get("Properties"));
            }
        }

        return newStep;
    }
}
