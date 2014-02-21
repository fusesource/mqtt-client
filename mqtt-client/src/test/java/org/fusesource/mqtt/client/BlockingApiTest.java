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

import java.util.concurrent.TimeUnit;

import static org.fusesource.hawtbuf.Buffer.utf8;
import org.fusesource.mqtt.codec.MQTTFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class BlockingApiTest extends BrokerTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(BlockingApiTest.class);

    public void testInterface() throws Exception {
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", port);

        BlockingConnection connection = mqtt.blockingConnection();
        connection.connect();

        Topic[] topics = {new Topic(utf8("foo"), QoS.AT_LEAST_ONCE)};
        byte[] qoses = connection.subscribe(topics);

        connection.publish("foo", "Hello".getBytes(), QoS.AT_LEAST_ONCE, false);
        Message message = connection.receive();
        assertEquals("Hello", new String(message.getPayload())) ;

        // To let the server know that it has been processed.
        message.ack();

        connection.disconnect();
    }

    public void testInvalidClientId() throws Exception {
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", port);
        mqtt.setVersion("3.1.1");
        mqtt.setCleanSession(false);
        mqtt.setClientId((String) null);

        try {
            mqtt.blockingConnection();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        } catch (Throwable e) {
            fail("Unexpected exception: "+e);
        }

        // also test "" client id
        mqtt.setClientId("");
        try {
            mqtt.blockingConnection();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        } catch (Throwable e) {
            fail("Unexpected exception: "+e);
        }
    }

    public void testReceiveTimeout() throws Exception {
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", port);
        mqtt.setTracer(new Tracer() {
            @Override
            public void onReceive(MQTTFrame frame) {
                LOG.info("Client Received:\n" + frame);
            }

            @Override
            public void onSend(MQTTFrame frame) {
                LOG.info("Client Sent:\n" + frame);
            }

            @Override
            public void debug(String message, Object... args) {
                LOG.info(String.format(message, args));
            }
        });

        BlockingConnection connection = mqtt.blockingConnection();
        connection.connect();

        Topic[] topics = {new Topic(utf8("foo"), QoS.AT_LEAST_ONCE)};
        byte[] qoses = connection.subscribe(topics);

        // force a receive timeout
        Message message = connection.receive(1000, TimeUnit.MILLISECONDS);
        assertNull(message);

        connection.publish("foo", "Hello".getBytes(), QoS.AT_LEAST_ONCE, false);
        message = connection.receive(5000, TimeUnit.MILLISECONDS);
        assertNotNull(message);
        assertEquals("Hello", new String(message.getPayload()));

        // To let the server know that it has been processed.
        message.ack();

        connection.disconnect();
    }
}
