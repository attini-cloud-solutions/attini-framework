package attini.action.actions.runner;

public class Ec2FailedToStartException extends RuntimeException{

    public Ec2FailedToStartException(String message) {
        super(message);
    }
}
