/**
 * Copyright (C) 2010-2012, FuseSource Corp.  All rights reserved.
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

package org.fusesource.mqtt.client;

import junit.framework.TestCase;
import org.fusesource.hawtbuf.Buffer;

import static org.fusesource.hawtbuf.Buffer.ascii;
import static org.fusesource.hawtbuf.Buffer.utf8;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class FutureApiTest extends BrokerTestSupport {

    public void testInterface() throws Exception {
        final Promise<Buffer> result = new Promise<Buffer>();

        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", port);

        FutureConnection connection = mqtt.futureConnection();
        Future<Void> f1 = connection.connect();
        f1.await();

        Future<byte[]> f2 = connection.subscribe(new Topic[]{new Topic(utf8("foo"), QoS.AT_LEAST_ONCE)});
        byte[] qoses = f2.await();

        // We can start future receive..
        Future<Message> receive = connection.receive();

        // send the message..
        connection.publish("foo", "Hello".getBytes(), QoS.AT_LEAST_ONCE, false);

        // Then the receive will get the message.
        Message message = receive.await();
        assertEquals("Hello", new String(message.getPayload()));

        // To let the server know that it has been processed.
        message.ack();

        connection.disconnect().await();

    }
}
