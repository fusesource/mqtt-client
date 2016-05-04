/**
 * Copyright (C) 2016, Red Hat Inc.  All rights reserved.
 *
 *     http://redhat.com
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
import java.util.concurrent.atomic.AtomicLong;

import static org.fusesource.hawtbuf.Buffer.utf8;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class BlockingApiBenchmark {


    static public void main(String []args) throws Exception {
        MQTT mqtt = new MQTT();
        mqtt.setConnectAttemptsMax(0);
        mqtt.setReconnectAttemptsMax(0);
        mqtt.setHost("localhost", 1883);

        final BlockingConnection publishConnection = mqtt.blockingConnection();
        publishConnection.connect();

        final BlockingConnection subscribeConnection = mqtt.blockingConnection();
        subscribeConnection.connect();
        subscribeConnection.setReceiveBuffer(1024*64);

        Topic[] topic = {new Topic(utf8("foo"), QoS.EXACTLY_ONCE)};
        byte[] qoses = subscribeConnection.subscribe(topic);

        final long start = System.currentTimeMillis();
        final AtomicLong sendCounter = new AtomicLong();
        final AtomicLong receiveCounter = new AtomicLong();

        Thread receiver = new Thread("receiver") {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (System.currentTimeMillis() > start + TimeUnit.SECONDS.toMillis(120)) {
                            break;
                        }
                        Thread.sleep(10);
                        subscribeConnection.receive().ack();
                        receiveCounter.incrementAndGet();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        receiver.start();

        Thread sender = new Thread("sender"){
            @Override
            public void run() {
                try {
                    while (true) {
                        if (System.currentTimeMillis() > start + TimeUnit.SECONDS.toMillis(120)) {
                            break;
                        }
                        publishConnection.publish("foo", new byte[1024], QoS.EXACTLY_ONCE, false);
                        sendCounter.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        sender.start();

        while (true) {
            if (System.currentTimeMillis() > start + TimeUnit.SECONDS.toMillis(120)) {
                break;
            }
            Thread.sleep(1000);
            System.out.println("Sent: "+sendCounter.get()+", Received: " + receiveCounter.get());
        }

        publishConnection.disconnect();
        subscribeConnection.disconnect();
        receiver.join();
        sender.join();

    }
}