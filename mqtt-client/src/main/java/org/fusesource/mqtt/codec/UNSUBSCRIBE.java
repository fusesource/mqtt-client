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

package org.fusesource.mqtt.codec;

import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.QoS;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import static org.fusesource.mqtt.codec.MessageSupport.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class UNSUBSCRIBE extends MessageSupport.HeaderBase implements Message, Acked {

    public static final byte TYPE = 10;
    public static final UTF8Buffer[] NO_TOPICS = new UTF8Buffer[0];

    private short messageId;
    private UTF8Buffer topics[] = NO_TOPICS;

    public UNSUBSCRIBE() {
        qos(QoS.AT_LEAST_ONCE);
    }

    public byte messageType() {
        return TYPE;
    }

    public UNSUBSCRIBE decode(MQTTFrame frame) throws ProtocolException {
        assert(frame.buffers.length == 1);
        if (frame.header != -94) {
            frame.header = -94;
        }
        header(frame.header());

        DataByteArrayInputStream is = new DataByteArrayInputStream(frame.buffers[0]);

        QoS qos = qos();
        if(qos != QoS.AT_MOST_ONCE) {
            messageId = is.readShort();
        }
        ArrayList<UTF8Buffer> list = new ArrayList<UTF8Buffer>();
        while(is.available() > 0) {
            list.add(MessageSupport.readUTF(is));
        }
        topics = list.toArray(new UTF8Buffer[list.size()]);
        return this;
    }
    
    public MQTTFrame encode() {
        try {
            DataByteArrayOutputStream os = new DataByteArrayOutputStream();
            QoS qos = qos();
            if(qos != QoS.AT_MOST_ONCE) {
                os.writeShort(messageId);
            }
            for(UTF8Buffer topic: topics) {
                MessageSupport.writeUTF(os, topic);
            }

            MQTTFrame frame = new MQTTFrame();
            frame.header(header());
            frame.commandType(TYPE);
            return frame.buffer(os.toBuffer());
        } catch (IOException e) {
            throw new RuntimeException("The impossible happened");
        }
    }

    @Override
    public boolean dup() {
        return super.dup();
    }

    @Override
    public UNSUBSCRIBE dup(boolean dup) {
        return (UNSUBSCRIBE) super.dup(dup);
    }

    @Override
    public QoS qos() {
        return super.qos();
    }

    public short messageId() {
        return messageId;
    }

    public UNSUBSCRIBE messageId(short messageId) {
        this.messageId = messageId;
        return this;
    }

    public UTF8Buffer[] topics() {
        return topics;
    }

    public UNSUBSCRIBE topics(UTF8Buffer[] topics) {
        this.topics = topics;
        return this;
    }

    @Override
    public String toString() {
        return "UNSUBSCRIBE{" +
                "dup=" + dup() +
                ", qos=" + qos() +
                ", messageId=" + messageId +
                ", topics=" + (topics == null ? null : Arrays.asList(topics)) +
                '}';
    }
}
