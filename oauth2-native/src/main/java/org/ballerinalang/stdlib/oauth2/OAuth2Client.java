/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.oauth2;

import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Extern function to call introspection endpoint and get the response.
 */
public class OAuth2Client {

    public static Object doHttpRequest(BString url, BMap<BString, Object> clientConfig, BMap<BString, BString> headers,
                                       BString payload) {
        String customPayload = clientConfig.containsKey(StringUtils.fromString(Constants.CUSTOM_PAYLOAD)) ?
                clientConfig.getStringValue(StringUtils.fromString(Constants.CUSTOM_PAYLOAD)).getValue() : null;
        String textPayload = payload.getValue();
        if (customPayload != null) {
            textPayload += "&" + customPayload;
        }

        BMap<BString, BString> customHeaders = clientConfig.containsKey(
                StringUtils.fromString(Constants.CUSTOM_HEADERS)) ?
                (BMap<BString, BString>) clientConfig.getMapValue(StringUtils.fromString(Constants.CUSTOM_HEADERS)) :
                null;
        HttpRequest request;
        if (customHeaders != null) {
            ArrayList<String> headersList = new ArrayList<>();
            for (Map.Entry<BString, BString> entry : headers.entrySet()) {
                headersList.add(entry.getKey().getValue());
                headersList.add(entry.getValue().getValue());
            }
            for (Map.Entry<BString, BString> entry : customHeaders.entrySet()) {
                headersList.add(entry.getKey().getValue());
                headersList.add(entry.getValue().getValue());
            }
            String[] flatHeaders = headersList.toArray(String[]::new);
            request = buildHttpRequest(url.getValue(), flatHeaders, textPayload);
        } else {
            request = buildHttpRequest(url.getValue(), textPayload);
        }

        String httpVersion = clientConfig.getStringValue(StringUtils.fromString(Constants.HTTP_VERSION)).getValue();
        BMap<BString, Object> secureSocket = clientConfig.containsKey(StringUtils.fromString(Constants.SECURE_SOCKET)) ?
                (BMap<BString, Object>) clientConfig.getMapValue(StringUtils.fromString(Constants.SECURE_SOCKET)) :
                null;
        if (secureSocket != null) {
            boolean disable = secureSocket.getBooleanValue(StringUtils.fromString(Constants.DISABLE));
            if (disable) {
                try {
                    SSLContext sslContext = initSslContext();
                    HttpClient client = buildHttpClient(httpVersion, sslContext);
                    return callEndpoint(client, request);
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    return createError("Failed to init SSL context. " + e.getMessage());
                }
            }
            BMap<BString, Object> trustStore = secureSocket.containsKey(StringUtils.fromString(Constants.TRUSTSTORE)) ?
                    (BMap<BString, Object>) secureSocket.getMapValue(StringUtils.fromString(Constants.TRUSTSTORE)) :
                    null;
            if (trustStore != null) {
                try {
                    SSLContext sslContext = initSslContext(trustStore);
                    HttpClient client = buildHttpClient(httpVersion, sslContext);
                    return callEndpoint(client, request);
                } catch (Exception e) {
                    return createError("Failed to init SSL context with truststore. " + e.getMessage());
                }
            }
        }
        HttpClient client = buildHttpClient(httpVersion);
        return callEndpoint(client, request);
    }

    private static HttpClient.Version getHttpVersion(String httpVersion) {
        if (Constants.HTTP_2.equals(httpVersion)) {
            return HttpClient.Version.HTTP_2;
        }
        return HttpClient.Version.HTTP_1_1;
    }

    private static SSLContext initSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance(Constants.TLS);
        sslContext.init(null, trustAllCerts, new SecureRandom());
        return sslContext;
    }

    private static SSLContext initSslContext(BMap<BString, Object> trustStore) throws Exception {
        String path = trustStore.getStringValue(StringUtils.fromString(Constants.PATH)).getValue();
        String password = trustStore.getStringValue(StringUtils.fromString(Constants.PASSWORD)).getValue();
        InputStream is = new FileInputStream(new File(path));
        char[] passphrase = password.toCharArray();
        KeyStore ks = KeyStore.getInstance(Constants.PKCS12);
        ks.load(is, passphrase);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, passphrase);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        SSLContext sslContext = SSLContext.getInstance(Constants.TLS);
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    private static HttpClient buildHttpClient(String httpVersion) {
        return HttpClient.newBuilder().version(getHttpVersion(httpVersion)).build();
    }

    private static HttpClient buildHttpClient(String httpVersion, SSLContext sslContext) {
        return HttpClient.newBuilder().version(getHttpVersion(httpVersion)).sslContext(sslContext).build();
    }

    private static HttpRequest buildHttpRequest(String url, String payload) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .setHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_FORM_URLENCODED)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
    }

    private static HttpRequest buildHttpRequest(String url, String[] headers, String payload) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .headers(headers)
                .setHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_FORM_URLENCODED)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
    }

    private static Object callEndpoint(HttpClient client, HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return StringUtils.fromString(response.body());
            }
            return createError("Failed to get a success response from introspection endpoint. Response Code: " +
                                       response.statusCode());
        } catch (IOException | InterruptedException e) {
            return createError("Failed to send the request to introspection endpoint. " + e.getMessage());
        }
    }

    private static BError createError(String errMsg) {
        return ErrorCreator.createDistinctError(Constants.OAUTH2_ERROR_TYPE, Constants.OAUTH2_PACKAGE_ID,
                                                StringUtils.fromString(errMsg));
    }
}
