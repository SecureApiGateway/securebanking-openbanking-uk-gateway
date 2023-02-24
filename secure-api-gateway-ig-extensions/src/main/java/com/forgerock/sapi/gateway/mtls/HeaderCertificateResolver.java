/*
 * Copyright © 2020-2022 ForgeRock AS (obst@forgerock.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.forgerock.sapi.gateway.mtls;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.forgerock.http.protocol.Request;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forgerock.sapi.gateway.fapi.FAPIUtils;

/**
 * CertificateResolver implementation that resolves the client's mTLS certificate from a HTTP Request Header.
 *
 * The certificateHeaderName field is used to control which header is used to resolve the cert.
 * The header value is expected to be a PEM encoded then URL encoded X509 certificate.
 */
public class HeaderCertificateResolver implements CertificateResolver {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String certificateHeaderName;

    public HeaderCertificateResolver(String certificateHeaderName) {
        Reject.ifBlank(certificateHeaderName, "certificateHeaderName must be provided");
        this.certificateHeaderName = certificateHeaderName;
    }

    @Override
    public X509Certificate resolveCertificate(Context context, Request request) throws CertificateException {
        final String fapInteractionId = FAPIUtils.getFapiInteractionIdForDisplay(context);
        final String headerValue = request.getHeaders().getFirst(certificateHeaderName);
        if (headerValue == null) {
            logger.debug("({}) No client cert could be found for header: {}", fapInteractionId, certificateHeaderName);
            throw new CertificateException("No client cert could be found for header: " + certificateHeaderName);
        }
        final String certPem;
        try {
             certPem = URLDecoder.decode(headerValue, StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            logger.debug("(" + fapInteractionId + ") Failed to URL decode cert from header: " + certificateHeaderName, ex);
            throw new CertificateException("Failed to URL decode certificate header value. " +
                    "Expect certificate in PEM encoded then URL encoded format", ex);
        }
        logger.debug("({}) Found client cert: {}", fapInteractionId, certPem);
        return parseCertificate(certPem);
    }

    static X509Certificate parseCertificate(String cert) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate certificate = cf.generateCertificate(new ByteArrayInputStream(cert.getBytes(StandardCharsets.UTF_8)));
        if (!(certificate instanceof X509Certificate)) {
            throw new CertificateException("client tls cert must be in X.509 format");
        }
        return (X509Certificate) certificate;
    }
}