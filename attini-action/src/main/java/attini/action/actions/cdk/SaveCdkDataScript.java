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
            RESPONSE=$(aws lambda invoke --function-name attini-step-guard \
                      --payload "{\
                        \\"requestType\\":\\"register-cdk-stacks\\",\
                        \\"objectIdentifier\\":\\"${OBJECT_IDENTIFIER}\\",\
                        \\"distributionId\\":\\"${ATTINI_DISTRIBUTION_ID}\\",\
                        \\"distributionName\\":\\"${ATTINI_DISTRIBUTION_NAME}\\",\
                        \\"environment\\":\\"${ATTINI_ENVIRONMENT_NAME}\\",\
                        \\"stepName\\":\\"${ATTINI_STEP_NAME}\\",\
                        \\"stacks\\":$(%s),\
                        \\"outputs\\": $(cat %s)}" \
                      --cli-binary-format raw-in-base64-out ${ATTINI_OUTPUT})
            if echo $RESPONSE | grep -q "FunctionError"; then
              echo $RESPONSE
              cat ${ATTINI_OUTPUT}
              exit 1
            fi
            """;

    public static String getSaveScript(CdkCommandBuilder commandBuilder){
        return SAVE.formatted(commandBuilder.buildListCommand(),
                              commandBuilder.getOutputFile());

    }
}

