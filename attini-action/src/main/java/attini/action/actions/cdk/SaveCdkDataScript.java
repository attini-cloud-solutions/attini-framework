package attini.action.actions.cdk;

public class SaveCdkDataScript {

    private static final String SAVE = """
            $ATTINI_RUNNER_EXEC command-mode save-cdk-stacks \
                       "{\
                        \\"requestType\\":\\"save-cdk-stacks\\",\
                        \\"objectIdentifier\\":\\"${ATTINI_OBJECT_IDENTIFIER}\\",\
                        \\"distributionId\\":\\"${ATTINI_DISTRIBUTION_ID}\\",\
                        \\"distributionName\\":\\"${ATTINI_DISTRIBUTION_NAME}\\",\
                        \\"environment\\":\\"${ATTINI_ENVIRONMENT_NAME}\\",\
                        \\"stepName\\":\\"${ATTINI_STEP_NAME}\\",\
                        \\"stacks\\":$(%s)\
                        }"
            """;

    public static String getFormatOutputScript(CdkCommandBuilder commandBuilder) {
        return FORMAT_OUTPUT.formatted(commandBuilder.buildListCommand(),
                              commandBuilder.getOutputFile());

    }

    private static final String FORMAT_OUTPUT = """
            $ATTINI_RUNNER_EXEC command-mode format-cdk-output \
                       "{\
                        \\"requestType\\":\\"format-cdk-stacks-output\\",\
                        \\"objectIdentifier\\":\\"${ATTINI_OBJECT_IDENTIFIER}\\",\
                        \\"distributionId\\":\\"${ATTINI_DISTRIBUTION_ID}\\",\
                        \\"distributionName\\":\\"${ATTINI_DISTRIBUTION_NAME}\\",\
                        \\"environment\\":\\"${ATTINI_ENVIRONMENT_NAME}\\",\
                        \\"stepName\\":\\"${ATTINI_STEP_NAME}\\",\
                        \\"stacks\\":$(%s),\
                        \\"outputs\\": $(cat %s)}" \
                      > ${ATTINI_OUTPUT}
            """;

    public static String getSaveScript(CdkCommandBuilder commandBuilder) {
        return SAVE.formatted(commandBuilder.buildListCommand());

    }



}

