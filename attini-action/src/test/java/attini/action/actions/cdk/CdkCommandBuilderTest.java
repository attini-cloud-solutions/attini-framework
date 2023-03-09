package attini.action.actions.cdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import attini.action.actions.cdk.input.StackConfiguration;
import attini.action.actions.runner.CouldNotParseInputException;

class CdkCommandBuilderTest {

    private static final String OUTPUT_FILE = "OUTPUT_FILE";

    @Test
    void ShouldCreateDeployCommand() {
        String commandString = CdkCommandBuilder.builder(null, OUTPUT_FILE).buildDeployCommand();
        assertEquals("cdk deploy --outputs-file "+OUTPUT_FILE+" --progress events --require-approval never --no-color --all", commandString);
    }

    @Test
    void ShouldCreateDeployAndListCommand_shouldAddApps() {
        CdkCommandBuilder builder = CdkCommandBuilder.builder(List.of("test1", "test2"), OUTPUT_FILE);
        assertEquals("cdk deploy --outputs-file "+OUTPUT_FILE+" --progress events --require-approval never --no-color test1 test2", builder.buildDeployCommand());
        assertEquals("cdk ls --long --json --no-color --app cdk.out test1 test2", builder.buildListCommand());

    }

    @Test
    void ShouldCreateDeployCommand_shouldAddBuild() {

        String commandString = CdkCommandBuilder.builder(null, OUTPUT_FILE).addBuild("npm install").buildDeployCommand();
        assertEquals("cdk deploy --outputs-file "+OUTPUT_FILE+" --progress events --require-approval never --build \"npm install\" --no-color --all", commandString);
    }


    @Test
    void ShouldCreateDeployCommand_withParameters() {
        Map<String, String> parameters = new TreeMap<>();
        parameters.put("MyParam", "MyParamValue");
        parameters.put("MySecondParam", "MySecondParamValue");
        List<StackConfiguration> stackConfiguration = List.of(new StackConfiguration("MyStack",
                                                                                     parameters));
        String commandString = CdkCommandBuilder.builder(null, OUTPUT_FILE)
                                                .addStackConfig(stackConfiguration)
                                                .buildDeployCommand();
        assertEquals(
                "cdk deploy --outputs-file "+OUTPUT_FILE+" --progress events --require-approval never --no-color --parameters MyStack:MyParam=MyParamValue --parameters MyStack:MySecondParam=MySecondParamValue --all",
                commandString);
    }

    @Test
    void ShouldCreateDeployCommand_withParametersNoStackName() {
        Map<String, String> parameters = new TreeMap<>();
        parameters.put("MyParam", "MyParamValue");
        parameters.put("MySecondParam", "MySecondParamValue");
        List<StackConfiguration> stackConfiguration = List.of(new StackConfiguration(null,
                                                                                     parameters));
        String commandString = CdkCommandBuilder.builder(null, OUTPUT_FILE)
                                                .addStackConfig(stackConfiguration)
                                                .buildDeployCommand();
        assertEquals(
                "cdk deploy --outputs-file "+OUTPUT_FILE+" --progress events --require-approval never --no-color --parameters MyParam=MyParamValue --parameters MySecondParam=MySecondParamValue --all",
                commandString);
    }

    @Test
    void ShouldCreateDeployCommand_withNoParametersShouldThrowException() {
        List<StackConfiguration> stackConfiguration = List.of(new StackConfiguration("myStack",
                                                                                     null));

        assertThrows(CouldNotParseInputException.class,
                     () -> CdkCommandBuilder.builder(null, OUTPUT_FILE)
                                            .addStackConfig(stackConfiguration)
                                            .buildDeployCommand());

    }

    @Test
    void ShouldCreateDeployCommand_withContext() {
        Map<String, String> context = new TreeMap<>();
        context.put("key1", "value1");
        context.put("key2", "value2");
        String commandString = CdkCommandBuilder.builder(null, OUTPUT_FILE)
                                                .addContext(context)
                                                .buildDeployCommand();
        assertEquals("cdk deploy --outputs-file "+OUTPUT_FILE+" --progress events --require-approval never --no-color --context key1=value1 --context key2=value2 --all",
                     commandString);
    }

    @Test
    void ShouldCreateDeployCommand_withApp() {

        String commandString = CdkCommandBuilder.builder(null, OUTPUT_FILE)
                                                .addApp("AppName")
                                                .buildDeployCommand();
        assertEquals("cdk deploy --outputs-file "+OUTPUT_FILE+" --progress events --require-approval never --app AppName --no-color --all", commandString);
    }

    @Test
    void ShouldCreateLsCommand_shouldExcludeBuild() {
        String commandString = CdkCommandBuilder.builder(null, OUTPUT_FILE).addBuild("npm install").buildListCommand();
        assertEquals("cdk ls --long --json --no-color --app cdk.out", commandString);
    }

    @Test
    void ShouldCreateDeployCommand_withPlugins() {

        String commandString = CdkCommandBuilder.builder(null, OUTPUT_FILE)
                                                .addApp("AppName")
                                                .addPlugins(List.of("plugin1", "plugin2"))
                                                .buildDeployCommand();
        assertEquals(
                "cdk deploy --outputs-file "+OUTPUT_FILE+" --progress events --require-approval never --app AppName --no-color --plugin plugin1 --plugin plugin2 --all",
                commandString);
    }

    @Test
    void ShouldCreateDeployCommand_withBuildExcludes() {

        CdkCommandBuilder commandsBuilder = CdkCommandBuilder.builder(null, OUTPUT_FILE)
                                                               .addApp("AppName")
                                                               .addBuildExclude(List.of("exclude1", "exclude2"));

        assertEquals(
                "cdk deploy --outputs-file "+OUTPUT_FILE+" --progress events --require-approval never --app AppName --build-exclude exclude1 --build-exclude exclude2 --no-color --all",
                commandsBuilder
                        .buildDeployCommand());

        assertEquals(
                "cdk ls --long --json --no-color --app cdk.out",
                commandsBuilder
                        .buildListCommand());
    }

    @Test
    void ShouldCreateDeployCommand_withNotificationArns() {

        CdkCommandBuilder commandsBuilder = CdkCommandBuilder.builder(null, OUTPUT_FILE)
                                                             .addApp("AppName")
                                                             .addNotificationArns(List.of("arn1", "arn2"));

        assertEquals(
                "cdk deploy --outputs-file "+OUTPUT_FILE+" --progress events --require-approval never --app AppName --notification-arns arn1 --notification-arns arn2 --no-color --all",
                commandsBuilder
                        .buildDeployCommand());

        assertEquals(
                "cdk ls --long --json --no-color --app cdk.out",
                commandsBuilder
                        .buildListCommand());
    }

    @Test
    void ShouldCreateDeployCommand_withForce() {

        CdkCommandBuilder commandsBuilder = CdkCommandBuilder.builder(null, OUTPUT_FILE)
                                                             .addApp("AppName")
                                                             .addForce(true);

        assertEquals(
                "cdk deploy --outputs-file "+OUTPUT_FILE+" --progress events --require-approval never --app AppName --force --no-color --all",
                commandsBuilder
                        .buildDeployCommand());

        assertEquals(
                "cdk ls --long --json --no-color --app cdk.out",
                commandsBuilder
                        .buildListCommand());
    }

    @Test
    void ShouldAddRoleArn() {

        CdkCommandBuilder commandsBuilder = CdkCommandBuilder.builder(null, OUTPUT_FILE)
                                                             .addApp("AppName")
                                                             .addRoleArn("some-arn");

        assertEquals(
                "cdk deploy --outputs-file "+OUTPUT_FILE+" --progress events --require-approval never --app AppName --no-color --role-arn some-arn --all",
                commandsBuilder
                        .buildDeployCommand());

        assertEquals(
                "cdk ls --long --json --no-color --role-arn some-arn --app cdk.out",
                commandsBuilder
                        .buildListCommand());
    }


}

