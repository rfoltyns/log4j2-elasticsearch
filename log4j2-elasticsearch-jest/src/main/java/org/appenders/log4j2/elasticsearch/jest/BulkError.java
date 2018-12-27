package org.appenders.log4j2.elasticsearch.jest;

public class BulkError {

    private String type;

    private String reason;

    private BulkError causedBy;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BulkError getCausedBy() {
        return causedBy;
    }

    public void setCausedBy(BulkError causedBy) {
        this.causedBy = causedBy;
    }

    StringBuilder appendErrorMessage(StringBuilder sb) {
        if (getCausedBy() != null) {
            return getCausedBy().appendErrorMessage(sb);
        }
        sb.append("errorType: ").append(getType());
        sb.append(", errorReason: ").append(getReason());
        return sb;
    }

}
