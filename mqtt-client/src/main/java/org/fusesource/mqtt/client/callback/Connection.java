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

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.mqtt.client.codec.MQTTFrame;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.fusesource.mqtt.client.codec.*;
import org.fusesource.mqtt.client.codec.CommandSupport.*;
import org.fusesource.hawtdispatch.transport.Transport;
import org.fusesource.hawtdispatch.transport.TransportListener;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;


/**
 * <p>
 * A callback based non/blocking Connection interface to MQTT.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Connection {

    private static class Request {
        final MQTTFrame frame;
        final Callback<?> cb;

        Request(MQTTFrame frame, Callback<?> cb) {
            this.cb = cb;
            this.frame = frame;
        }
    }

    private final DispatchQueue queue;
    private final ConnectionBuilder builder;

    private Transport transport;
    private Listener listener = new Listener();
    private Runnable refiller;

    private HashMap<Short, Request> requests = new HashMap<Short, Request>();
    private LinkedList<Request> overflow = new LinkedList<Request>();
    private HashSet<Short> processed = new HashSet<Short>();

    private Throwable failure;

    Connection(Transport transport, ConnectionBuilder builder) {
        this.queue = transport.getDispatchQueue();
        this.transport = transport;
        this.builder = builder;
        this.transport.setTransportListener(new TransportListener() {
            public void onTransportCommand(Object command) {
                processFrame((MQTTFrame) command);
            }
            public void onRefill() {
                drainOverflow();
            }
            public void onTransportFailure(IOException error) {
                processFailure(error);
            }
            public void onTransportConnected() {
            }
            public void onTransportDisconnected() {
            }
        });
    }


    public Transport transport() {
        return transport;
    }

    public DispatchQueue getDispatchQueue() {
        return queue;
    }

    public void resume() {
        this.transport.resumeRead();
    }
    public void suspend() {
        this.transport.suspendRead();
    }

    public void close(final Runnable onComplete) {
        this.transport.stop(new Runnable() {
            public void run() {
                if( onComplete!=null ) {
                    failRequests(new ClosedChannelException());
                    onComplete.run();
                }
            }
        });
    }

    public Connection refiller(Runnable refiller) {
        queue.assertExecuting();
        this.refiller = refiller;
        return this;
    }

    public Connection listener(Listener listener) {
        queue.assertExecuting();
        this.listener = listener;
        return this;
    }

    public boolean full() {
        queue.assertExecuting();
        return this.transport.full();
    }

    public Throwable failure() {
        queue.assertExecuting();
        return failure;
    }

    public void publish(UTF8Buffer topic, Buffer payload, Callback<Void> cb) {
        publish(topic, QoS.AT_MOST_ONCE, false, payload, cb);
    }

    public void publish(UTF8Buffer topic, QoS qos, boolean retain, Buffer payload, Callback<Void> cb) {
        queue.assertExecuting();
        PUBLISH command = new PUBLISH().qos(qos).retain(retain);
        command.topicName(topic).setPayload(payload);
        send(command, cb);
    }

    public void subscribe(Topic[] topics, Callback<byte[]> cb) {
        queue.assertExecuting();
        send(new SUBSCRIBE().topics(topics), cb);
    }

    public void unsubscribe(UTF8Buffer[] topics, Callback<Void> cb) {
        queue.assertExecuting();
        send(new UNSUBSCRIBE().topics(topics), cb);
    }

    private void send(Acked command, Callback<?> cb) {
        if( failure !=null ) {
            if( cb!=null ) {
                cb.failure(failure);
            }
        } else {
            MQTTFrame frame = null;
            switch(command.qos()) {
                case AT_LEAST_ONCE: case EXACTLY_ONCE:
                    short id = getNextMessageId();
                    command.messageId(id);
                    frame = command.encode();
                    this.requests.put(id, new Request(frame, cb));
                    cb = null;
                    // Yes we want to fall through to the next case...
                case AT_MOST_ONCE:
                    if(frame==null) {
                        frame = command.encode();
                    }
                    send(frame, cb);
                    break;
            }
        }
    }

    private void send(MQTTFrame frame, Callback<?> cb) {
        if( overflow.isEmpty() && this.transport.offer(frame) ) {
            if( cb!=null ) {
                cb.success(null);
            }
        } else {
            overflow.addLast(new Request(frame, cb));
        }
    }

    short nextMessageId = 1;
    private short getNextMessageId() {
        short rc = nextMessageId;
        nextMessageId++;
        if(nextMessageId==0) {
            nextMessageId=1;
        }
        return rc;
    }

    private void drainOverflow() {
        queue.assertExecuting();
        if( overflow.isEmpty() ){
            return;
        }
        Request entry;
        while((entry=overflow.peek())!=null) {
            if( this.transport.offer(entry.frame) ) {
                overflow.removeFirst();
                if( entry.cb!=null ) {
                    entry.cb.success(null);
                }
            } else {
                break;
            }
        }
        if( overflow.isEmpty() ) {
            if( refiller!=null ) {
                try {
                    refiller.run();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void completeRequest(short id, byte originalType, Object arg) {
        Request request = requests.remove(id);
        if( request!=null ) {
            assert originalType==request.frame.commandType();
            if(request.cb!=null) {
                ((Callback<Object>)request.cb).success(arg);
            }
        } else {
            processFailure(new ProtocolException("Command from server contained an invalid message id: "+id));
        }
    }

    private void processFrame(MQTTFrame frame) {
        try {
            switch(frame.commandType()) {
                case PUBLISH.TYPE: {
                    PUBLISH publish = new PUBLISH().decode(frame);
                    toReceiver(publish);
                    break;
                }
                case PUBREL.TYPE:{
                    PUBREL ack = new PUBREL().decode(frame);
                    processed.remove(ack.messageId());
                    PUBCOMP reponse = new PUBCOMP();
                    reponse.messageId(ack.messageId());
                    send(reponse.encode(), null);
                }
                case PUBACK.TYPE:{
                    PUBACK ack = new PUBACK().decode(frame);
                    completeRequest(ack.messageId(), PUBLISH.TYPE, null);
                    break;
                }
                case PUBREC.TYPE:{
                    PUBREC ack = new PUBREC().decode(frame);
                    PUBREL reponse = new PUBREL();
                    reponse.messageId(ack.messageId());
                    send(reponse.encode(), null);
                    break;
                }
                case PUBCOMP.TYPE:{
                    PUBCOMP ack = new PUBCOMP().decode(frame);
                    completeRequest(ack.messageId(), PUBLISH.TYPE, null);
                    break;
                }
                case SUBACK.TYPE: {
                    SUBACK ack = new SUBACK().decode(frame);
                    completeRequest(ack.messageId(), SUBSCRIBE.TYPE, ack.grantedQos());
                    break;
                }
                case UNSUBACK.TYPE: {
                    UNSUBACK ack = new UNSUBACK().decode(frame);
                    completeRequest(ack.messageId(), UNSUBSCRIBE.TYPE, null);
                    break;
                }
                default:
                    System.out.println(frame.commandType());
                    // throw new ProtocolException("Unexpected MQTT command type: "+frame.commandType());
            }
        } catch (Throwable e) {
            processFailure(e);
        }
    }

    static public final Runnable NOOP = new Runnable() {
        public void run() {
        }
    };

    private void toReceiver(final PUBLISH publish) {
        if( listener !=null ) {
            try {
                Runnable cb = NOOP;
                switch( publish.qos() ) {
                    case AT_LEAST_ONCE:
                        cb = new Runnable() {
                            public void run() {
                                PUBACK reponse = new PUBACK();
                                reponse.messageId(publish.messageId());
                                send(reponse.encode(), null);
                            }
                        };
                        break;
                    case EXACTLY_ONCE:
                        cb = new Runnable() {
                            public void run() {
                                PUBREC reponse = new PUBREC();
                                reponse.messageId(publish.messageId());
                                processed.add(publish.messageId());
                                send(reponse.encode(), null);
                            }
                        };
                        // It might be a dup.
                        if( processed.contains(publish.messageId()) ) {
                            cb.run();
                            return;
                        }
                        break;
                    case AT_MOST_ONCE:
                }
                listener.onPublish(publish.topicName(), publish.payload(), cb);
            } catch (Throwable e) {
                processFailure(e);
            }
        }
    }

    private void processFailure(Throwable error) {
        if( failure == null ) {
            failure = error;
            failRequests(failure);
            if( listener !=null ) {
                try {
                    listener.failure(failure);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void failRequests(Throwable failure) {
        ArrayList<Request> values = new ArrayList(requests.values());
        requests.clear();
        for (Request value : values) {
            if( value.cb!= null ) {
                value.cb.failure(failure);
            }
        }

        ArrayList<Request> overflowEntries = new ArrayList<Request>(overflow);
        overflow.clear();
        for (Request entry : overflowEntries) {
            if( entry.cb !=null ) {
                entry.cb.failure(failure);
            }
        }
    }

}
