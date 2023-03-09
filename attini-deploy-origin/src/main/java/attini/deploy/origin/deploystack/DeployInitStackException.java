package attini.deploy.origin.deploystack;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

public class DeployInitStackException extends RuntimeException {

    public DeployInitStackException(AwsServiceException cause) {
        super(cause);
    }

    public String getAwsErrorMessage() {
        AwsServiceException awsServiceException = (AwsServiceException) this.getCause();
        return awsServiceException.awsErrorDetails().errorMessage();
    }

    public String getAwsErrorCode() {
        AwsServiceException awsServiceException = (AwsServiceException) this.getCause();
        return awsServiceException.awsErrorDetails().errorCode();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
