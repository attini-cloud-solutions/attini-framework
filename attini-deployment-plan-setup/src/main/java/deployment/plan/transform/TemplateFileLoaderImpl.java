/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package deployment.plan.transform;

import java.io.File;

public class TemplateFileLoaderImpl implements TemplateFileLoader {

    public static final String DEPLOY_DATA_STATE_JSON_FILE = "deploy-data-state.json";

    public static final String ATTINI_CFN_TEMPLATE_FILE = "attini-cfn-template.json";

    public static final String ATTINI_RUNNER_TEMPLATE_FILE = "attini-runner-template.json";

    public static final String ATTINI_CDK_TEMPLATE_FILE = "attini-cdk-template.json";

    public static final String ATTINI_SAM_TEMPLATE_FILE = "attini-sam-template.json";

    public static final String ATTINI_CDK_CHANGESET_TEMPLATE_FILE = "attini-cdk-changeset-template.json";

    public static final String ATTINI_IMPORT_TEMPLATE_FILE = "attini-import-template.json";

    public static final String ATTINI_INVOKE_TEMPLATE_FILE = "attini-lambda-invoke-template.json";


    public static final String ATTINI_CFN_MAP_TEMPLATE_FILE = "attini-map-template.json";

    public static final String ATTINI_MERGE_OUTPUT_FILE = "attini-merge-output-template.json";

    public static final String ATTINI_MANUAL_APPROVAL_FILE = "attini-manual-approval-template.json";


    @Override
    public File getDeployStateTemplate() {
        return new File(DEPLOY_DATA_STATE_JSON_FILE);
    }

    @Override
    public File getAttiniCfnTemplate() {
        return new File(ATTINI_CFN_TEMPLATE_FILE);
    }

    @Override
    public File getAttiniLambdaInvokeTemplate() {
        return new File(ATTINI_INVOKE_TEMPLATE_FILE);
    }

    @Override
    public File getAttiniRunnerTemplate() {
        return new File(ATTINI_RUNNER_TEMPLATE_FILE);
    }

    @Override
    public File getAttiniCdkTemplate() {
        return new File(ATTINI_CDK_TEMPLATE_FILE);
    }

    @Override
    public File getAttiniSamTemplate() {
        return new File(ATTINI_SAM_TEMPLATE_FILE);
    }

    @Override
    public File getAttiniCdkChangesetTemplate() {
        return new File(ATTINI_CDK_CHANGESET_TEMPLATE_FILE);
    }

    @Override
    public File getAttiniImportTemplate() {
        return new File(ATTINI_IMPORT_TEMPLATE_FILE);
    }

    @Override
    public File getAttiniManualApprovalTemplate() {
        return new File(ATTINI_MANUAL_APPROVAL_FILE);
    }


    @Override
    public File getAttiniMapCfnTemplate() {
        return new File(ATTINI_CFN_MAP_TEMPLATE_FILE);
    }

    @Override
    public File getAttiniMergeOutputTemplate() {
        return new File(ATTINI_MERGE_OUTPUT_FILE);
    }
}
