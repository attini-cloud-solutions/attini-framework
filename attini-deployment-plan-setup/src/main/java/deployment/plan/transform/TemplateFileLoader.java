/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package deployment.plan.transform;

import java.io.File;

public interface TemplateFileLoader {
    File getDeployStateTemplate();

    File getAttiniCfnTemplate();

    File getAttiniLambdaInvokeTemplate();


    File getAttiniRunnerTemplate();

    File getAttiniCdkTemplate();

    File getAttiniSamTemplate();

    File getAttiniCdkChangesetTemplate();

    File getAttiniImportTemplate();

    File getAttiniManualApprovalTemplate();


    File getAttiniMapCfnTemplate();

    File getAttiniMergeOutputTemplate();

}
