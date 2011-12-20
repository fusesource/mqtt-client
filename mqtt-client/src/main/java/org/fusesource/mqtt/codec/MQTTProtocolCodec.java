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

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import org.fusesource.hawtdispatch.transport.ProtocolCodec;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class MQTTProtocolCodec implements ProtocolCodec {
    public static final int max_data_length = 1024 * 1024 * 100;

    /////////////////////////////////////////////////////////////////////
    //
    // Non blocking write imp
    //
    /////////////////////////////////////////////////////////////////////
    int write_buffer_size = 1024 * 64;
    long write_counter = 0L;
    WritableByteChannel write_channel = null;

    DataByteArrayOutputStream next_write_buffer = new DataByteArrayOutputStream(write_buffer_size);

    ByteBuffer write_buffer = ByteBuffer.allocate(0);
    int last_write_io_size = 0;

    public void setWritableByteChannel(WritableByteChannel channel) throws SocketException {
        this.write_channel = channel;
        if (this.write_channel instanceof SocketChannel) {
            write_buffer_size = ((SocketChannel) this.write_channel).socket().getSendBufferSize();
        }
    }

    public int getReadBufferSize() {
        return read_buffer_size;
    }

    public int getWriteBufferSize() {
        return write_buffer_size;
    }

    public boolean full() {
        return next_write_buffer.size() >= (write_buffer_size >> 1);
    }

    public boolean is_empty() {
        return write_buffer.remaining() == 0;
    }

    public long getWriteCounter() {
        return write_counter;
    }

    public int getLastWriteSize() {
        return last_write_io_size;
    }

    public BufferState write(Object value) throws IOException {
        if (full()) {
            return BufferState.FULL;
        } else {
            boolean was_empty = is_empty();
            MQTTFrame frame = (MQTTFrame) value;
            next_write_buffer.write(frame.header());

            int remaining = 0;
            for(Buffer buffer : frame.buffers) {
                remaining += buffer.length;
            }

            do {
                byte digit = (byte) (remaining & 0x7F);
                remaining >>>= 7;
                if (remaining > 0) {
                    digit |= 0x80;
                }
                next_write_buffer.write(digit);
            } while (remaining > 0);

            for(Buffer buffer : frame.buffers) {
                next_write_buffer.write(buffer.data, buffer.offset, buffer.length);
            }

            if (was_empty) {
                return BufferState.WAS_EMPTY;
            } else {
                return BufferState.NOT_EMPTY;
            }
        }
    }


    public BufferState flush() throws IOException {
        while( true ) {
            // if we have a pending write that is being sent over the socket...
            if (write_buffer.remaining() != 0) {
                last_write_io_size = write_channel.write(write_buffer);
                if( last_write_io_size == 0 ) {
                    return BufferState.NOT_EMPTY;
                } else {
                    write_counter += last_write_io_size;
                }
            } else {
                if( next_write_buffer.size() == 0  ) {
                    return BufferState.EMPTY;
                } else {
                    // size of next buffer is based on how much was used in the previous buffer.
                    int prev_size = Math.min(Math.max((write_buffer.position() + 512), 512), write_buffer_size);
                    write_buffer = next_write_buffer.toBuffer().toByteBuffer();
                    next_write_buffer = new DataByteArrayOutputStream(prev_size);
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////////////
    //
    // Non blocking read impl
    //
    /////////////////////////////////////////////////////////////////////

    interface FrameReader {
        MQTTFrame apply(ByteBuffer buffer) throws IOException;
    }

    long read_counter = 0L;
    int read_buffer_size = 1024 * 64;
    ReadableByteChannel read_channel = null;

    ByteBuffer read_buffer = ByteBuffer.allocate(read_buffer_size);
    int read_end = 0;
    int read_start = 0;
    int last_read_io_size = 0;

    private final FrameReader read_header = new FrameReader() {
        public MQTTFrame apply(ByteBuffer buffer) throws IOException {
            int length = read_length(buffer, read_start+1);
            if( length >= 0 ) {
                if( length > max_data_length ) {
                    throw new IOException("The maximum message length was exceeded");
                }
                byte header = buffer.get(read_start);
                read_start = read_end;
                if( length > 0 ) {
                    next_action = read_body(header, length);
                } else {
                    return new MQTTFrame().header(header);
                }
            }
            return null;
        }
    };

    FrameReader next_action = read_header;
    boolean trim = true;

    public void setReadableByteChannel(ReadableByteChannel channel) throws SocketException {
        this.read_channel = channel;
        if (this.read_channel instanceof SocketChannel) {
            read_buffer_size = ((SocketChannel) this.read_channel).socket().getReceiveBufferSize();
        }
    }

    public void unread(byte[] buffer) {
        assert (read_counter == 0);
        read_buffer.put(buffer);
        read_counter += buffer.length;
    }

    public long getReadCounter() {
        return read_counter;
    }

    public int getLastReadSize() {
        return last_read_io_size;
    }

    public Object read() throws IOException {
        Object command = null;
        while (command == null) {
            // do we need to read in more data???
            if (read_end == read_buffer.position()) {

                // do we need a new data buffer to read data into??
                if (read_buffer.remaining() == 0) {

                    // How much data is still not consumed by the wireformat
                    int size = read_end - read_start;

                    int new_capacity;
                    if (read_start == 0) {
                        new_capacity = size + read_buffer_size;
                    } else {
                        if (size > read_buffer_size) {
                            new_capacity = size + read_buffer_size;
                        } else {
                            new_capacity = read_buffer_size;
                        }
                    }

                    byte[] new_buffer = new byte[new_capacity];
                    if (size > 0) {
                        System.arraycopy(read_buffer.array(), read_start, new_buffer, 0, size);
                    }

                    read_buffer = ByteBuffer.wrap(new_buffer);
                    read_buffer.position(size);
                    read_start = 0;
                    read_end = size;
                }

                // Try to fill the buffer with data from the socket..
                int p = read_buffer.position();
                last_read_io_size = read_channel.read(read_buffer);
                if (last_read_io_size == -1) {
                    throw new EOFException("Peer disconnected");
                } else if (last_read_io_size == 0) {
                    return null;
                }
                read_counter += last_read_io_size;
            }

            command = next_action.apply(read_buffer);

            // Sanity checks to make sure the codec is behaving as expected.
            assert (read_start <= read_end);
            assert (read_end <= read_buffer.position());
        }
        return command;
    }


    int read_length(ByteBuffer buffer, int pos) throws IOException {
        int limit = buffer.position();
        int length = 0;
        int multiplier = 1;
        byte digit;
        while (pos < limit) {
            digit = buffer.get(pos);
            length += (digit & 0x7F) * multiplier;
            if( (digit & 0x80) == 0 ) {
                read_end = pos+1;
                return length;
            }
            multiplier <<= 7;
            pos++;
        }
        return -1;
    }

    FrameReader read_body(final byte header, final int length) {
        return new FrameReader() {
            public MQTTFrame apply(ByteBuffer buffer) throws IOException {

                int read_limit = buffer.position();
                if ((read_limit - read_start) < length) {
                    read_end = read_limit;
                    return null;
                } else {
                    Buffer body = new Buffer(buffer.array(), read_start, length);
                    read_end = read_start + length;
                    read_start = read_end;
                    next_action = read_header;
                    return new MQTTFrame(body).header(header);
                }
            }
        };
    }



}
