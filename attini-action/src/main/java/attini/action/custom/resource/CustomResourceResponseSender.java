/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.custom.resource;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.jboss.logging.Logger;

public class CustomResourceResponseSender {
    private static final Logger logger = Logger.getLogger(CustomResourceResponseSender.class);

    public void sendResponse(String url, CustomResourceResponse response){
        try {
            RequestConfig requestConfig = RequestConfig.custom()
                                                       .setConnectTimeout(5000)
                                                       .setSocketTimeout(30000)
                                                       .build();
            CloseableHttpClient httpClient =
                    HttpClientBuilder.create()
                                     .setRetryHandler((exception, executionCount, context) -> {
                                         try {
                                             TimeUnit.SECONDS.sleep(2);
                                         } catch (InterruptedException e) {
                                             throw new RuntimeException("Interrupted while waiting for next retry", e);
                                         }
                                         return executionCount < 60;
                                     })
                                     .addInterceptorFirst(new Interceptor())
                                     .setDefaultRequestConfig(requestConfig)
                                     .build();


            HttpPut httpPut = new HttpPut(url);
            httpPut.setEntity(new StringEntity(response.toJsonString()));
            CloseableHttpResponse httpResponse = httpClient.execute(httpPut);

            logger.info("Response Code: " + httpResponse.getStatusLine().getStatusCode());
            httpClient.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Interceptor implements HttpResponseInterceptor {

        @Override
        public void process(HttpResponse httpResponse, HttpContext httpContext) throws IOException {
            if (!String.valueOf(httpResponse.getStatusLine().getStatusCode()).startsWith("2")) {
                logger.warn("Got status code " + httpResponse.getStatusLine()
                                                             .getStatusCode() + " when responding to custom resource, will trigger retry");
                throw new IOException("Retry");
            }
        }
    }

}
