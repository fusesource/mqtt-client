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

import junit.framework.TestCase;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;


import static org.fusesource.hawtbuf.Buffer.ascii;
import static org.fusesource.hawtbuf.Buffer.utf8;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class BlockingApiTest extends TestCase {

    public void testCallbackInterface() throws Exception {
        Connection connection = MQTT.blocking("localhost", 1883/* broker.port*/).clientId("Hiram").connect();
        byte[] qoses = connection.subscribe(new Topic[]{new Topic(utf8("foo"), QoS.AT_LEAST_ONCE)});

        connection.publish(utf8("foo"), ascii("Hello"));
        Message message = connection.receive();
        assertEquals(ascii("Hello"), message.getPayload().ascii());

        // To let the server know that it has been processed.
        message.ack();
    }
}
