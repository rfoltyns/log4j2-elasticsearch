package org.appenders.log4j2.elasticsearch.tls;

/*-
 * #%L
 * log4j-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Optional;

public class KeyReader {

    public PKCS8EncodedKeySpec readPrivateKey(FileInputStream fis, Optional<String> keyPassword)
            throws IOException {
        PEMParser keyReader = new PEMParser(new InputStreamReader(fis));

        PEMDecryptorProvider decryptorProvider = new JcePEMDecryptorProviderBuilder().build(keyPassword.get().toCharArray());

        Object keyPair = keyReader.readObject();
        keyReader.close();

        PrivateKeyInfo keyInfo;

        if (keyPair instanceof PEMEncryptedKeyPair) {
            PEMKeyPair decryptedKeyPair = ((PEMEncryptedKeyPair) keyPair).decryptKeyPair(decryptorProvider);
            keyInfo = decryptedKeyPair.getPrivateKeyInfo();
        } else {
            keyInfo = ((PEMKeyPair) keyPair).getPrivateKeyInfo();
        }

        return new PKCS8EncodedKeySpec(keyInfo.getEncoded());
    }
}