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

package org.fusesource.mqtt.client.future;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.fusesource.mqtt.client.callback.Listener;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * <p>
 * A Future based optionally-blocking Connection interface to MQTT.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Connection {

    private final org.fusesource.mqtt.client.callback.Connection next;

    private LinkedList<CallbackFuture<Message>> receiveFutures = new LinkedList<CallbackFuture<Message>>();
    private LinkedList<Message> receivedFrames = new LinkedList<Message>();

    Connection(org.fusesource.mqtt.client.callback.Connection next) {
        this.next = next;
        this.next.listener(new Listener() {
            @Override
            public void failure(Throwable value) {
                getDispatchQueue().assertExecuting();
                ArrayList<CallbackFuture<?>> tmp = new ArrayList<CallbackFuture<?>>(receiveFutures);
                receiveFutures.clear();
                for (CallbackFuture<?> future : tmp) {
                    future.failure(value);
                }
            }

            @Override
            public void onPublish(UTF8Buffer topic, Buffer payload, Runnable onComplete) {
                getDispatchQueue().assertExecuting();
                Message msg = new Message(getDispatchQueue(), topic, payload, onComplete);
                if( receiveFutures.isEmpty() ) {
                    receivedFrames.add(msg);
                } else {
                    receiveFutures.removeFirst().success(msg);
                }
            }
        });
        this.next.resume();
    }

    private DispatchQueue getDispatchQueue() {
        return this.next.getDispatchQueue();
    }

    public CallbackFuture<Void> close() {
        final CallbackFuture<Void> future = new CallbackFuture<Void>();
        next.close(new Runnable() {
            public void run() {
                future.success(null);
            }
        });
        return future;
    }

    public CallbackFuture<byte[]> subscribe(final Topic[] topics) {
        final CallbackFuture<byte[]> future = new CallbackFuture<byte[]>();
        next.getDispatchQueue().execute(new Runnable() {
            public void run() {
                next.subscribe(topics, future);
            }
        });
        return future;
    }

    public CallbackFuture<Void> publish(final UTF8Buffer topic, final QoS qos, final boolean retain, final Buffer payload) {
        final CallbackFuture<Void> future = new CallbackFuture<Void>();
        next.getDispatchQueue().execute(new Runnable() {
            public void run() {
                next.publish(topic, qos, retain, payload, future);
            }
        });
        return future;
    }

    public CallbackFuture<Void> publish(final UTF8Buffer topic, final Buffer payload) {
        final CallbackFuture<Void> future = new CallbackFuture<Void>();
        next.getDispatchQueue().execute(new Runnable() {
            public void run() {
                next.publish(topic, payload, future);
            }
        });
        return future;
    }

    public CallbackFuture<Message> receive() {
        final CallbackFuture<Message> future = new CallbackFuture<Message>();
        getDispatchQueue().execute(new Runnable(){
            public void run() {
                if( next.failure()!=null ) {
                    future.failure(next.failure());
                } else {
                    if( receivedFrames.isEmpty() ) {
                        receiveFutures.add(future);
                    } else {
                        future.success(receivedFrames.removeFirst());
                    }
                }
            }
        });
        return future;
    }

    public void resume() {
        next.resume();
    }

    public void suspend() {
        next.suspend();
    }
}
