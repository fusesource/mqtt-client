/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.mqtt.client.blocking;

import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.mqtt.client.callback.Callback;
import org.fusesource.mqtt.client.future.CallbackFuture;
import org.fusesource.mqtt.client.future.Future;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.util.concurrent.Executor;

/**
* <p>
* </p>
*
* @author <a href="http://hiramchirino.com">Hiram Chirino</a>
*/
public class ConnectionBuilder {
    
    private org.fusesource.mqtt.client.future.ConnectionBuilder next;

    public ConnectionBuilder(org.fusesource.mqtt.client.future.ConnectionBuilder next) {
        this.next = next;
    }

    public ConnectionBuilder blockingExecutor(Executor blockingExecutor) {
        next.blockingExecutor(blockingExecutor);
        return this;
    }

    public ConnectionBuilder cleanSession(boolean cleanSession) {
        next.cleanSession(cleanSession);
        return this;
    }

    public ConnectionBuilder clientId(String clientId) {
        next.clientId(clientId);
        return this;
    }

    public ConnectionBuilder dispatchQueue(DispatchQueue dispatchQueue) {
        next.dispatchQueue(dispatchQueue);
        return this;
    }

    public ConnectionBuilder keepAlive(short keepAlive) {
        next.keepAlive(keepAlive);
        return this;
    }

    public ConnectionBuilder localURI(URI localURI) {
        next.localURI(localURI);
        return this;
    }

    public ConnectionBuilder maxReadRate(int maxReadRate) {
        next.maxReadRate(maxReadRate);
        return this;
    }

    public ConnectionBuilder maxWriteRate(int maxWriteRate) {
        next.maxWriteRate(maxWriteRate);
        return this;
    }

    public ConnectionBuilder password(String password) {
        next.password(password);
        return this;
    }

    public ConnectionBuilder receiveBufferSize(int receiveBufferSize) {
        next.receiveBufferSize(receiveBufferSize);
        return this;
    }

    public ConnectionBuilder sendBufferSize(int sendBufferSize) {
        next.sendBufferSize(sendBufferSize);
        return this;
    }

    public ConnectionBuilder sslContext(SSLContext sslContext) {
        next.sslContext(sslContext);
        return this;
    }

    public ConnectionBuilder trafficClass(int trafficClass) {
        next.trafficClass(trafficClass);
        return this;
    }

    public ConnectionBuilder useLocalHost(boolean useLocalHost) {
        next.useLocalHost(useLocalHost);
        return this;
    }

    public ConnectionBuilder userName(String userName) {
        next.userName(userName);
        return this;
    }

    public ConnectionBuilder willMessage(String willMessage) {
        next.willMessage(willMessage);
        return this;
    }

    public ConnectionBuilder willQos(byte willQos) {
        next.willQos(willQos);
        return this;
    }

    public ConnectionBuilder willRetain(boolean willRetain) {
        next.willRetain(willRetain);
        return this;
    }

    public ConnectionBuilder willTopic(String willTopic) {
        next.willTopic(willTopic);
        return this;
    }

    public Connection connect() throws Exception {
        return new Connection(next.connect().await());
    }


}
