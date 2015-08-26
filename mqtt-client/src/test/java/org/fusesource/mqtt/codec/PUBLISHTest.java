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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.mqtt.client.QoS;

/**
 * A test for PUBLISH.
 */
public class PUBLISHTest extends TestCase {

    public void testEncodeDecode() throws Exception {
        QoS qos = QoS.EXACTLY_ONCE;
        boolean retain = false;
        String topic = "testTopic";
        String payload = "foobar";
        PUBLISH input = new PUBLISH()
                        .qos(qos)
                        .retain(retain)
                        .topicName(Buffer.utf8(topic))
                        .payload(new Buffer(payload.getBytes()));
        PUBLISH output = new PUBLISH().decode(input.encode());
        Assert.assertEquals(qos, output.qos());
        Assert.assertEquals(retain, output.retain());
        Assert.assertEquals(topic, output.topicName().toString());
        Assert.assertEquals(payload, new String(output.payload().toByteArray()));
    }
}
