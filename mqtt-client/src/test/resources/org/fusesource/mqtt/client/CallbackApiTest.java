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

package org.fusesource.mqtt.client;

import junit.framework.TestCase;
import org.fusesource.hawtbuf.Buffer;
import static org.fusesource.hawtbuf.Buffer.*;
import org.fusesource.hawtbuf.UTF8Buffer;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class CallbackApiTest extends TestCase {
//    ApolloBroker broker = new ApolloBroker();
//
//    @Override
//    protected void setUp() throws Exception {
//        super.setUp();
//        broker.start();
//    }
//
//    @Override
//    protected void tearDown() throws Exception {
//        broker.stop();
//        super.tearDown();
//    }

    public void testCallbackInterface() throws Exception {
        final Promise<Buffer> result = new Promise<Buffer>();
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", 1883 /* broker.port*/);
        mqtt.setClientId("Hiram");
        mqtt.connectCallback(new Callback<CallbackConnection>() {
            // Once we connect..
            public void onSuccess(final CallbackConnection connection) {

                // Start add a listener to process subscirption messages, and start the
                // resume the connection so it starts receiving messages from the socket.
                connection.listener(new Listener() {
                    public void apply(UTF8Buffer topic, Buffer payload, Runnable onComplete) {
                        result.onSuccess(payload);
                        onComplete.run();
                    }

                    public void failure(Throwable value) {
                        result.onFailure(value);
                        connection.disconnect(null);
                    }
                }).resume();

                // Subscribe to a topic foo
                Topic[] topics = {new Topic(utf8("foo"), QoS.AT_LEAST_ONCE)};
                connection.subscribe(topics, new Callback<byte[]>() {
                    public void onSuccess(byte[] value) {

                        // Once subscribed, publish a message on the same topic.
                        connection.publish("foo", "Hello".getBytes(), QoS.AT_LEAST_ONCE, false, null);

                    }

                    public void onFailure(Throwable value) {
                        result.onFailure(value);
                        connection.disconnect(null);
                    }
                });

            }

            public void onFailure(Throwable value) {
                result.onFailure(value);
            }
        });

        assertEquals("Hello", new String(result.await().toByteArray()));
    }
}
