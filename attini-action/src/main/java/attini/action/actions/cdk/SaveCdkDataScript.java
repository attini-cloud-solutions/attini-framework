package attini.action.actions.cdk;

public class SaveCdkDataScript {

    public static final String SET_VARIABLE = """
           
            ATTINI_SAVE_CDK_STACK_ITEM="{
              \\"resourceType\\": {
                \\"S\\": \\"CdkStack\\"
              },
              \\"name\\":{
                \\"S\\": \\"__STACK_NAME__-${ATTINI_AWS_REGION}-${ATTINI_AWS_ACCOUNT}\\"
              },
              \\"attiniObjectIdentifier\\": {
                \\"S\\": \\"${ATTINI_OBJECT_IDENTIFIER}\\"
              },
              \\"distributionId\\":{
                \\"S\\": \\"${ATTINI_DISTRIBUTION_ID}\\"
              },
              \\"distributionName\\":{
                \\"S\\": \\"${ATTINI_DISTRIBUTION_NAME}\\"
              },
              \\"environment\\":{
                \\"S\\": \\"${ATTINI_ENVIRONMENT_NAME}\\"
              },
              \\"stackName\\":{
                \\"S\\": \\"__STACK_NAME__\\"
              },
              \\"stepName\\":{
                \\"S\\": \\"${ATTINI_STEP_NAME}\\"
              }
            }"
            """;
    private static final String SAVE = """
            exec $ATTINI_RUNNER_EXEC command-mode register-cdk-stacks \
                       "{\
                        \\"requestType\\":\\"register-cdk-stacks\\",\
                        \\"objectIdentifier\\":\\"${ATTINI_OBJECT_IDENTIFIER}\\",\
                        \\"distributionId\\":\\"${ATTINI_DISTRIBUTION_ID}\\",\
                        \\"distributionName\\":\\"${ATTINI_DISTRIBUTION_NAME}\\",\
                        \\"environment\\":\\"${ATTINI_ENVIRONMENT_NAME}\\",\
                        \\"stepName\\":\\"${ATTINI_STEP_NAME}\\",\
                        \\"stacks\\":$(%s),\
                        \\"outputs\\": $(cat %s)}" \
                      > ${ATTINI_OUTPUT}
            """;

    public static String getSaveScript(CdkCommandBuilder commandBuilder){
        return SAVE.formatted(commandBuilder.buildListCommand(),
                              commandBuilder.getOutputFile());

    }

}

