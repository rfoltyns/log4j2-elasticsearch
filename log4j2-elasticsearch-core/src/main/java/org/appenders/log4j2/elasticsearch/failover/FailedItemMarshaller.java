package org.appenders.log4j2.elasticsearch.failover;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import io.netty.buffer.ByteBuf;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ReadResolvable;
import net.openhft.chronicle.hash.serialization.BytesReader;
import net.openhft.chronicle.hash.serialization.BytesWriter;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ExtendedObjectMapper;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.ReleaseCallback;
import org.appenders.log4j2.elasticsearch.StringItemSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FailedItemMarshaller implements BytesWriter<ItemSource>, BytesReader<ItemSource>,
        ReadResolvable<FailedItemMarshaller> {

    private static final Logger LOG = InternalLogging.getLogger();

    private ObjectMapper objectMapper;

    public FailedItemMarshaller() {
        readResolve();
    }

    /**
     * @return a COPY(!) of underlying ObjectMapper
     */
    public ObjectMapper objectMapper() {
        return objectMapper.copy();
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        // noop - no fields to marshall
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        // noop - no fields to unmarshall
    }

    @Override
    public FailedItemMarshaller readResolve() {

        if (objectMapper != null) {
            return this;
        }

        this.objectMapper = createConfiguredObjectMapper();

        return this;

    }

    ObjectMapper createConfiguredObjectMapper() {
        return new ExtendedObjectMapper(new JsonFactory())
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .configure(SerializationFeature.CLOSE_CLOSEABLE, false)
                .addMixIn(FailedItemInfo.class, FailedItemInfoMixIn.class)
                .addMixIn(ItemSource.class, ItemSourceMixIn.class)
                .addMixIn(FailedItemSource.class, FailedItemSourceDelegateMixIn.class)
                .addMixIn(ByteBufItemSource.class, ByteBufItemSourceMixIn.class)
                .addMixIn(StringItemSource.class, StringItemSourceMixIn.class)
                .addMixIn(ByteBuf.class, CompositeByteBufMixIn.class)
                .addMixIn(KeySequenceConfigKeys.class, KeySequenceConfigKeysMixIn.class)
                .addMixIn(KeySequenceConfig.class, KeySequenceConfigMixIn.class)
                .setInjectableValues(new InjectableValues.Std()
                        .addValue("releaseCallback", (ReleaseCallback<ByteBuf>) source -> source.getSource().release()))
                .setVisibility(VisibilityChecker.Std.defaultInstance()
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    }

    @Override
    public ItemSource read(Bytes in, @Nullable ItemSource using) {
        try {
            return objectMapper.readValue(in.inputStream(), ItemSource.class);
        } catch (Exception e) {
            LOG.error(
                    "{} deserialization failed: {}. Returning null..",
                    FailedItemSource.class.getSimpleName(),
                    e.getMessage()
            );
        }
        return null;
    }

    @Override
    public void write(Bytes out, @NotNull ItemSource toWrite) {
        try {
            objectMapper.writeValue(out.outputStream(), toWrite);
        } catch (Exception e) {
            LOG.error(
                    "{} serialization failed: {}",
                    FailedItemSource.class.getSimpleName(),
                    e.getMessage()
            );
        }
    }

}
