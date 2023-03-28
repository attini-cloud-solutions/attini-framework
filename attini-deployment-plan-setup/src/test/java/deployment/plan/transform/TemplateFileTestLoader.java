/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package deployment.plan.transform;

import java.io.File;
import java.util.Objects;

public class TemplateFileTestLoader implements TemplateFileLoader {
    @Override
    public File getDeployStateTemplate() {
        return new File(Objects.requireNonNull(this.getClass().getResource("/templates/deploy-data-state.json")).getFile());
    }

    @Override
    public File getAttiniCfnTemplate() {
        return new File(Objects.requireNonNull(this.getClass().getResource("/templates/attini-cfn-template.json")).getFile());

    }

    @Override
    public File getAttiniLambdaInvokeTemplate() {
        return new File(Objects.requireNonNull(this.getClass()
                                                   .getResource("/templates/attini-lambda-invoke-template.json")).getFile());
    }

    @Override
    public File getAttiniMapCfnTemplate() {
        return new File(Objects.requireNonNull(this.getClass().getResource("/templates/attini-map-template.json")).getFile());
    }

    @Override
    public File getAttiniRunnerTemplate() {
        return new File(Objects.requireNonNull(this.getClass().getResource("/templates/attini-runner-template.json")).getFile());
    }

    @Override
    public File getAttiniCdkTemplate() {
        return new File(Objects.requireNonNull(this.getClass().getResource("/templates/attini-cdk-template.json")).getFile());

    }

    @Override
    public File getAttiniCdkChangesetTemplate() {
        return new File(Objects.requireNonNull(this.getClass()
                                                   .getResource("/templates/attini-cdk-changeset-template.json")).getFile());
    }

    @Override
    public File getAttiniImportTemplate() {
        return new File(Objects.requireNonNull(this.getClass().getResource("/templates/attini-import-template.json")).getFile());
    }

    @Override
    public File getAttiniManualApprovalTemplate() {
        return new File(Objects.requireNonNull(this.getClass()
                                                   .getResource("/templates/attini-manual-approval-template.json")).getFile());

    }

    @Override
    public File getAttiniMergeOutputTemplate() {
        return new File(Objects.requireNonNull(this.getClass()
                                                   .getResource("/templates/attini-merge-output-template.json")).getFile());
    }
}
