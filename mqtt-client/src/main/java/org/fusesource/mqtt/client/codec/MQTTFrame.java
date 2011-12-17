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

package org.fusesource.mqtt.client.codec;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.mqtt.client.QoS;

/**
* <p>
* </p>
*
* @author <a href="http://hiramchirino.com">Hiram Chirino</a>
*/
public class MQTTFrame extends CommandSupport.HeaderBase {

    public Buffer[] buffers;

    public MQTTFrame() {
    }
    public MQTTFrame( Buffer buffer) {
        this(new Buffer[]{buffer});
    }
    public MQTTFrame( Buffer[] buffers) {
        this.buffers = buffers;
    }

    public Buffer[] buffers() {
        return buffers;
    }
    public MQTTFrame buffers(Buffer...buffers) {
        this.buffers = buffers;
        return this;
    }

    public MQTTFrame buffer(Buffer buffer) {
        this.buffers = new Buffer[]{buffer};
        return this;
    }

    @Override
    public byte header() {
        return super.header();
    }

    @Override
    public MQTTFrame header(byte header) {
        return (MQTTFrame)super.header(header);
    }

    @Override
    public byte commandType() {
        return super.commandType();
    }

    @Override
    public MQTTFrame commandType(int type) {
        return (MQTTFrame)super.commandType(type);
    }

    @Override
    public boolean dup() {
        return super.dup();
    }

    @Override
    public MQTTFrame dup(boolean dup) {
        return (MQTTFrame) super.dup(dup);
    }

    @Override
    public QoS qos() {
        return super.qos();
    }

    @Override
    public MQTTFrame qos(QoS qos) {
        return (MQTTFrame) super.qos(qos);
    }

    @Override
    public boolean retain() {
        return super.retain();
    }

    @Override
    public MQTTFrame retain(boolean retain) {
        return (MQTTFrame) super.retain(retain);
    }
}
