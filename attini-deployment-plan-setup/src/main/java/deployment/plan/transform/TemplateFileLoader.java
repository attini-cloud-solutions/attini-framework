/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
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

    File getAttiniImportTemplate();

    File getAttiniManualApprovalTemplate();


    File getAttiniMapCfnTemplate();

    File getAttiniMergeOutputTemplate();

}
