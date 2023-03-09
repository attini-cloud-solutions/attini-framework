package attini.action.actions.deploycloudformation;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Objects;

public class ClientRequestToken {

    private final String token;

    private ClientRequestToken(String token) {
        this.token = requireNonNull(token, "token");
    }

    public String asString() {
        return token;
    }

    public static ClientRequestToken create(SfnExecutionArn sfnExecutionArn) {
        return new ClientRequestToken(sfnExecutionArn.extractExecutionId() + Instant.now().toEpochMilli());

    }

    @Override
    public String toString() {
        return "ClientRequestToken{" +
               "token='" + token + '\'' +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientRequestToken that = (ClientRequestToken) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }
}
