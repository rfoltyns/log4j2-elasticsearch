package org.appenders.log4j2.elasticsearch.jest;

public class BulkResultItem {

    private String id;

    private String type;

    private String index;

    private int status;

    private BulkError bulkError;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public BulkError getBulkError() {
        return bulkError;
    }

    public void setBulkError(BulkError bulkError) {
        this.bulkError = bulkError;
    }

}