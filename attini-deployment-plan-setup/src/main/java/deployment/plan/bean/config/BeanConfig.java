package deployment.plan.bean.config;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.ObjectMapper;

import deployment.plan.AwsClientSingletonFactory;
import deployment.plan.custom.resource.CfnResponseSender;
import deployment.plan.custom.resource.CustomResourceHandler;
import deployment.plan.custom.resource.service.DeployStatesFacade;
import deployment.plan.custom.resource.service.RegisterDeployOriginDataService;
import deployment.plan.custom.resource.service.RegisterDeploymentPlanTriggerService;
import deployment.plan.system.EnvironmentVariables;
import deployment.plan.transform.AttiniStepLoader;
import deployment.plan.transform.DeployData;
import deployment.plan.transform.DeploymentPlanStepsCreator;
import deployment.plan.transform.TemplateFileLoader;
import deployment.plan.transform.TemplateFileLoaderImpl;
import deployment.plan.transform.TransformDeploymentPlanCloudFormation;

public class BeanConfig {

    @ApplicationScoped
    public EnvironmentVariables environmentVariables() {
        return new EnvironmentVariables();
    }


    @ApplicationScoped
    public TemplateFileLoader templateFileLoader() {
        return new TemplateFileLoaderImpl();
    }

    @ApplicationScoped
    public AttiniStepLoader attiniStepLoader(TemplateFileLoader templateFileLoader, ObjectMapper objectMapper) {
        return new AttiniStepLoader(templateFileLoader, objectMapper);
    }

    @ApplicationScoped
    public DeployData deployData(TemplateFileLoader templateFileLoader) {
        return new DeployData(templateFileLoader);
    }

    @ApplicationScoped
    public DeploymentPlanStepsCreator deploymentPlanStepsCreator(AttiniStepLoader attiniStepLoader,
                                                                 DeployData deployData,
                                                                 ObjectMapper objectMapper,
                                                                 EnvironmentVariables environmentVariables) {
        return new DeploymentPlanStepsCreator(attiniStepLoader, deployData, objectMapper, environmentVariables);
    }

    @ApplicationScoped
    public TransformDeploymentPlanCloudFormation transformDeploymentPlanCloudFormation(EnvironmentVariables environmentVariables,
                                                                                       ObjectMapper objectMapper,
                                                                                       DeploymentPlanStepsCreator deploymentPlanStepsCreator) {
        return new TransformDeploymentPlanCloudFormation(environmentVariables,
                                                         AwsClientSingletonFactory.getEc2Client(),
                                                         objectMapper,
                                                         deploymentPlanStepsCreator);
    }

    @ApplicationScoped
    DeployStatesFacade deployStatesFacade(EnvironmentVariables environmentVariables, ObjectMapper objectMapper) {
        return new DeployStatesFacade(
                AwsClientSingletonFactory.getDynamoDbClient(), environmentVariables, objectMapper);
    }

    @ApplicationScoped
    RegisterDeploymentPlanTriggerService registerDeploymentPlanTriggerService(DeployStatesFacade deployStatesFacade) {
        return new RegisterDeploymentPlanTriggerService(deployStatesFacade);
    }


    @ApplicationScoped
    RegisterDeployOriginDataService registerDeployOriginDataService(DeployStatesFacade deployStatesFacade) {
        return new RegisterDeployOriginDataService(deployStatesFacade, AwsClientSingletonFactory.getCfnClient());
    }

    @ApplicationScoped
    public CustomResourceHandler customResourceHandler(RegisterDeploymentPlanTriggerService registerDeploymentPlanTriggerService,
                                                       RegisterDeployOriginDataService registerDeployOriginDataService,
                                                       ObjectMapper objectMapper) {


        return new CustomResourceHandler(registerDeploymentPlanTriggerService,
                                         registerDeployOriginDataService,
                                         new CfnResponseSender(),
                                         objectMapper);
    }

}
