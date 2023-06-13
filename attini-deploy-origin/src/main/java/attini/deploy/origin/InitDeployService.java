/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.deploy.origin;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jboss.logging.Logger;
import com.vdurmont.semver4j.Semver;

import attini.deploy.origin.appdeployment.AppDeploymentFacade;
import attini.deploy.origin.config.AttiniConfig;
import attini.deploy.origin.config.DistributionDependency;
import attini.deploy.origin.config.DistributionType;
import attini.deploy.origin.config.InitDeployStackConfig;
import attini.deploy.origin.deploystack.DeployDataFacade;
import attini.deploy.origin.deploystack.DeployDataFacade.SaveDeploymentDataRequest;
import attini.deploy.origin.deploystack.DeployInitStackException;
import attini.deploy.origin.deploystack.DeployInitStackService;
import attini.deploy.origin.deploystack.DynamoInitDeployStackFacade;
import attini.deploy.origin.deploystack.InitDeployError;
import attini.deploy.origin.lifecycle.LifeCycleService;
import attini.deploy.origin.stepguard.StepGuardFacade;
import attini.domain.DistributionContext;
import attini.domain.DistributionContextImpl;
import attini.domain.Environment;
import attini.domain.Version;

public class InitDeployService {

    private static final Logger logger = Logger.getLogger(InitDeployService.class);

    private final PublishArtifactService publishArtifactService;
    private final DeployInitStackService deployInitStackService;
    private final DeployDataFacade deployDataFacade;
    private final PutLatestDistributionReferenceParameter putLatestDistributionReferenceParameter;
    private final DynamoInitDeployStackFacade dynamoInitDeployStackFacade;
    private final LifeCycleService lifeCycleService;
    private final MonitoringFacade monitoringFacade;
    private final SystemClockFacade systemClockFacade;
    private final StepGuardFacade stepGuardFacade;
    private final DistributionDataFacade distributionDataFacade;
    private final AppDeploymentFacade appDeploymentFacade;

    public InitDeployService(PublishArtifactService publishArtifactService,
                             DeployInitStackService deployInitStackService,
                             DeployDataFacade deployDataFacade,
                             PutLatestDistributionReferenceParameter putLatestDistributionReferenceParameter,
                             DynamoInitDeployStackFacade dynamoInitDeployStackFacade,
                             LifeCycleService lifeCycleService,
                             MonitoringFacade monitoringFacade,
                             SystemClockFacade systemClockFacade,
                             StepGuardFacade stepGuardFacade,
                             DistributionDataFacade distributionDataFacade, AppDeploymentFacade appDeploymentFacade) {
        this.publishArtifactService = requireNonNull(publishArtifactService, "publishArtifactService");
        this.deployInitStackService = requireNonNull(deployInitStackService, "deployInitStackService");
        this.deployDataFacade = requireNonNull(deployDataFacade, "deployDataFacade");
        this.putLatestDistributionReferenceParameter = requireNonNull(putLatestDistributionReferenceParameter,
                                                                      "putLatestDistributionReferenceParameter");
        this.dynamoInitDeployStackFacade = requireNonNull(dynamoInitDeployStackFacade, "dynamoInitDeployStackFacade");
        this.lifeCycleService = requireNonNull(lifeCycleService, "lifeCycleService");
        this.monitoringFacade = requireNonNull(monitoringFacade, "monitoringFacade");
        this.systemClockFacade = requireNonNull(systemClockFacade, "systemClockFacade");
        this.stepGuardFacade = requireNonNull(stepGuardFacade, "stepGuardFacade");
        this.distributionDataFacade = requireNonNull(distributionDataFacade, "distributionDataFacade");
        this.appDeploymentFacade = requireNonNull(appDeploymentFacade, "appDeploymentFacade");
    }

    public void initDeploy(InitDeployEvent initDeployEvent) {

        long deployTime = systemClockFacade.getCurrentTime();

        try {

            DistributionData distributionData = publishArtifactService.publishDistribution(initDeployEvent);

            AttiniConfig attiniConfig = distributionData.getAttiniConfig();

            CompletableFuture<Void> cleanup = lifeCycleService.cleanup(attiniConfig.getAttiniDistributionName(),
                                                                       initDeployEvent.getEnvironmentName(),
                                                                       initDeployEvent.getS3Bucket());


            putLatestDistributionReferenceParameter.putParameter(attiniConfig, initDeployEvent.getEnvironmentName());


            distributionDataFacade.saveDistributionData(attiniConfig,
                                                        initDeployEvent,
                                                        distributionData.getArtifactPath());






            attiniConfig.getAppConfig()
                        .ifPresent(appConfig -> {
                            appDeploymentFacade.runAppDeploymentPlan(appConfig,
                                                                     createDistributionContext(initDeployEvent, attiniConfig),
                                                                     distributionData, deployTime);
                        });


            attiniConfig.getAttiniInitDeployStackConfig()
                        .ifPresentOrElse(initDeployStackConfig ->
                                                 handelInitDeployStack(createDistributionContext(initDeployEvent,
                                                                                                 attiniConfig),
                                                                       deployTime,
                                                                       distributionData,
                                                                       initDeployStackConfig),
                                         () -> {
                                             if (attiniConfig.getAppConfig().isEmpty()){
                                                 deployDataFacade.savePlatformDeployment(SaveDeploymentDataRequest.builder()
                                                                                                                  .distributionData(
                                                                                                        distributionData)
                                                                                                                  .distributionContext(
                                                                                                        createDistributionContext(
                                                                                                                initDeployEvent,
                                                                                                                attiniConfig))
                                                                                                                  .deployTime(deployTime)
                                                                                                                  .isUnchanged(false)
                                                                                                                  .build());
                                             }
                                             validateDependencies(initDeployEvent.getEnvironmentName(), attiniConfig);
                                         }
                        );


            monitoringFacade.sendInitDeployEvent(distributionData, initDeployEvent.getEnvironmentName());
            cleanup.join();
        } catch (PublishDistributionException e) {
            logger.error("There was an error publishing distribution", e);
            deployDataFacade.savePlatformDeployment(SaveDeploymentDataRequest.builder()
                                                                             .deployTime(deployTime)
                                                                             .distributionContext(createDistributionContext(
                                                                   initDeployEvent, e))
                                                                             .error(new InitDeployError(
                                                                   "PublishDistributionError",
                                                                   e.getMessage() != null ? e.getMessage() : "error publishing artifacts to S3"))
                                                                             .build());
            throw new InitDeployException("Failed to publish distribution", e);

        }
    }

    private static DistributionContextImpl createDistributionContext(InitDeployEvent initDeployEvent,
                                                                     PublishDistributionException exception) {
        return DistributionContextImpl.builder()
                                      .distributionId(exception.getDistributionId())
                                      .environment(initDeployEvent.getEnvironmentName())
                                      .objectIdentifier(initDeployEvent.getObjectIdentifier())
                                      .distributionName(exception.getDistributionName())
                                      .build();
    }

    private static DistributionContextImpl createDistributionContext(InitDeployEvent initDeployEvent,
                                                                     AttiniConfig attiniConfig) {
        return DistributionContextImpl.builder()
                                      .distributionId(attiniConfig.getAttiniDistributionId())
                                      .environment(initDeployEvent.getEnvironmentName())
                                      .objectIdentifier(initDeployEvent.getObjectIdentifier())
                                      .distributionName(attiniConfig.getAttiniDistributionName())
                                      .build();
    }

    private void handelInitDeployStack(DistributionContext distributionContext,
                                       long deployTime,
                                       DistributionData distributionData,
                                       InitDeployStackConfig initDeployStackConfig) {

        boolean shouldDeployNewsStack = shouldDeployNewsStack(distributionData,
                                                              initDeployStackConfig);

        deployDataFacade.savePlatformDeployment(SaveDeploymentDataRequest.builder()
                                                                         .deployTime(deployTime)
                                                                         .distributionContext(distributionContext)
                                                                         .isUnchanged(!shouldDeployNewsStack)
                                                                         .distributionData(distributionData)
                                                                         .build());

        validateDependencies(distributionContext.getEnvironment(), distributionData.getAttiniConfig());

        if (shouldDeployNewsStack) {

            dynamoInitDeployStackFacade.saveInitDeployItem(initDeployStackConfig,
                                                           distributionContext,
                                                           distributionData);
            deployInitStack(distributionData,
                            distributionContext,
                            initDeployStackConfig,
                            deployTime);


        } else {
            dynamoInitDeployStackFacade.updateInitDeployItemForUnchangedStack(initDeployStackConfig,
                                                                              distributionContext,
                                                                              distributionData);
            stepGuardFacade.respondToStepGuard(initDeployStackConfig.getInitDeployStackName());

        }
    }

    private void validateDependencies(Environment environment,
                                      AttiniConfig attiniConfig) {
        for (DistributionDependency dependency : attiniConfig.getDependencies()) {

            Distribution distribution =
                    distributionDataFacade.getDistribution(dependency.distributionName(), environment)
                                          .orElseThrow(() -> new PublishDistributionException(
                                                  attiniConfig.getAttiniDistributionName(),
                                                  attiniConfig.getAttiniDistributionId(),
                                                  "Failed to publish distribution. Unsatisfied dependency = " + dependency.distributionName()
                                                                                                                          .asString() + ", distribution is not present in current environment"));

            if (dependency.version() != null) {
                String requiredVersion = distribution.getVersion()
                                                     .map(Version::asString)
                                                     .orElseThrow(() -> new PublishDistributionException(
                                                             attiniConfig.getAttiniDistributionName(),
                                                             attiniConfig.getAttiniDistributionId(),
                                                             "Failed to publish distribution. Unsatisfied dependency = %s, dependency is present in environment but has no version".formatted(
                                                                     dependency.distributionName()
                                                                               .asString())));

                Semver semver = new Semver(requiredVersion, Semver.SemverType.NPM);

                boolean satisfies = semver.satisfies(dependency.version());

                if (!satisfies) {
                    throw new PublishDistributionException(
                            attiniConfig.getAttiniDistributionName(),
                            attiniConfig.getAttiniDistributionId(),
                            "Failed to publish distribution. Unsatisfied dependency = %s, dependency is present in environment but but does not match version. Version requirement = %s, deployed version = %s".formatted(
                                    dependency.distributionName()
                                              .asString(),
                                    dependency.version(),
                                    requiredVersion));
                }
            }

            deployDataFacade.getLatestDeployData(dependency.distributionName(), environment)
                            .getErrorMessage()
                            .ifPresent(s -> {
                                throw new PublishDistributionException(attiniConfig.getAttiniDistributionName(),
                                                                       attiniConfig.getAttiniDistributionId(),
                                                                       "Failed to publish distribution. Unsatisfied dependency = " + dependency.distributionName()
                                                                                                                                               .asString() + ", latest deployment of dependency failed with message = " + s);
                            });


        }
    }

    private boolean shouldDeployNewsStack(DistributionData distributionData,
                                          InitDeployStackConfig initDeployStackConfig) {

        if (initDeployStackConfig.forceUpdate()) {
            return true;
        }

        if (distributionData.getTemplateMd5Hex().isEmpty()) {
            return true;
        }
        Optional<String> previousMd5Hex = dynamoInitDeployStackFacade.geInitStackMd5(initDeployStackConfig.getInitDeployStackName());
        return previousMd5Hex.map(s -> !s.equals(distributionData.getTemplateMd5Hex().get())).orElse(true);
    }

    private void deployInitStack(DistributionData distributionData,
                                 DistributionContext distributionContext,
                                 InitDeployStackConfig initDeployStackConfig,
                                 long deployTime) {
        try {
            deployInitStackService.deployInitStack(distributionContext,
                                                   initDeployStackConfig,
                                                   distributionData.getArtifactPath(),
                                                   distributionData.getAttiniConfig().getAttiniDistributionTags());

        } catch (DeployInitStackException e) {
            dynamoInitDeployStackFacade.setInitDeployError(initDeployStackConfig, e.getAwsErrorMessage());
            deployDataFacade.savePlatformDeployment(SaveDeploymentDataRequest.builder()
                                                                             .distributionData(distributionData)
                                                                             .distributionContext(distributionContext)
                                                                             .isUnchanged(false)
                                                                             .deployTime(deployTime)
                                                                             .error(new InitDeployError(e.getAwsErrorCode(),
                                                                                      e.getAwsErrorMessage()))
                                                                             .build());
            throw new InitDeployException("Failed deploying init stack", e);
        }
    }
}
