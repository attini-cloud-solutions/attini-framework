package attini.deploy.origin.deploystack;

import attini.deploy.origin.config.InitDeployStackConfig;

public class AttiniInitDeployStackConfigTestBuilder {

    public static InitDeployStackConfig.Builder aAttiniInitDeployStackConfig() {
        return InitDeployStackConfig.builder()
                                    .attiniInitDeployTemplatePath("testpath");
    }
}
