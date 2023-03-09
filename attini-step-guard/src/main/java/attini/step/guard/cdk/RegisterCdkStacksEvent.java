package attini.step.guard.cdk;


import java.util.List;
import java.util.Map;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record RegisterCdkStacksEvent(String requestType,
                                     DistributionId distributionId,
                                     DistributionName distributionName,
                                     Environment environment,
                                     ObjectIdentifier objectIdentifier,
                                     String stepName,
                                     List<CdkStack> stacks,
                                     Map<String, Map<String, Object>> outputs) {
}
