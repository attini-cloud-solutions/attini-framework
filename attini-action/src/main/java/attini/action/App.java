package attini.action;

import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.logging.Logger;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.actions.cdk.CdkRunnerAdapter;
import attini.action.actions.cdk.input.CdkInput;
import attini.action.actions.deploycloudformation.CfnHandler;
import attini.action.actions.deploycloudformation.input.AttiniCfnInput;
import attini.action.actions.getdeployorigindata.GetDeployOriginDataHandler;
import attini.action.actions.merge.MergeUtil;
import attini.action.actions.readoutput.ImportHandler;
import attini.action.actions.readoutput.input.ImportInput;
import attini.action.actions.runner.RunnerHandler;
import attini.action.actions.runner.input.RunnerInput;
import attini.action.configuration.InitDeployConfigurationHandler;
import attini.action.domain.DeploymentPlanExecutionMetadata;
import attini.action.domain.DeploymentPlanStateData;
import attini.action.facades.ArtifactStoreFacade;
import attini.action.facades.deployorigin.DeployOriginFacade;
import attini.action.facades.stackdata.DeploymentPlanDataFacade;
import attini.action.facades.stackdata.StackDataFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import attini.action.actions.runner.CouldNotParseInputException;
import attini.action.licence.LicenceAgreementHandler;
import attini.domain.DeployOriginData;
import attini.domain.DistributionContext;
import attini.domain.deserializers.AttiniDeserializationException;


@Named("app")
public class App implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final Logger logger = Logger.getLogger(App.class);
    private final CfnHandler cfnHandler;
    private final StepFunctionFacade stepFunctionFacade;
    private final GetDeployOriginDataHandler getDeployOriginDataHandler;
    private final DeployOriginFacade deployOriginFacade;
    private final SendUsageDataFacade sendUsageDataFacade;
    private final LicenceAgreementHandler licenceAgreementHandler;
    private final InitDeployConfigurationHandler initDeployConfigurationHandler;
    private final RunnerHandler runnerHandler;
    private final ObjectMapper quarkusMapper;
    private final ArtifactStoreFacade artifactStoreFacade;
    private final ImportHandler importHandler;
    private final StackDataFacade stackDataFacade;

    private final DeploymentPlanDataFacade deploymentPlanDataFacade;
    private final CdkRunnerAdapter cdkRunnerAdapter;

    @Inject
    public App(CfnHandler cfnHandler,
               StepFunctionFacade stepFunctionFacade,
               GetDeployOriginDataHandler getDeployOriginDataHandler,
               DeployOriginFacade deployOriginFacade,
               SendUsageDataFacade sendUsageDataFacade,
               LicenceAgreementHandler licenceAgreementHandler,
               InitDeployConfigurationHandler initDeployConfigurationHandler,
               RunnerHandler runnerHandler,
               ObjectMapper quarkusMapper,
               ArtifactStoreFacade artifactStoreFacade,
               ImportHandler importHandler,
               StackDataFacade stackDataFacade,
               DeploymentPlanDataFacade deploymentPlanDataFacade,
               CdkRunnerAdapter cdkRunnerAdapter) {
        this.cfnHandler = requireNonNull(cfnHandler, "deployCfnStackService");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "stepFunctionFacade");
        this.getDeployOriginDataHandler = requireNonNull(getDeployOriginDataHandler, "getDeployOriginDataHandler");
        this.deployOriginFacade = requireNonNull(deployOriginFacade, "deployOriginFacade");
        this.sendUsageDataFacade = requireNonNull(sendUsageDataFacade, "sendUsageDataFacade");
        this.licenceAgreementHandler = requireNonNull(licenceAgreementHandler, "licenceAgreementHandler");
        this.initDeployConfigurationHandler = requireNonNull(initDeployConfigurationHandler,
                                                             "initDeployConfigurationHandler");
        this.runnerHandler = requireNonNull(runnerHandler, "runnerHandler");
        this.quarkusMapper = requireNonNull(quarkusMapper, "quarkusMapper");
        this.artifactStoreFacade = requireNonNull(artifactStoreFacade, "artifactStoreFacade");
        this.importHandler = requireNonNull(importHandler, "readOutputHandler");
        this.stackDataFacade = requireNonNull(stackDataFacade, "stackDataFacade");
        this.deploymentPlanDataFacade = requireNonNull(deploymentPlanDataFacade, "deploymentPlanDataFacade");
        this.cdkRunnerAdapter = requireNonNull(cdkRunnerAdapter, "cdkRunnerAdapter");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {

        String inputString = toJsonString(input);
        logger.info("Got event: " + inputString);

        if (isCloudWatchEvent(input)) {
            handlePostPipelineHook(input);
            return input;
        }

        if (isCustomResource(input)) {
            String resourceType = (String) input.get("ResourceType");
            if (resourceType.equals("Custom::ConfigurationMutations")) {
                logger.info("Mutate configuration request triggered");
                initDeployConfigurationHandler.handleConfig(input);
            } else {
                logger.info("Accept licence agreement request triggered");
                licenceAgreementHandler.handleLicenceAgreement(input);
            }
            return input;
        }

        switch (input.get("attiniActionType").toString()) {
            case "DeployCfn" -> {
                logger.info("Deploying a Cloudformation stack");
                AttiniCfnInput attiniCfnInput = toInput(input, AttiniCfnInput.class);
                DeploymentPlanExecutionMetadata deploymentPlanExecutionMetadata = attiniCfnInput.deploymentPlanExecutionMetadata();
                try {
                    cfnHandler.deployCfn(attiniCfnInput);
                } catch (StackConfigException e) {
                    logger.error("Stack config error", e);
                    stepFunctionFacade.sendError(deploymentPlanExecutionMetadata.sfnToken(),
                                                 e.getMessage(),
                                                 "StackConfigError");
                }
            }
            case "AttiniMergeOutput" -> {
                logger.info("Merge input triggered");
                List<Map<String, Object>> inputsToMerge = (List<Map<String, Object>>) input.get("InputsToMerge");
                return MergeUtil.merge(inputsToMerge);
            }
            case "GetDeployOriginData" -> {
                logger.info("Get deploy data triggered");
                return getDeployOriginDataHandler.getDeployOriginData(input);
            }
            case "DeployCdk" -> {
                try {
                    cdkRunnerAdapter.handle(toInput(input, CdkInput.class));
                } catch (CouldNotParseInputException e) {
                    logger.error("Could not parse cdk input", e);
                    stepFunctionFacade.sendError(extractSfnToken(input), e.getMessage(), "CdkConfigError");
                }
            }
            case "ExecuteRunner" -> {
                logger.info("Runner triggered");
                try {
                    RunnerInput runnerInput = toInput(input, RunnerInput.class);
                    runnerHandler.handle(runnerInput);
                } catch (CouldNotParseInputException e) {
                    logger.error("Could not parse runner input", e);
                    stepFunctionFacade.sendError(extractSfnToken(input), e.getMessage(), "RunnerConfigError");
                }
            }
            case "Import" -> {
                logger.info("import triggered");
                try {
                    ImportInput importInput = toInput(input, ImportInput.class);
                    stepFunctionFacade.sendSuccess(importInput.deploymentPlanExecutionMetadata().sfnToken(),
                                                   toJsonString(importHandler.readOutputs(importInput)));
                } catch (Exception e) {
                    logger.error("Could not execute import", e);
                    stepFunctionFacade.sendError(extractSfnToken(input), e.getMessage(), "ImportError");
                }
            }
            case "ManualApproval" -> {
                DeployOriginData deploymentOriginData = quarkusMapper.convertValue(input.get("deploymentOriginData"),
                                                                                   DeployOriginData.class);
                DeploymentPlanExecutionMetadata deploymentPlanExecutionMetadata = quarkusMapper.convertValue(input.get(
                                                                                                                     "deploymentPlanExecutionMetadata"),
                                                                                                             DeploymentPlanExecutionMetadata.class);
                stackDataFacade.saveManualApprovalData(deploymentPlanExecutionMetadata, deploymentOriginData);

            }
            default -> logger.error("No Action was executed");
        }
        return input;

    }

    private void handlePostPipelineHook(Map<String, Object> input) {
        logger.info("Running post pipeline hook");
        JsonNode jsonNode = quarkusMapper.valueToTree(input);

        sendUsageDataFacade.sendEndUsage(jsonNode.get("detail")
                                                 .get("executionArn")
                                                 .asText(),
                                         jsonNode.get("detail")
                                                 .get("status")
                                                 .asText(),
                                         jsonNode.get("detail")
                                                 .get("stateMachineArn")
                                                 .asText(),
                                         Instant.ofEpochSecond(Long.parseLong(jsonNode.get("detail")
                                                                                      .get("startDate")
                                                                                      .asText())));

        DeploymentPlanStateData deploymentPlan = deploymentPlanDataFacade.getDeploymentPlan(jsonNode.get("detail")
                                                                                                    .get("stateMachineArn")
                                                                                                    .asText());
        DistributionContext context = deployOriginFacade.getContext(jsonNode.get("detail")
                                                                            .get("executionArn")
                                                                            .asText(),
                                                                    deploymentPlan.getDeployOriginSourceName());


        if ("SUCCEEDED".equals(jsonNode.path("detail")
                                       .path("status")
                                       .asText())) {

            saveDistributionOutput(jsonNode.get("detail")
                                           .get("output"), context);
        } else {
            deployOriginFacade.addExecutionError(deploymentPlan.getDeployOriginSourceName(),
                                                 context.getObjectIdentifier(),
                                                 jsonNode.path("detail")
                                                         .path("cause")
                                                         .asText(),
                                                 jsonNode.path("detail")
                                                         .path("error")
                                                         .asText());
        }
    }

    private static boolean isCloudWatchEvent(Map<String, Object> input) {
        //"detail-type" indicates its a cloud watch event triggered by the post pipeline hook
        return input.containsKey("detail-type");
    }

    private <T> T toInput(Map<String, Object> input, Class<T> toValueType) {
        try {
            return quarkusMapper.convertValue(input, toValueType);
        } catch (IllegalArgumentException e) {
            if (e.getCause().getCause() != null && e.getCause().getCause() instanceof AttiniDeserializationException) {
                DeploymentPlanExecutionMetadata deploymentPlanExecutionMetadata = quarkusMapper.convertValue(
                        input.get(
                                "deploymentPlanExecutionMetadata"),
                        DeploymentPlanExecutionMetadata.class);

                stepFunctionFacade.sendError(deploymentPlanExecutionMetadata.sfnToken(),
                                             "Could not convert input for step " + deploymentPlanExecutionMetadata.stepName() + ", " + e.getCause()
                                                                                                                                        .getCause()
                                                                                                                                        .getMessage(),
                                             "IllegalFormatException");


            }
            throw e;
        }
    }

    private void saveDistributionOutput(JsonNode output, DistributionContext context) {
        try {


            artifactStoreFacade.saveDistributionOutput(context.getEnvironment(),
                                                       context.getDistributionName(),
                                                       context.getDistributionId(),
                                                       quarkusMapper.readTree(output.asText()));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Could not read distribution meta data from output block", e);
        }
    }

    private String toJsonString(Map<String, Object> input) {
        try {
            return quarkusMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            logger.fatal("Could not parse input", e);
            throw new IllegalArgumentException(e);
        }
    }

    private boolean isCustomResource(Map<String, Object> input) {
        String resourceType = (String) input.get("ResourceType");
        return input.containsKey("ResourceType")
               && resourceType.startsWith("Custom::");
    }


    @SuppressWarnings("unchecked")
    private String extractSfnToken(Map<String, Object> input) {
        Map<String, Object> deploymentPlanMetadata = (Map<String, Object>) input.get("deploymentPlanExecutionMetadata");
        return (String) deploymentPlanMetadata.get("sfnToken");
    }
}
