package attini.action;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.actions.cdk.CdkRunnerAdapter;
import attini.action.actions.cdk.input.CdkInput;
import attini.action.actions.deploycloudformation.CfnHandler;
import attini.action.actions.deploycloudformation.input.AttiniCfnInput;
import attini.action.actions.getdeployorigindata.GetAppDeployOriginDataHandler;
import attini.action.actions.getdeployorigindata.GetDeployOriginDataHandler;
import attini.action.actions.merge.MergeUtil;
import attini.action.actions.posthook.PostPipelineHook;
import attini.action.actions.readoutput.ImportHandler;
import attini.action.actions.readoutput.input.ImportInput;
import attini.action.actions.runner.CouldNotParseInputException;
import attini.action.actions.runner.RunnerHandler;
import attini.action.actions.runner.input.RunnerInput;
import attini.action.actions.sam.SamPackageRunnerAdapter;
import attini.action.actions.sam.input.SamInput;
import attini.action.configuration.InitDeployConfigurationHandler;
import attini.action.domain.DeploymentPlanExecutionMetadata;
import attini.action.facades.artifactstore.ArtifactStoreFacade;
import attini.action.facades.deployorigin.DeployOriginFacade;
import attini.action.facades.stackdata.DeploymentPlanDataFacade;
import attini.action.facades.stackdata.ResourceStateFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import attini.action.licence.LicenceAgreementHandler;
import attini.domain.DeployOriginData;
import attini.domain.deserializers.AttiniDeserializationException;
import jakarta.inject.Inject;
import jakarta.inject.Named;


@Named("app")
public class App implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final Logger logger = Logger.getLogger(App.class);
    private final CfnHandler cfnHandler;
    private final StepFunctionFacade stepFunctionFacade;
    private final GetDeployOriginDataHandler getDeployOriginDataHandler;
    private final LicenceAgreementHandler licenceAgreementHandler;
    private final InitDeployConfigurationHandler initDeployConfigurationHandler;
    private final RunnerHandler runnerHandler;
    private final ObjectMapper quarkusMapper;
    private final ImportHandler importHandler;
    private final ResourceStateFacade resourceStateFacade;

    private final CdkRunnerAdapter cdkRunnerAdapter;
    private final SamPackageRunnerAdapter samPackageRunnerAdapter;
    private final GetAppDeployOriginDataHandler getAppDeployOriginDataHandler;
    private final PostPipelineHook postPipelineHook;

    @Inject
    public App(CfnHandler cfnHandler,
               StepFunctionFacade stepFunctionFacade,
               GetDeployOriginDataHandler getDeployOriginDataHandler,
               LicenceAgreementHandler licenceAgreementHandler,
               InitDeployConfigurationHandler initDeployConfigurationHandler,
               RunnerHandler runnerHandler,
               ObjectMapper quarkusMapper,
               ImportHandler importHandler,
               ResourceStateFacade resourceStateFacade,
               CdkRunnerAdapter cdkRunnerAdapter,
               SamPackageRunnerAdapter samPackageRunnerAdapter,
               GetAppDeployOriginDataHandler getAppDeployOriginDataHandler,
               PostPipelineHook postPipelineHook) {
        this.cfnHandler = requireNonNull(cfnHandler, "deployCfnStackService");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "stepFunctionFacade");
        this.getDeployOriginDataHandler = requireNonNull(getDeployOriginDataHandler, "getDeployOriginDataHandler");
        this.licenceAgreementHandler = requireNonNull(licenceAgreementHandler, "licenceAgreementHandler");
        this.initDeployConfigurationHandler = requireNonNull(initDeployConfigurationHandler,
                                                             "initDeployConfigurationHandler");
        this.runnerHandler = requireNonNull(runnerHandler, "runnerHandler");
        this.quarkusMapper = requireNonNull(quarkusMapper, "quarkusMapper");
        this.importHandler = requireNonNull(importHandler, "readOutputHandler");
        this.resourceStateFacade = requireNonNull(resourceStateFacade, "stackDataFacade");
        this.cdkRunnerAdapter = requireNonNull(cdkRunnerAdapter, "cdkRunnerAdapter");
        this.samPackageRunnerAdapter = requireNonNull(samPackageRunnerAdapter, "samPackageRunnerAdapter");
        this.getAppDeployOriginDataHandler = requireNonNull(getAppDeployOriginDataHandler, "getAppDeployOriginDataHandler");
        this.postPipelineHook = requireNonNull(postPipelineHook, "postPipelineHook");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {

        String inputString = toJsonString(input);
        logger.info("Got event: " + inputString);

        if (isCloudWatchEvent(input)) {
            postPipelineHook.handlePostPipelineHook(input);
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
            case "GetAppDeployOriginData" -> {
                logger.info("Get app deploy data triggered");
                return getAppDeployOriginDataHandler.getAppDeployOriginData(input);
            }
            case "DeployCdk" -> {
                try {
                    cdkRunnerAdapter.handle(toInput(input, CdkInput.class));
                } catch (CouldNotParseInputException e) {
                    logger.error("Could not parse cdk input", e);
                    stepFunctionFacade.sendError(extractSfnToken(input), e.getMessage(), "CdkConfigError");
                }
            }
            case "DeployCdkChangeset" -> {
                try {
                    cdkRunnerAdapter.handleChangeset(toInput(input, CdkInput.class));
                } catch (CouldNotParseInputException e) {
                    logger.error("Could not parse cdk changeset input", e);
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
                resourceStateFacade.saveManualApprovalData(deploymentPlanExecutionMetadata, deploymentOriginData);

            }

            case "PackageSam" -> samPackageRunnerAdapter
                    .handle(toInput(input, SamInput.class),
                            cfnHandler.createCfnStackConfig(toInput(input, AttiniCfnInput.class)));
            default -> logger.error("No Action was executed");
        }
        return input;

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
