package attini.action.actions.sam;

public class PackageSamScript {

    private final static String PACKAGE_SAM_SCRIPT = """
                      SAM_CLI_TELEMETRY=0
                      SAM_PROJECT_PATH="%s"
                      SAM_BUILD_DIR="%s"
                      echo "SAM package for project: ${SAM_PROJECT_PATH}"
                      unzip -qo ${SAM_PROJECT_PATH}/attiniSamProject.zip -d ${SAM_PROJECT_PATH}
                      S3_PREFIX="${ATTINI_ENVIRONMENT_NAME}/${ATTINI_DISTRIBUTION_NAME}/${ATTINI_DISTRIBUTION_ID}/.sam-source/${SAM_PROJECT_PATH}"
                      sam package -t ${SAM_PROJECT_PATH}/${SAM_BUILD_DIR}/template.yaml \\
                        --s3-bucket "${ATTINI_ARTIFACT_STORE}" \\
                        --s3-prefix "${S3_PREFIX}" \\
                        --output-template-file "template.yaml" 1> /dev/null
                      $ATTINI_RUNNER_EXEC command-mode store-artifact "${S3_PREFIX}/template.yaml" "./template.yaml"
                      echo "s3://${ATTINI_ARTIFACT_STORE}/${S3_PREFIX}/template.yaml" > ${ATTINI_OUTPUT}
                        
            """;

    public static String getPackageSamScript(String path, String buildDir) {
        String samPath = path.startsWith("/") ? path.substring(1) : path;
        return PACKAGE_SAM_SCRIPT.formatted(samPath,
                                            buildDir);
    }

}
