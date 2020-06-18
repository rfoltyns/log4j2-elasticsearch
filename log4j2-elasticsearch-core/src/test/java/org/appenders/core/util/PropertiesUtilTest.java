package org.appenders.core.util;

import org.appenders.core.logging.InternalLoggingTest;
import org.appenders.core.logging.Logger;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class PropertiesUtilTest {

    public static final int OFFSET = 1000;

    @Test
    public void getIntReturnsValueIfPropertyIsSet() {

        // given
        String propertyName = UUID.randomUUID().toString();
        int expectedValue = new Random().nextInt(1000) + OFFSET;
        System.setProperty(propertyName, String.valueOf(expectedValue));

        // when
        int result = PropertiesUtil.getInt(propertyName, 1);

        // then
        assertEquals(expectedValue, result);

    }

    @Test
    public void getIntReturnsDefaultIfPropertyIsNull() {

        // given
        String propertyName = UUID.randomUUID().toString();

        // when
        int result = PropertiesUtil.getInt(propertyName, 1);

        // then
        assertEquals(1, result);

    }

    @Test
    public void getIntReturnsDefaultIfPropertyIsEmpty() {

        // given
        String propertyName = UUID.randomUUID().toString();
        System.setProperty(propertyName, "");

        // when
        int result = PropertiesUtil.getInt(propertyName, 1);

        // then
        assertEquals(1, result);

    }

    @Test
    public void getIntReturnsDefaultIfPropertyIsBlank() {

        // given
        String propertyName = UUID.randomUUID().toString();
        System.setProperty(propertyName, "    ");

        // when
        int result = PropertiesUtil.getInt(propertyName, 1);

        // then
        assertEquals(1, result);

    }

    @Test
    public void getIntLogsIfDefaultIsReturned() {

        // given
        Logger logger = InternalLoggingTest.mockTestLogger();
        String propertyName = UUID.randomUUID().toString();
        System.setProperty(propertyName, "    ");

        // when
        int result = PropertiesUtil.getInt(propertyName, 1);

        // then
        assertEquals(1, result);

        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(logger).warn(eq("Property {} not found. Returning default: {}"), captor.capture());

        List args = captor.getAllValues();
        assertEquals(propertyName, args.get(0));
        assertEquals(1, args.get(1));

    }

    @Test
    public void getIntLogsIfValueCannotBeParsed() {

        // given
        Logger logger = InternalLoggingTest.mockTestLogger();

        String propertyName = UUID.randomUUID().toString();
        String nonIntegerValue = UUID.randomUUID().toString();
        System.setProperty(propertyName, nonIntegerValue);

        // when
        int result = PropertiesUtil.getInt(propertyName, 1);

        // then
        assertEquals(1, result);

        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(logger).error(eq("{} {} while parsing {}. Returning default: {}"), captor.capture());

        List args = captor.getAllValues();
        assertEquals(NumberFormatException.class.getSimpleName(), args.get(0));
        assertTrue(((String)args.get(1)).contains(nonIntegerValue));
        assertEquals(propertyName, args.get(2));
        assertEquals(1, args.get(3));

    }

}