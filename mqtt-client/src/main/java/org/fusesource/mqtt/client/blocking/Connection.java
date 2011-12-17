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

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.fusesource.mqtt.client.future.CallbackFuture;

/**
 * <p>
 * A blocking Connection interface to MQTT.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Connection {

    private final org.fusesource.mqtt.client.future.Connection next;

    Connection(org.fusesource.mqtt.client.future.Connection next) {
        this.next = next;
    }

    public void close() throws Exception {
        this.next.close().await();
    }

    public byte[] subscribe(final Topic[] topics) throws Exception {
        return this.next.subscribe(topics).await();
    }

    public void publish(final UTF8Buffer topic, final QoS qos, final boolean retain, final Buffer payload) throws Exception {
        this.next.publish(topic, qos, retain, payload).await();
    }

    public void publish(final UTF8Buffer topic, final Buffer payload) throws Exception {
        this.next.publish(topic, payload).await();
    }

    public Message receive() throws Exception {
        return this.next.receive().await();
    }

    public void resume() {
        next.resume();
    }

    public void suspend() {
        next.suspend();
    }
}
