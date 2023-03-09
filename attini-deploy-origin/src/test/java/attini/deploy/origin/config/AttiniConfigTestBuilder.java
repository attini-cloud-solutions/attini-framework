package attini.deploy.origin.config;

import java.util.Collections;

import attini.deploy.origin.deploystack.AttiniInitDeployStackConfigTestBuilder;
import attini.domain.DistributionId;
import attini.domain.DistributionName;

public class AttiniConfigTestBuilder {

    public static AttiniConfigImpl.Builder aConfig() {
        return AttiniConfigImpl.builder()
                               .distributionId(DistributionId.of("123232323Testx"))
                               .distributionName(DistributionName.of("platform"))
                               .dependencies(Collections.emptyList())
                               .attiniInitDeployStackConfig(AttiniInitDeployStackConfigTestBuilder.aAttiniInitDeployStackConfig()
                                                                                                  .build())
                               .distributionTags(Collections.emptyMap());

    }
}
