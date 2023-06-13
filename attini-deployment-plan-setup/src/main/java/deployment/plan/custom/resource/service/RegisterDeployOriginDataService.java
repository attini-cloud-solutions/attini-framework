/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package deployment.plan.custom.resource.service;

import static deployment.plan.custom.resource.StackType.INFRA;
import static java.util.Objects.requireNonNull;

import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import deployment.plan.custom.resource.StackType;
import deployment.plan.transform.Runner;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

public class RegisterDeployOriginDataService {
    private static final Logger logger = Logger.getLogger(RegisterDeployOriginDataService.class);
    private final DeployStatesFacade deployStatesFacade;
    private final DeploymentPlanStateFactory deploymentPlanStateFactory;


    public RegisterDeployOriginDataService(DeployStatesFacade deployStatesFacade,
                                           DeploymentPlanStateFactory deploymentPlanStateFactory) {
        this.deployStatesFacade = requireNonNull(deployStatesFacade, "deployStatesFacade");
        this.deploymentPlanStateFactory = requireNonNull(deploymentPlanStateFactory, "deploymentPlanStateFactory");
    }

    public void registerDeployOriginData(RegisterDeployOriginDataRequest request) {

        switch (request.getCfnRequestType()) {
            case CREATE -> {
                logger.info("Register OriginDeployDataLink");
                DeploymentPlanResourceState dataSource = deploymentPlanStateFactory.create(request, INFRA);
                deployStatesFacade.saveDeploymentPlanState(dataSource);
                request.getRunners().forEach(runner -> deployStatesFacade.saveRunnerState(runner, dataSource));
                saveToInitStack(request);

            }
            case UPDATE -> {
                logger.info("Updating OriginDeployDataLink");
                deployStatesFacade.deleteDeploymentPlanState(request.getOldSfnArn());
                DeploymentPlanResourceState dataSourceUpdate = deploymentPlanStateFactory.create(request, INFRA);;
                logger.info("working with data source = " + dataSourceUpdate);
                deployStatesFacade.saveDeploymentPlanState(dataSourceUpdate);
                request.getRunners().forEach(runner -> deployStatesFacade.saveRunnerState(runner, dataSourceUpdate));
                saveToInitStack(request);

            }
            case DELETE -> deployStatesFacade.deleteDeploymentPlanState(request.getNewSfnArn());
            default -> throw new IllegalStateException("Invalid request type = " + request.getCfnRequestType());
        }

    }


    private void saveToInitStack(RegisterDeployOriginDataRequest request) {

        deployStatesFacade.saveToInitData(request.getRunners()
                                                 .stream()
                                                 .map(Runner::getName)
                                                 .collect(Collectors.toList()),
                                          request.getStackName(), request.getParameters());
    }


}
