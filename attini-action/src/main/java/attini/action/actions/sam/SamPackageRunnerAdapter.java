package attini.action.actions.sam;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import attini.action.actions.deploycloudformation.stackconfig.StackConfiguration;
import attini.action.actions.deploycloudformation.stackconfig.StackConfigurationService;
import attini.action.actions.runner.RunnerHandler;
import attini.action.actions.runner.input.RunnerInput;
import attini.action.actions.runner.input.RunnerProperties;
import attini.action.actions.sam.input.Project;
import attini.action.actions.sam.input.SamInput;
import attini.action.domain.CfnStackConfig;
import attini.action.facades.stackdata.ResourceStateFacade;
import attini.action.facades.stackdata.StackTemplate;
import attini.action.facades.stepfunction.StepFunctionFacade;

public class SamPackageRunnerAdapter {

    private static final Logger logger = Logger.getLogger(SamPackageRunnerAdapter.class);
    private final RunnerHandler runnerHandler;
    private final ResourceStateFacade resourceStateFacade;
    private final StackConfigurationService stackConfigurationService;
    private final StepFunctionFacade stepFunctionFacade;

    public SamPackageRunnerAdapter(RunnerHandler runnerHandler,
                                   ResourceStateFacade resourceStateFacade,
                                   StackConfigurationService stackConfigurationService,
                                   StepFunctionFacade stepFunctionFacade) {
        this.runnerHandler = requireNonNull(runnerHandler, "runnerHandler");
        this.resourceStateFacade = requireNonNull(resourceStateFacade, "stackDataFacade");
        this.stackConfigurationService = requireNonNull(stackConfigurationService, "stackConfigurationService");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "stepFunctionFacade");
    }

    public void handle(SamInput samInput, CfnStackConfig cfnConfig) {


        StackConfiguration stackConfig = stackConfigurationService.getStackConfig(cfnConfig, false);
        Optional<StackTemplate> stackTemplate = resourceStateFacade.getStackTemplate(stackConfig);
        if (stackTemplate.isPresent() && stackTemplate.get()
                                                      .objectIdentifier()
                                                      .equals(samInput.deployOriginData().getObjectIdentifier())) {
            logger.info("Stack has already been updated withe the current object identifier, no need to repackage.");
            stepFunctionFacade.sendSuccess(samInput.deploymentPlanExecutionMetadata().sfnToken(),
                                           """
                                                   {
                                                     "result": "%s"
                                                   }
                                                   """.formatted(stackTemplate.get().template()));
            return;
        }

        Project project = samInput.properties().project();
        String packageSamScript = PackageSamScript.getPackageSamScript(project.path(),
                                                                       project.buildDir() == null ? ".aws-sam/build" : project.buildDir());
        RunnerProperties runnerProperties =
                new RunnerProperties(List.of(packageSamScript),
                                     samInput.properties().runner(), null);

        runnerHandler.handle(new RunnerInput(samInput.output(),
                                             runnerProperties,
                                             samInput.deploymentPlanExecutionMetadata(),
                                             samInput.deployOriginData(),
                                             samInput.dependencies(),
                                             samInput.customData(),
                                             samInput.stackParameters(),
                                             samInput.appConfig()));

    }


}
