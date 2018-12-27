package org.appenders.log4j2.elasticsearch.jest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BulkResultDeserializer extends JsonDeserializer {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        int took = 0;
        boolean errors = false;
        BulkError error = null;
        int status = -1;
        List<BulkResultItem> items = null;

        String fieldName;
        while ((fieldName = p.nextFieldName()) != null)  {
            switch (fieldName) {
                case "took": {
                    took = p.nextIntValue(-1);
                    break;
                }
                case "errors": {
                    errors = p.nextBooleanValue();
                    break;
                }
                case "status": {
                    status = p.nextIntValue(-1);
                    break;
                }
                case "error": {
                    p.nextValue(); // skip to START_OBJECT or VALUE_NULL
                    JsonDeserializer<Object> typeDeserializer = ctxt.findNonContextualValueDeserializer(ctxt.constructType(BulkError.class));
                    error = (BulkError) typeDeserializer.deserialize(p, ctxt);
                    break;
                }
                case "items": {
                    if (errors) {
                        items = new ArrayList<>();
                        p.nextValue(); // skip to START_ARRAY
                        p.nextValue(); // skip to START_OBJECT
                        ObjectMapper mapper = (ObjectMapper) p.getCodec();
                        MappingIterator<BulkResultItem> bulkResultItemMappingIterator = mapper.readValues(p, BulkResultItem.class);

                        while (bulkResultItemMappingIterator.hasNext()) {
                            items.add(bulkResultItemMappingIterator.next());
                        }
                    }
                    break;
                }
            }
        }

        return new BufferedBulkResult(took, errors, error, status, items);
    }

}
