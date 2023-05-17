package attini.domain.polling;

public class PollerTimeoutException extends RuntimeException{

    public PollerTimeoutException(String message) {
        super(message);
    }
}
