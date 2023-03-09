/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation.stackconfig;

import java.nio.file.Path;

public class S3PathUtil {


    public static String resolveS3PrefixedPath(String path) {
        String[] split = path.substring(5).split("/", 2);
        return "https://" +
               split[0] +
               ".s3.amazonaws.com/" +
               split[1];
    }

    public static String resolvePath(String path) {
        if (path.startsWith("https://")){
            //dont normalize https:// because it will remove the double slash
            return "https://" + Path.of(path.substring(8)).normalize();
        }
        return Path.of(path).normalize().toString();
    }
}
