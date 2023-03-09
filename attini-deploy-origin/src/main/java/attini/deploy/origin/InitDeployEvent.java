package attini.deploy.origin;

import static java.util.Objects.requireNonNull;

import attini.domain.Environment;
import attini.domain.ObjectIdentifier;

public class InitDeployEvent {
    private final String s3Bucket;
    private final String s3Key;
    private final ObjectIdentifier objectIdentifier;
    private final String userIdentity;

    public InitDeployEvent(String s3Bucket, String s3Key, ObjectIdentifier objectIdentifier, String userIdentity) {
        this.s3Bucket = requireNonNull(s3Bucket, "s3Bucket");
        this.s3Key = requireNonNull(s3Key, "s3Key");
        this.objectIdentifier = requireNonNull(objectIdentifier, "objectIdentifier");
        this.userIdentity = requireNonNull(userIdentity, "userIdentity");
    }


    public String getS3Key() {
        return s3Key;
    }

    public String getFileName() {
        return s3Key.substring(s3Key.lastIndexOf("/") + 1);
    }

    public String getFolderName() {
        String[] dirs = s3Key.split("/");
        return dirs[dirs.length-2];
    }
    public String getS3Bucket() {
        return s3Bucket;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public String getUserIdentity() {
        return userIdentity;
    }

    public Environment getEnvironmentName() {
        String[] dirs = s3Key.split("/");
        return Environment.of(dirs[0]);
    }
}

