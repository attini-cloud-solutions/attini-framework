package attini.action.actions.cdk;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;

import attini.action.actions.cdk.input.CdkInput;
import attini.action.actions.cdk.input.CdkProperties;
import attini.action.actions.runner.RunnerHandler;
import attini.action.actions.runner.input.RunnerInput;
import attini.action.actions.runner.input.RunnerProperties;

public class CdkRunnerAdapter {

    private static final Logger logger = Logger.getLogger(CdkRunnerAdapter.class);


    private final RunnerHandler runnerHandler;

    public CdkRunnerAdapter(RunnerHandler runnerHandler) {
        this.runnerHandler = requireNonNull(runnerHandler, "runnerHandler");
    }

    public void handle(CdkInput cdkInput) {

        logger.info("Attini cdk step triggered");


        CdkCommandBuilder cdkCommands = createCdkCommands(cdkInput.properties());
        String saveCommand = SaveCdkDataScript.getSaveScript(cdkCommands);
        String deployCommand = cdkCommands.buildDeployCommand();
        String cdkSynthCommand = cdkCommands.buildSynthCommand();
        List<String> commands = List.of("cd " + cdkInput.properties().path(),
                                        "echo  'Running cdk synth command: " + cdkSynthCommand + "'",
                                        cdkSynthCommand,
                                        saveCommand,
                                        "echo  'Running cdk deploy command: " + deployCommand + "'",
                                        deployCommand,
                                        SaveCdkDataScript.getFormatOutputScript(cdkCommands));
        RunnerProperties runnerProperties = new RunnerProperties(commands,
                                                                 cdkInput.properties().runner(),
                                                                 cdkInput.properties()
                                                                         .environment());

        runnerHandler.handle(new RunnerInput(cdkInput.output(),
                                             runnerProperties,
                                             cdkInput.deploymentPlanExecutionMetadata(),
                                             cdkInput.deployOriginData(),
                                             cdkInput.dependencies(),
                                             cdkInput.customData(),
                                             cdkInput.stackParameters(),
                                             cdkInput.customData()));

    }

    public void handleChangeset(CdkInput cdkInput) {

        logger.info("Attini cdk change set step triggered");


        String diffCommand = createCdkCommands(cdkInput.properties()).buildDiffCommand();

        List<String> commands = List.of("cd " + cdkInput.properties().path(),
                                        "echo  'Running cdk diff command: " + diffCommand + "'",
                                        """
                                                if %s; then
                                                  echo no-change > ${ATTINI_OUTPUT}
                                                else
                                                  echo change-detected > ${ATTINI_OUTPUT}
                                                fi
                                                """.formatted(diffCommand));
        RunnerProperties runnerProperties = new RunnerProperties(commands,
                                                                 cdkInput.properties().runner(),
                                                                 cdkInput.properties()
                                                                         .environment());

        runnerHandler.handle(new RunnerInput(cdkInput.output(),
                                             runnerProperties,
                                             cdkInput.deploymentPlanExecutionMetadata(),
                                             cdkInput.deployOriginData(),
                                             cdkInput.dependencies(),
                                             cdkInput.customData(),
                                             cdkInput.stackParameters(),
                                             cdkInput.appConfig()));

    }

    private static CdkCommandBuilder createCdkCommands(CdkProperties properties) {


        return CdkCommandBuilder.builder(properties.stacks(), "/tmp/%s-cdk-output.json".formatted(UUID.randomUUID().toString()))
                                .addRoleArn(properties.roleArn())
                                .addContext(properties.context())
                                .addStackConfig(properties.stackConfiguration())
                                .addBuild(properties.build())
                                .addNotificationArns(properties.notificationArns())
                                .addPlugins(properties.plugins())
                                .addApp(properties.app())
                                .addBuildExclude(properties.buildExcludes())
                                .addForce(properties.force() != null && properties.force().equalsIgnoreCase("true"));

    }

}
