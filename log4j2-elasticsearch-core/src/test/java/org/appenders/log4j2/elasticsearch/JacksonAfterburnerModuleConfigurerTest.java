package org.appenders.log4j2.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.appenders.log4j2.elasticsearch.JacksonAfterburnerModuleConfigurer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class JacksonAfterburnerModuleConfigurerTest {

    @Test
    public void configuresAfterburnerModule() {

        // given
        JacksonAfterburnerModuleConfigurer configurer = new JacksonAfterburnerModuleConfigurer();

        ObjectMapper objectMapper = spy(new ObjectMapper());

        // when
        configurer.configure(objectMapper);

        // then
        ArgumentCaptor<AfterburnerModule> captor = ArgumentCaptor.forClass(AfterburnerModule.class);
        verify(objectMapper).registerModule(captor.capture());

        assertEquals(AfterburnerModule.class, captor.getValue().getClass());

    }

}
