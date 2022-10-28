package org.appenders.log4j2.elasticsearch.ahc.thirdparty;

/*
 * Copyright 2014 The Netty Project
 *
 * Modification (rfoltyns): File parameters changed to FileInputStream to make use of AutoCloseable in try clauses
 * Modification (rfoltyns): readPrivateKey replaced with bcpkix based impl
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * NOTE (rfoltyns): Source found at https://gist.github.com/dain/29ce5c135796c007f9ec88e82ab21822
 * NOTE (rfoltyns): Cast to java.nio.Buffer added
 *
 */

import org.appenders.log4j2.elasticsearch.ahc.tls.KeyReader;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

public final class PemReader
{
    private static final Pattern CERT_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                    "([a-z0-9+/=\\r\\n]+)" +                    // Base64 text
                    "-+END\\s+.*CERTIFICATE[^-]*-+",            // Footer
            CASE_INSENSITIVE);

    private PemReader() {}

    public static KeyStore loadTrustStore(final FileInputStream certificateChainFile)
            throws IOException, GeneralSecurityException
    {
        final KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);

        final List<X509Certificate> certificateChain = readCertificateChain(certificateChainFile);
        for (X509Certificate certificate : certificateChain) {
            final X500Principal principal = certificate.getSubjectX500Principal();
            keyStore.setCertificateEntry(principal.getName("RFC2253"), certificate);
        }
        return keyStore;
    }

    public static KeyStore loadKeyStore(final FileInputStream certificateChainFis, final FileInputStream privateKeyFis, final Optional<String> keyPassword)
            throws IOException, GeneralSecurityException
    {
        final PKCS8EncodedKeySpec encodedKeySpec = new KeyReader().readPrivateKey(privateKeyFis, keyPassword);
        PrivateKey key;
        try {
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            key = keyFactory.generatePrivate(encodedKeySpec);
        }
        catch (InvalidKeySpecException ignore) {
            final KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            key = keyFactory.generatePrivate(encodedKeySpec);
        }

        final List<X509Certificate> certificateChain = readCertificateChain(certificateChainFis);
        if (certificateChain.isEmpty()) {
            throw new CertificateException("Certificate file does not contain any certificates");
        }

        final KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setKeyEntry("key", key, keyPassword.orElse("").toCharArray(), certificateChain.stream().toArray(Certificate[]::new));
        return keyStore;
    }

    private static List<X509Certificate> readCertificateChain(final FileInputStream certificateChainFis)
            throws IOException, GeneralSecurityException
    {
        final String contents = readFile(certificateChainFis);

        final Matcher matcher = CERT_PATTERN.matcher(contents);
        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        final List<X509Certificate> certificates = new ArrayList<>();

        int start = 0;
        while (matcher.find(start)) {
            final byte[] buffer = base64Decode(matcher.group(1));
            certificates.add((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(buffer)));
            start = matcher.end();
        }

        return certificates;
    }

    private static byte[] base64Decode(final String base64)
    {
        return Base64.getMimeDecoder().decode(base64.getBytes(US_ASCII));
    }

    private static String readFile(final FileInputStream fis)
            throws IOException
    {
        try (Reader reader = new InputStreamReader(fis, US_ASCII)) {
            final StringBuilder stringBuilder = new StringBuilder();

            final CharBuffer buffer = CharBuffer.allocate(2048);
            while (reader.read(buffer) != -1) {
                ((Buffer)buffer).flip();
                stringBuilder.append(buffer);
                ((Buffer)buffer).clear();
            }
            return stringBuilder.toString();
        }
    }
}
