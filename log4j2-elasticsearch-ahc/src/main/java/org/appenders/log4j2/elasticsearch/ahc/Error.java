package org.appenders.log4j2.elasticsearch.ahc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * Carries error description of {@link BatchResult} or {@link BatchItemResult}
 */
public class Error {

    private Error[] rootCause;

    private String type;

    private String reason;

    private Error causedBy;

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    public Error getCausedBy() {
        return causedBy;
    }

    public void setCausedBy(final Error causedBy) {
        this.causedBy = causedBy;
    }

    public Error[] getRootCause() {
        return rootCause;
    }

    public void setRootCause(final Error[] rootCause) {
        this.rootCause = rootCause;
    }

    StringBuilder appendErrorMessage(final StringBuilder sb) {
        if (getCausedBy() != null) {
            return getCausedBy().appendErrorMessage(sb);
        }
        sb.append("errorType: ").append(getType());
        sb.append(", errorReason: ").append(getReason());
        return sb;
    }

}
