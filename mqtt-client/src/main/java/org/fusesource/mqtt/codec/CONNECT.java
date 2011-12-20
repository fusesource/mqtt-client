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

package org.fusesource.mqtt.codec;

import org.fusesource.hawtbuf.*;

import java.io.IOException;
import java.net.ProtocolException;
import static org.fusesource.mqtt.codec.CommandSupport.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class CONNECT implements Command {

    public static final byte TYPE = 1;
    
    private static final Buffer PROTOCOL_NAME = new AsciiBuffer("MQIsdp").buffer();
    private static final int PROTOCOL_VERSION = 3;

    private short keepAlive = 30;
    private UTF8Buffer clientId;
    private UTF8Buffer willTopic;
    private UTF8Buffer willMessage;
    private boolean willRetain;
    private byte willQos;
    private boolean cleanSession = true;
    private UTF8Buffer userName;
    private UTF8Buffer password;


    public CONNECT(){
    }

    public CONNECT(CONNECT other) {
        this.keepAlive = other.keepAlive;
        this.clientId = other.clientId;
        this.willTopic = other.willTopic;
        this.willMessage = other.willMessage;
        this.willRetain = other.willRetain;
        this.willQos = other.willQos;
        this.cleanSession = other.cleanSession;
        this.userName = other.userName;
        this.password = other.password;
    }

    public byte getType() {
        return TYPE;
    }

    public CONNECT decode(MQTTFrame frame) throws ProtocolException {
        assert(frame.buffers.length == 1);
        DataByteArrayInputStream is = new DataByteArrayInputStream(frame.buffers[0]);

        if( !PROTOCOL_NAME.equals(CommandSupport.readUTF(is)) ) {
            throw new ProtocolException("Invalid CONNECT encoding");
        }
        int version = is.readByte();
        if( version != PROTOCOL_VERSION ) {
            throw new ProtocolException("MQTT version "+version+" not supported");
        }
        byte flags = is.readByte();

        boolean username_flag = (flags & 0x80) > 0;
        boolean password_flag = (flags & 0x40) > 0;
        willRetain = (flags & 0x20) > 0;
        willQos = (byte) ((flags & 0x18) >>> 3);
        boolean will_flag = (flags & 0x04) > 0;
        cleanSession = (flags & 0x02) > 0;

        keepAlive = is.readShort();
        clientId = CommandSupport.readUTF(is);
        if(will_flag) {
            willTopic = CommandSupport.readUTF(is);
            willMessage = CommandSupport.readUTF(is);
        }
        if( username_flag ) {
            userName = CommandSupport.readUTF(is);
        }
        if( password_flag ) {
            password = CommandSupport.readUTF(is);
        }
        return this;
    }
    
    public MQTTFrame encode() {
        try {
            DataByteArrayOutputStream os = new DataByteArrayOutputStream(500);
            CommandSupport.writeUTF(os, PROTOCOL_NAME);
            os.writeByte(PROTOCOL_VERSION);
            int flags = 0;
            if(userName!=null) {
                flags |= 0x80;
            }
            if(password!=null) {
                flags |= 0x40;
            }
            if(willTopic!=null && willMessage!=null) {
                flags |= 0x04;
                if(willRetain) {
                    flags |= 0x20;
                }
                flags |= (willQos << 3) & 0x18;
            }
            if(cleanSession) {
                flags |= 0x02;
            }
            os.writeByte(flags);
            os.writeShort(keepAlive);
            CommandSupport.writeUTF(os, clientId);
            if(willTopic!=null && willMessage!=null) {
                CommandSupport.writeUTF(os, willTopic);
                CommandSupport.writeUTF(os, willMessage);
            }
            if(userName!=null) {
                CommandSupport.writeUTF(os, userName);
            }
            if(password!=null) {
                CommandSupport.writeUTF(os, password);
            }

            MQTTFrame frame = new MQTTFrame();
            frame.commandType(TYPE);
            return frame.buffer(os.toBuffer());
        } catch (IOException e) {
            throw new RuntimeException("The impossible happened");
        }
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public UTF8Buffer getClientId() {
        return clientId;
    }

    public void setClientId(UTF8Buffer clientId) {
        this.clientId = clientId;
    }

    public short getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(short keepAlive) {
        this.keepAlive = keepAlive;
    }

    public UTF8Buffer getPassword() {
        return password;
    }

    public void setPassword(UTF8Buffer password) {
        this.password = password;
    }

    public UTF8Buffer getUserName() {
        return userName;
    }

    public void setUserName(UTF8Buffer userName) {
        this.userName = userName;
    }

    public UTF8Buffer getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(UTF8Buffer willMessage) {
        this.willMessage = willMessage;
    }

    public byte getWillQos() {
        return willQos;
    }

    public void setWillQos(byte willQos) {
        this.willQos = willQos;
    }

    public boolean isWillRetain() {
        return willRetain;
    }

    public void setWillRetain(boolean willRetain) {
        this.willRetain = willRetain;
    }

    public UTF8Buffer getWillTopic() {
        return willTopic;
    }

    public void setWillTopic(UTF8Buffer willTopic) {
        this.willTopic = willTopic;
    }

    @Override
    public String toString() {
        return "CONNECT{" +
                "cleanSession=" + cleanSession +
                ", keepAlive=" + keepAlive +
                ", clientId=" + clientId +
                ", willTopic=" + willTopic +
                ", willMessage=" + willMessage +
                ", willRetain=" + willRetain +
                ", willQos=" + willQos +
                ", userName=" + userName +
                ", password=" + password +
                '}';
    }
}
