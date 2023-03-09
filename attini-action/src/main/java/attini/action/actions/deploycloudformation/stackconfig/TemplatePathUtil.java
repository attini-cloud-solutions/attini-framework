/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation.stackconfig;

public class TemplatePathUtil {

    public static String getTemplatePath(String templatePath, String templatePathPrefix){
        if (templatePath.startsWith("https://")) {
            return templatePath;
        }  else if(templatePath.startsWith("s3://")) {
                return S3PathUtil.resolveS3PrefixedPath(templatePath);
        }else{
            String path = templatePath.startsWith("/") ? templatePath : "/"+templatePath;
            return S3PathUtil.resolvePath(String.format("%s%s", templatePathPrefix, path));
        }

    }
}
