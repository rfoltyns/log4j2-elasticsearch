package org.appenders.log4j2.elasticsearch.jest;

import io.searchbox.action.GenericJestRequestIntrospector;
import io.searchbox.client.config.ElasticsearchVersion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.UUID;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class GenericJestRequestTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderThrowsByDefault() {

        // given
        GenericJestRequest.EmptyBuilder builder = new GenericJestRequest.EmptyBuilder();

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("No need to use builder. Create directly");

        // when
        builder.build();

    }

    @Test
    public void inheritedBuildURIDelegates() {

        // given
        String httpMethodName = UUID.randomUUID().toString();
        String source = UUID.randomUUID().toString();

        GenericJestRequest request = spy(new GenericJestRequest(httpMethodName, source) {
            @Override
            public String buildURI() {
                return null;
            }
        });

        // when
        request.buildURI(ElasticsearchVersion.UNKNOWN);

        // then
        verify(request).buildURI();

    }

    @Test
    public void parametersAreNotModified() {

        // given
        String httpMethodName = UUID.randomUUID().toString();
        String source = UUID.randomUUID().toString();


        // when
        GenericJestRequest request = spy(new GenericJestRequest(httpMethodName, source) {
            @Override
            public String buildURI() {
                return null;
            }
        });

        // then
        assertSame(httpMethodName, request.getRestMethodName());
        assertSame(source, GenericJestRequestIntrospector.getPayload(request));

    }

}