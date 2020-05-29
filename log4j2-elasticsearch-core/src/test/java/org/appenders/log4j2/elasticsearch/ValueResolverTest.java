package org.appenders.log4j2.elasticsearch;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValueResolverTest {

    @Test
    public void noopResolverDoesNotChangeGivenValue() {

        // given
        String expected = UUID.randomUUID().toString();

        // when
        String resolved = ValueResolver.NO_OP.resolve(expected);

        // then
        assertSame(expected, resolved);

    }

    @Test
    public void noopResolverReturnsCurrentVirtualPropertyValue() {

        // given
        VirtualProperty property = mock(VirtualProperty.class);
        String expected = UUID.randomUUID().toString();
        when(property.getValue()).thenReturn(expected);

        // when
        String resolved = ValueResolver.NO_OP.resolve(property);

        // then
        assertSame(expected, resolved);

    }

}