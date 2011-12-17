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

package org.fusesource.mqtt.client.callback;

import junit.framework.TestCase;
import org.fusesource.hawtbuf.Buffer;
import static org.fusesource.hawtbuf.Buffer.*;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.fusesource.mqtt.client.future.CallbackFuture;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class CallbackApiTest extends TestCase {
//    ApolloBroker broker = new ApolloBroker();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
//        broker.start();
    }

    @Override
    protected void tearDown() throws Exception {
//        broker.stop();
        super.tearDown();
    }

    public void testCallbackInterface() throws Exception {
        final CallbackFuture<Buffer> result = new CallbackFuture<Buffer>();
        MQTT.callback("localhost", 1883/* broker.port*/).clientId("Hiram").connect(new Callback<Connection>() {
            @Override
            public void failure(Throwable value) {
                result.failure(value);
            }

            @Override
            public void success(final Connection connection) {

                connection.listener(new Listener() {
                    @Override
                    public void failure(Throwable value) {
                        result.failure(value);
                        connection.close(null);
                    }

                    @Override
                    public void onPublish(UTF8Buffer topic, Buffer payload, Runnable onComplete) {
                        result.success(payload);
                        onComplete.run();
                    }
                });

                connection.resume();
                connection.subscribe(new Topic[]{new Topic(utf8("foo"), QoS.AT_LEAST_ONCE)}, new Callback<byte[]>(){
                    @Override
                    public void failure(Throwable value) {
                        result.failure(value);
                        connection.close(null);
                    }

                    @Override
                    public void success(byte[] value) {
                        connection.publish(utf8("foo"), ascii("Hello"), null);
                    }
                });

            }
        });

        assertEquals(ascii("Hello"), result.await().ascii());
    }
}
