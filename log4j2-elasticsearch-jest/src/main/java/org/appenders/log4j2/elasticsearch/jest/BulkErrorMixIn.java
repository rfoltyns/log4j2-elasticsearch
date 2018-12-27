package org.appenders.log4j2.elasticsearch.jest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkErrorMixIn {

    @JsonProperty("caused_by")
    BulkError causedBy;

}
