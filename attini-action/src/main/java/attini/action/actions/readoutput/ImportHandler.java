package attini.action.actions.readoutput;

import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import attini.action.S3ClientFactory;
import attini.action.actions.readoutput.input.ImportInput;
import attini.action.system.EnvironmentVariables;
import attini.domain.json.AttiniJsonProvider;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;


public class ImportHandler {

    private static final Logger logger = Logger.getLogger(ImportHandler.class);

    private final S3ClientFactory s3ClientFactory;
    private final EnvironmentVariables environmentVariables;


    public ImportHandler(S3ClientFactory s3ClientFactory,
                         EnvironmentVariables environmentVariables) {
        this.s3ClientFactory = requireNonNull(s3ClientFactory, "s3ClientFactory");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    public Map<String, Object> readOutputs(ImportInput input) {

        Configuration configuration = Configuration.builder()
                                                   .jsonProvider(new AttiniJsonProvider())
                                                   .build();

        DocumentContext context = JsonPath.using(configuration).parse(getJson(input));

        return input.properties().mapping().entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                        try {
                            return context.read(entry.getValue());
                        } catch (PathNotFoundException e) {
                            throw new IllegalArgumentException("No results for path: " + entry.getValue(), e);
                        }
                    }));

    }

    public String getJson(ImportInput input) {

        if (input.properties().sourceType().equalsIgnoreCase("Distribution")) {
            return handleDistributionImport(input);
        }
        if (input.properties().sourceType().equalsIgnoreCase("S3")) {
            return handleS3Import(input);
        }

        throw new IllegalArgumentException("Unknown SourceType: " + input.properties().sourceType());

    }


    private String handleS3Import(ImportInput input) {
        logger.info("Performing S3 import");
        String key = input.properties().source().get("Key");
        if (key == null) {
            throw new IllegalArgumentException(
                    "Missing required property \"Key\" in AttiniImport step with SourceType S3");

        }

        String bucket = input.properties().source().get("Bucket");
        if (bucket == null) {
            throw new IllegalArgumentException(
                    "Missing required property \"Bucket\" in AttiniImport step with SourceType S3");

        }

        byte[] bytes = s3ClientFactory.getClient(input.properties().executionRole())
                                      .getObject(GetObjectRequest.builder()
                                                                 .key(key)
                                                                 .bucket(bucket)
                                                                 .build(), ResponseTransformer.toBytes())
                                      .asByteArray();
        return new String(bytes);

    }

    private String handleDistributionImport(ImportInput input) {
        logger.info("Performing distribution import");

        String name = input.properties().source().get("Name");
        if (name == null) {
            throw new IllegalArgumentException(
                    "Missing required property \"Name\" in AttiniImport step with SourceType Distribution");

        }
        if (!input.dependencies().containsKey(name)) {
            throw new IllegalArgumentException("Distribution \"" + name + "\" is not present among the dependencies in the payload. Make sure that the dependency is configured");

        }
        String url = input.dependencies().get(name).get("outputUrl");

        String key = getKey(url);

        byte[] bytes = s3ClientFactory.getClient(input.properties().executionRole())
                                      .getObject(GetObjectRequest.builder()
                                                                 .key(key)
                                                                 .bucket(environmentVariables.getAttiniArtifactBucket())
                                                                 .build(), ResponseTransformer.toBytes())
                                      .asByteArray();
        return new String(bytes);
    }

    private static String getKey(String url) {
        try {
            return new URL(url).getPath().substring(1);
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }


}
