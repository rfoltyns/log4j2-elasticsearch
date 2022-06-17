package org.appenders.log4j2.elasticsearch.ahc.tls;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import org.appenders.log4j2.elasticsearch.ahc.PEMCertInfoTest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KeyReaderTest {

    @Test
    public void canReadPrivateKeyWithNoPassword() throws IOException {

        // given
        final KeyReader keyReader = new KeyReader();
        final FileInputStream pemPKey = new FileInputStream(PEMCertInfoTest.TEST_KEY_PATH);

        // when
        final PKCS8EncodedKeySpec keySpec = keyReader.readPrivateKey(pemPKey, Optional.of(""));

        // then
        assertNotNull(keySpec);
    }

    @Test
    public void canReadPrivateKeyWithPassword() throws IOException {

        // given
        Security.addProvider(new BouncyCastleProvider());

        final KeyReader keyReader = new KeyReader();
        final FileInputStream pemPKey = new FileInputStream(PEMCertInfoTest.TEST_KEY_PATH_WITH_PASSPHRASE);

        // when
        final PKCS8EncodedKeySpec keySpec = keyReader.readPrivateKey(pemPKey,
                Optional.ofNullable(PEMCertInfoTest.TEST_KEY_PASSPHRASE)
        );

        // then
        assertNotNull(keySpec);

    }

}
