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

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.hawtdispatch.DispatchQueue;

import java.util.ArrayList;
import java.util.LinkedList;

import static org.fusesource.hawtbuf.Buffer.utf8;

/**
 * <p>
 * A Future based optionally-blocking Connection interface to MQTT.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class FutureConnection {

    private final CallbackConnection next;

    private LinkedList<FutureCallback<Message>> receiveFutures = new LinkedList<FutureCallback<Message>>();
    private LinkedList<Message> receivedFrames = new LinkedList<Message>();

    public FutureConnection(CallbackConnection next) {
        this.next = next;
        this.next.listener(new Listener() {
            public void apply(UTF8Buffer topic, Buffer payload, Runnable onComplete) {
                getDispatchQueue().assertExecuting();
                Message msg = new Message(getDispatchQueue(), topic, payload, onComplete);
                if( receiveFutures.isEmpty() ) {
                    receivedFrames.add(msg);
                } else {
                    receiveFutures.removeFirst().apply(msg);
                }
            }
            public void failure(Throwable value) {
                getDispatchQueue().assertExecuting();
                ArrayList<FutureCallback<?>> tmp = new ArrayList<FutureCallback<?>>(receiveFutures);
                receiveFutures.clear();
                for (FutureCallback<?> future : tmp) {
                    future.failure(value);
                }
            }
        });
        this.next.resume();
    }

    private DispatchQueue getDispatchQueue() {
        return this.next.getDispatchQueue();
    }

    public Future<Void> disconnect() {
        final FutureCallback<Void> future = new FutureCallback<Void>();
        next.getDispatchQueue().execute(new Runnable() {
            public void run() {
                next.disconnect(future);
            }
        });
        return future;
    }

    public Future<byte[]> subscribe(final Topic[] topics) {
        final FutureCallback<byte[]> future = new FutureCallback<byte[]>();
        next.getDispatchQueue().execute(new Runnable() {
            public void run() {
                next.subscribe(topics, future);
            }
        });
        return future;
    }

    public Future<Void> publish(final String topic, final byte[] payload, final QoS qos, final boolean retain) {
        return publish(utf8(topic), new Buffer(payload), qos, retain);
    }

    public Future<Void> publish(final UTF8Buffer topic, final Buffer payload,  final QoS qos, final boolean retain) {
        final FutureCallback<Void> future = new FutureCallback<Void>();
        next.getDispatchQueue().execute(new Runnable() {
            public void run() {
                next.publish(topic, payload, qos, retain, future);
            }
        });
        return future;
    }

    public Future<Message> receive() {
        final FutureCallback<Message> future = new FutureCallback<Message>();
        getDispatchQueue().execute(new Runnable(){
            public void run() {
                if( next.failure()!=null ) {
                    future.failure(next.failure());
                } else {
                    if( receivedFrames.isEmpty() ) {
                        receiveFutures.add(future);
                    } else {
                        future.apply(receivedFrames.removeFirst());
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
