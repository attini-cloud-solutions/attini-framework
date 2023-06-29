package attini.action.facades.artifactstore;

import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.facades.S3Facade;
import attini.action.facades.stackdata.DistributionDataFacade;
import attini.action.system.EnvironmentVariables;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;

public class ArtifactStoreFacade {

    private final S3Facade s3Facade;
    private final String bucketName;
    private final ObjectMapper objectMapper;
    private final EnvironmentVariables environmentVariables;
    private final DistributionDataFacade distributionDataFacade;

    public ArtifactStoreFacade(S3Facade s3Facade,
                               String bucketName,
                               ObjectMapper objectMapper,
                               EnvironmentVariables environmentVariables,
                               DistributionDataFacade distributionDataFacade) {
        this.s3Facade = requireNonNull(s3Facade, "s3Facade");
        this.bucketName = requireNonNull(bucketName, "bucketName");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.distributionDataFacade = requireNonNull(distributionDataFacade, "distributionDataFacade");
    }

    public void saveDistributionOutput(Environment environment,
                                       DistributionName distributionName,
                                       DistributionId distributionId, JsonNode jsonNode) {
        try {
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(jsonNode);
            s3Facade.saveObject(bucketName,
                                createOutputKey(environment,
                                                distributionName,
                                                distributionId),
                                bytes);
            distributionDataFacade.updateDistributionOutput(distributionName,
                                                            environment,
                                                            getOutputUrl(environment,
                                                                         distributionName,
                                                                         distributionId));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Could not parse output", e);
        }
    }

    private static String createOutputKey(Environment environment,
                                          DistributionName distributionName,
                                          DistributionId distributionId) {
        return "outputs/" + environment.asString() + "/" + distributionName.asString() + "/" + distributionId.asString() + "/output.json";
    }

    public String getOutputUrl(Environment environment,
                               DistributionName distributionName,
                               DistributionId distributionId) {
        return "https://" +
               bucketName +
               ".s3." +
               environmentVariables.getRegion() +
               ".amazonaws.com/" +
               createOutputKey(environment, distributionName, distributionId);

    }


}
