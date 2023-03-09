/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.deploy.origin;

import attini.domain.DistributionId;
import attini.domain.DistributionName;

public class PublishDistributionException extends RuntimeException {
    private final DistributionName distributionName;
    private final DistributionId distributionId;


    public PublishDistributionException(DistributionName distributionName, DistributionId distributionId, String message) {
        super(message);
        this.distributionName = distributionName;
        this.distributionId = distributionId;
    }

    public PublishDistributionException(DistributionName distributionName, DistributionId distributionId,  Exception e) {
        super(e.getMessage(), e);
        this.distributionName = distributionName;
        this.distributionId = distributionId;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public DistributionId getDistributionId() {
        return distributionId;
    }
}
