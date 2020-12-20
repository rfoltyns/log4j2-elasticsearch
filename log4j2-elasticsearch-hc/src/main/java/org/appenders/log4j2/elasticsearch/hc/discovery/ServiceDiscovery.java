package org.appenders.log4j2.elasticsearch.hc.discovery;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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

public interface ServiceDiscovery {

    /**
     * @param listener Listener to get notified if {@link #refresh()} finds changes in the {@link ServerInfo}s
     */
    void addListener(final ServerInfoListener listener);

    /**
     * Refreshes the list of {@link ServerInfo}s. MUST notify registered listeners of the changes if any.
     */
    void refresh();

}
