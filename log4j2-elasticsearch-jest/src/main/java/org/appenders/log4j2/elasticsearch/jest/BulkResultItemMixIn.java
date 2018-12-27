package org.appenders.log4j2.elasticsearch.jest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(value = "index")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonIgnoreProperties(ignoreUnknown = true, value = {"_version", "result", "_shards", "_primary_term", "_seq_no"})
public abstract class BulkResultItemMixIn {

    @JsonProperty("_id")
    String id;

    @JsonProperty("_type")
    String type;

    @JsonProperty("_index")
    String index;

    @JsonProperty("error")
    BulkError bulkError;

}
