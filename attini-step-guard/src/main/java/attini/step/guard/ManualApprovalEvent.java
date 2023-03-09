package attini.step.guard;

import attini.domain.DistributionName;
import attini.domain.Environment;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@Builder
@EqualsAndHashCode
@Getter
public class ManualApprovalEvent {

    @NonNull
    private final DistributionName distributionName;
    @NonNull
    private final Environment environment;
    @NonNull
    private final String stepName;
    @NonNull
    private final String sfnToken;
    private final String message;
    private final boolean abort;
}
