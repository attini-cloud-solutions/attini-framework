package attini.deploy.origin;


import attini.domain.DistributionId;
import attini.domain.DistributionName;

public class AttiniConfigException extends RuntimeException {

    private final ErrorCode errorCode;
    private final DistributionName distributionName;

    private final DistributionId distributionId;

    public AttiniConfigException(ErrorCode errorCode, String errorMessage, DistributionName distributionName,  DistributionId distributionId) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.distributionName = distributionName;
        this.distributionId = distributionId;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public DistributionId getDistributionId() {
        return distributionId;
    }

    public enum ErrorCode{
        INVALID_FORMAT, UNKNOWN, INVALID_PARAMETER_CONFIG
    }
}
