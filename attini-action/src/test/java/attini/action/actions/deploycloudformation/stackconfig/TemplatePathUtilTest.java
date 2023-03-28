package attini.action.actions.deploycloudformation.stackconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


class TemplatePathUtilTest {

    @Test
    void shouldTransformS3Url() {

        String url = "s3://attini-artifact-store-eu-central-1-855066048591/stage/hello-world/2022-05-02_13:14:42/distribution-origin/attini-config.yaml";
        String expectedResult = "https://attini-artifact-store-eu-central-1-855066048591.s3.amazonaws.com/stage/hello-world/2022-05-02_13:14:42/distribution-origin/attini-config.yaml";

        assertEquals(expectedResult, TemplatePathUtil.getTemplatePath(url, "some-prefix-to-be-ignored"));
    }


    @Test
    void shouldTransformLocalUrl() {

        String url = "/my/path";
        String prefix = "some-prefix";
        String expectedResult = "some-prefix/my/path";

        assertEquals(expectedResult, TemplatePathUtil.getTemplatePath(url, prefix));
    }

    @Test
    void shouldTransformLocalUrl_handleNotStartingWithSlash() {

        String url = "my/path";
        String prefix = "some-prefix";
        String expectedResult = "some-prefix/my/path";

        assertEquals(expectedResult, TemplatePathUtil.getTemplatePath(url, prefix));
    }

    @Test
    void shouldTransformLocalUrl_handleStartingWithDots() {

        String url = "../path";
        String prefix = "some/prefix";
        String expectedResult = "some/path";

        assertEquals(expectedResult, TemplatePathUtil.getTemplatePath(url, prefix));
    }

    @Test
    void shouldTransformLocalUrl_handleStartingWithDots_multipleLevels() {

        String url = "../../../../../path";
        String prefix = "https://some/prefix/that/I/dont/like";
        String expectedResult = "https://some/path";

        String templatePath = TemplatePathUtil.getTemplatePath(url, prefix);
        assertEquals(expectedResult, templatePath);
    }


    @Test
    void shouldTransformLocalUrl_handleStartingWithSlashAndDots_multipleLevels() {

        String url = "/../../../../../path";
        String prefix = "https://some/prefix/that/I/dont/like";
        String expectedResult = "https://some/path";

        assertEquals(expectedResult, TemplatePathUtil.getTemplatePath(url, prefix));
    }
    @Test
    void shouldTransformLocalUrl_handleStartingWithSlashAndDots() {

        String url = "/../path";
        String prefix = "some/prefix";
        String expectedResult = "some/path";

        assertEquals(expectedResult, TemplatePathUtil.getTemplatePath(url, prefix));
    }

}
