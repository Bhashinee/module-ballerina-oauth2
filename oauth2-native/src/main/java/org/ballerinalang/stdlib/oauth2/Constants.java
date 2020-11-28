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

import io.ballerina.runtime.api.Module;

import static io.ballerina.runtime.api.constants.RuntimeConstants.BALLERINA_BUILTIN_PKG_PREFIX;

/**
 * Constants related to Ballerina OAuth2 stdlib.
 */
public class Constants {
    public static final String PACKAGE_NAME = "oauth2";
    public static final Module OAUTH2_PACKAGE_ID = new Module(BALLERINA_BUILTIN_PKG_PREFIX, PACKAGE_NAME, "1.0.3");
    public static final String OAUTH2_ERROR_TYPE = "OAuth2Error";

    public static final String HTTP_VERSION = "httpVersion";
    public static final String DISABLE = "disable";
    public static final String SECURE_SOCKET = "secureSocket";
    public static final String TRUSTSTORE = "trustStore";
    public static final String PATH = "path";
    public static final String PASSWORD = "password";
    public static final String CUSTOM_HEADERS = "customHeaders";
    public static final String CUSTOM_PAYLOAD = "customPayload";

    public static final String TLS = "TLS";
    public static final String PKCS12 = "PKCS12";
    public static final String HTTP_2 = "HTTP_2";

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
}
