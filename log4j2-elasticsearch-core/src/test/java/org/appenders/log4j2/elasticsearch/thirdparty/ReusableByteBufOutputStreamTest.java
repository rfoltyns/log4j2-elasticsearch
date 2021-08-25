package org.appenders.log4j2.elasticsearch.thirdparty;

import io.netty.buffer.ByteBuf;
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReusableByteBufOutputStreamTest {

    @Test
    public void canResetTheUnderlyingBuffer() {

        // given
        final ByteBuf expectedBuffer = ByteBufItemSourceTest.createDefaultTestByteBuf();
        final ReusableByteBufOutputStream outputStream = new ReusableByteBufOutputStream(ByteBufItemSourceTest.createDefaultTestByteBuf());

        // when
        outputStream.reset(expectedBuffer);

        // then
        assertEquals(expectedBuffer, outputStream.buffer());

    }

}