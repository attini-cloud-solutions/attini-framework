package attini.action.actions.cdk;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import attini.action.actions.cdk.input.StackConfiguration;
import attini.action.actions.runner.CouldNotParseInputException;

class CdkCommandBuilder {


    private String buildCommand = "";

    private String appOption = "";

    private String buildExcludes = "";

    private String notificationArns = "";

    private String force = "";

    private String stacks = "";

    private final String outputFile;

    private String context = "";

    private String stackConfig = "";

    private String plugins = "";

    private String roleArn = "";

    private static final String NO_COLOR = " --no-color";


    private CdkCommandBuilder(List<String> stacks, String outputFile) {
        this.outputFile = requireNonNull(outputFile, "outputFile");
        if (stacks != null && !stacks.isEmpty()) {
            this.stacks = " " + String.join(" ", stacks);
        }

    }

    public CdkCommandBuilder addContext(Map<String, String> context) {
        if (context != null) {
            this.context = context.entrySet()
                                  .stream()
                                  .map(entry -> " --context " + entry.getKey() + "=" + entry.getValue())
                                  .collect(Collectors.joining());

        }
        return this;
    }

    public CdkCommandBuilder addStackConfig(List<StackConfiguration> stackConfigurations) {
        if (stackConfigurations != null) {

            stackConfig = stackConfigurations.stream()
                                             .flatMap(config -> {
                                                 if (config.parameters() == null || config.parameters().isEmpty()) {
                                                     throw new CouldNotParseInputException(
                                                             "StackConfigurations require at least one parameter ");
                                                 }
                                                 return config.parameters()
                                                              .entrySet()
                                                              .stream()
                                                              .map(entry -> {
                                                                  if (config.stackName() != null && !config.stackName()
                                                                                                           .isBlank()) {

                                                                      return " --parameters " + config.stackName() + ":" + entry.getKey() + "=" + entry.getValue();
                                                                  }

                                                                  return " --parameters " + entry.getKey() + "=" + entry.getValue();

                                                              });
                                             }).collect(Collectors.joining());
        }
        return this;
    }

    public CdkCommandBuilder addApp(String app) {
        if (app != null && !app.isBlank()) {
            appOption = " --app " + app;
        }

        return this;
    }

    public CdkCommandBuilder addBuild(String build) {
        if (build != null && !build.isBlank()) {
            buildCommand = " --build " +
                           "\"" +
                           build +
                           "\"";
        }

        return this;
    }

    public CdkCommandBuilder addPlugins(List<String> plugins) {
        if (plugins != null) {
            this.plugins = plugins.stream().map(s -> " --plugin " + s).collect(Collectors.joining());
        }

        return this;
    }

    public CdkCommandBuilder addBuildExclude(List<String> buildExclude) {
        if (buildExclude != null) {

            this.buildExcludes = buildExclude.stream()
                                             .map(exclude -> " --build-exclude " + exclude)
                                             .collect(Collectors.joining());
        }

        return this;
    }

    public CdkCommandBuilder addNotificationArns(List<String> arns) {
        if (arns != null) {
            this.notificationArns = arns.stream()
                                        .map(arn -> " --notification-arns " + arn)
                                        .collect(Collectors.joining());
        }

        return this;
    }

    public CdkCommandBuilder addForce(boolean force) {
        if (force) {
            this.force = " --force";
        }
        return this;
    }

    public CdkCommandBuilder addRoleArn(String roleArn) {
        if (roleArn != null && !roleArn.isBlank()) {
            this.roleArn = " --role-arn " + roleArn;
        }
        return this;

    }

    public String getOutputFile() {
        return outputFile;
    }

    private String getStacksWithAllDefault(){
        if (stacks == null || stacks.isBlank()){
            return " --all";
        }
        return stacks;
    }


    public String buildDeployCommand() {
        return "cdk deploy --outputs-file " +
               outputFile +
               " --progress events --require-approval never" +
               " --app cdk.out" +
               buildExcludes +
               notificationArns +
               force +
               NO_COLOR +
               roleArn +
               plugins +
               stackConfig +
               context +
               getStacksWithAllDefault();
    }

    public String buildSynthCommand() {
        return "cdk synth --quiet" +
               appOption +
               buildCommand +
               buildExcludes +
               notificationArns +
               force +
               NO_COLOR +
               roleArn +
               plugins +
               stackConfig +
               context +
               getStacksWithAllDefault();
    }

    public String buildListCommand() {
        return "cdk ls --long --json" +
               NO_COLOR +
               roleArn +
               plugins +
               context +
               " --app cdk.out" +
               stacks;
    }

    public String buildDiffCommand() {
        return "cdk diff --fail" +
               NO_COLOR +
               roleArn +
               buildCommand +
               plugins +
               context +
               stacks;
    }

    public static CdkCommandBuilder builder(List<String> stacks, String outputFile) {
        return new CdkCommandBuilder(stacks, outputFile);
    }
}
