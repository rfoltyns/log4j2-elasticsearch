package org.appenders.log4j2.elasticsearch.jest;

import io.searchbox.action.BulkableAction;
import io.searchbox.core.Bulk;
import io.searchbox.core.Index;
import io.searchbox.core.JestBatchIntrospector;
import org.appenders.log4j2.elasticsearch.BatchIntrospector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExtendedBulkTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderBuildsSuccessfully() {

        // given
        ExtendedBulk.Builder builder = new ExtendedBulk.Builder();

        // when
        ExtendedBulk bulk = builder.build();

        // then
        assertNotNull(bulk);

    }

    @Test
    public void builderAddAddsSingleElement() {

        // given
        ExtendedBulk.Builder builder = new ExtendedBulk.Builder();
        BatchIntrospector<Bulk> introspector = new JestBatchIntrospector();

        String source = UUID.randomUUID().toString();
        Index action = new Index.Builder(source).build();

        // when
        builder.addAction(action);

        // then
        ExtendedBulk bulk = builder.build();
        assertEquals(1, introspector.items(bulk).size());

    }

    @Test
    public void builderAddCollectionAddsAllElements() {

        // given
        ExtendedBulk.Builder builder = new ExtendedBulk.Builder();
        BatchIntrospector<Bulk> introspector = new JestBatchIntrospector();

        String source = UUID.randomUUID().toString();
        Index action = new Index.Builder(source).build();

        int randomSize = new Random().nextInt(1000) + 10;
        Collection<BulkableAction> actions = new ArrayList<>(randomSize);
        for (int ii = 0; ii < randomSize; ii++) {
            actions.add(action);
        }

        // when
        builder.addAction(actions);

        // then
        ExtendedBulk bulk = builder.build();
        assertEquals(randomSize, introspector.items(bulk).size());

    }
}
