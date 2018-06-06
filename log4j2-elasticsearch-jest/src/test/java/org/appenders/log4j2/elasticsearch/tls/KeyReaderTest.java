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


import org.appenders.log4j2.elasticsearch.jest.PEMCertInfoTest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Optional;

public class KeyReaderTest {

    @Test
    public void canReadPrivateKeyWithNoPassword() throws IOException {

        // given
        KeyReader keyReader = new KeyReader();
        FileInputStream pemPKey = new FileInputStream(PEMCertInfoTest.TEST_KEY_PATH);

        // when
        PKCS8EncodedKeySpec keySpec = keyReader.readPrivateKey(pemPKey, Optional.ofNullable(""));

        // then
        Assert.assertNotNull(keySpec);
    }

    @Test
    public void canReadPrivateKeyWithPassword() throws IOException {

        // given
        Security.addProvider(new BouncyCastleProvider());

        KeyReader keyReader = new KeyReader();
        FileInputStream pemPKey = new FileInputStream(PEMCertInfoTest.TEST_KEY_PATH_WITH_PASSPHRASE);

        // when
        PKCS8EncodedKeySpec keySpec = keyReader.readPrivateKey(pemPKey,
                Optional.ofNullable(PEMCertInfoTest.TEST_KEY_PASSPHRASE)
        );

        // then
        Assert.assertNotNull(keySpec);
    }
}
