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

    private LinkedList<FutureCB1<Message>> receiveFutures = new LinkedList<FutureCB1<Message>>();
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
                ArrayList<FutureCB1<?>> tmp = new ArrayList<FutureCB1<?>>(receiveFutures);
                receiveFutures.clear();
                for (FutureCB1<?> future : tmp) {
                    future.failure(value);
                }
            }
        });
        this.next.resume();
    }

    private DispatchQueue getDispatchQueue() {
        return this.next.getDispatchQueue();
    }

    public Future1<Void> close() {
        final FutureCB1<Void> future = new FutureCB1<Void>();
        next.close(new Runnable() {
            public void run() {
                future.apply(null);
            }
        });
        return future;
    }

    public Future1<byte[]> subscribe(final Topic[] topics) {
        final FutureCB1<byte[]> future = new FutureCB1<byte[]>();
        next.getDispatchQueue().execute(new Runnable() {
            public void run() {
                next.subscribe(topics, future);
            }
        });
        return future;
    }

    public Future0 publish(final String topic, final byte[] payload, final QoS qos, final boolean retain) {
        return publish(utf8(topic), new Buffer(payload), qos, retain);
    }

    public Future0 publish(final UTF8Buffer topic, final Buffer payload,  final QoS qos, final boolean retain) {
        final FutureCB0 future = new FutureCB0();
        next.getDispatchQueue().execute(new Runnable() {
            public void run() {
                next.publish(topic, payload, qos, retain, future);
            }
        });
        return future;
    }

    public Future1<Message> receive() {
        final FutureCB1<Message> future = new FutureCB1<Message>();
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
