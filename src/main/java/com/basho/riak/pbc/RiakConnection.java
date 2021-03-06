/**
 * This file is part of riak-java-pb-client
 *
 * Copyright (c) 2010 by Trifork
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
 **/
package com.basho.riak.pbc;

import com.basho.riak.protobuf.RiakPB.RpbErrorResp;
import com.google.protobuf.MessageLite;

import java.io.*;
import java.net.*;

/**
 * Wraps the {@link Socket} used to send/receive data to Riak's protocol buffers interface.
 *
 * See <a href="http://wiki.basho.com/PBC-API.html">Basho Wiki</a> for more details.
 */
class RiakConnection implements Comparable<RiakConnection>
{

	static final int DEFAULT_RIAK_PB_PORT = 8087;
	private Socket sock;
	private DataOutputStream dout;
	private DataInputStream din;
	private final RiakConnectionPool pool;
	// Guarded by the intrinsic lock 'this'
	private byte[] clientId;
	private volatile long idleStart;

	public RiakConnection(InetAddress addr, int port, int bufferSizeKb, final RiakConnectionPool pool, final long connectTimeoutMillis, final int requestTimeoutMillis) throws IOException {
		this(new InetSocketAddress(addr, port), bufferSizeKb, pool, connectTimeoutMillis, requestTimeoutMillis);
	}

	public RiakConnection(SocketAddress addr, int bufferSizeKb, final RiakConnectionPool pool, final long connectTimeoutMillis, final int requestTimeoutMillis) throws IOException {
        
		if (connectTimeoutMillis > Integer.MAX_VALUE || connectTimeoutMillis < Integer.MIN_VALUE) {
			throw new IllegalArgumentException("Cannot cast timeout to int without changing value");
		}

		this.pool = pool;
		sock = new Socket();
        
        // With the original Java IO the SO_TIMEOUT value is used for read/write operations
        if (requestTimeoutMillis > 0) {
            sock.setSoTimeout(requestTimeoutMillis);
        }
		
        sock.connect(addr, (int) connectTimeoutMillis);

		sock.setSendBufferSize(1024 * bufferSizeKb);

		dout = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream(), 1024 * bufferSizeKb));
		din = new DataInputStream(new BufferedInputStream(sock.getInputStream(), 1024 * bufferSizeKb));
	}

	///////////////////////
	void send(int code, MessageLite req) throws IOException {
		try {
			int len = req.getSerializedSize();
			dout.writeInt(len + 1);
			dout.write(code);
			req.writeTo(dout);
			dout.flush();
		} catch (IOException e) {
			// Explicitly close our Socket on an IOException then rethrow
			close();
			throw e;
		}
	}

	void send(int code) throws IOException {
		try {
			dout.writeInt(1);
			dout.write(code);
			dout.flush();
		} catch (IOException e) {
			// Explicitly close our Socket on an IOException then rethrow
			close();
			throw e;
		}

	}

	byte[] receive(int code) throws IOException {
		int len;
		int get_code;
		byte[] data = null;

		try {
			len = din.readInt();
			get_code = din.read();

			if (len > 1) {
				data = new byte[len - 1];
				din.readFully(data);
			}
		} catch (IOException e) {
			// Explicitly close our Socket on an IOException then rethrow
			close();
			throw e;
		}

		if (get_code == RiakClient.MSG_ErrorResp) {
			RpbErrorResp err = com.basho.riak.protobuf.RiakPB.RpbErrorResp.parseFrom(data);
			throw new RiakError(err);
		}

		if (code != get_code) {
			throw new IOException("bad message code. Expected: " + code + " actual: " + get_code);
		}

		return data;
	}

	void receive_code(int code) throws IOException, RiakError {
		int len;
		int get_code;

		try {
			len = din.readInt();
			get_code = din.read();
			if (get_code == RiakClient.MSG_ErrorResp) {
				RpbErrorResp err = com.basho.riak.protobuf.RiakPB.RpbErrorResp.parseFrom(din);
				throw new RiakError(err);
			}
		} catch (IOException e) {
			// Explicitly close our Socket on an IOException then rethrow
			close();
			throw e;
		}

		if (len != 1 || code != get_code) {
			throw new IOException("bad message code. Expected: " + code + " actual: " + get_code);
		}
	}

	void close() {
		if (isClosed()) {
			return;
		}

		try {
			sock.close();
			din = null;
			dout = null;
			sock = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	boolean checkValid() {
		return isClosed();
	}

	public DataOutputStream getOutputStream() {
		return dout;
	}

	public boolean isClosed() {
		return sock == null || sock.isClosed();
	}

	public synchronized void beginIdle()  {
		this.idleStart = System.nanoTime();
	}

	public long getIdleStartTimeNanos() {
		return this.idleStart;
	}

	/**
	 *
	 */
	public void release() {
		pool.releaseConnection(this);
	}

	/**
	 * @return the clientId
	 */
	public synchronized byte[] getClientId() {
		return clientId;
	}

	/**
	 * @param clientId the clientId to set
	 */
	public synchronized void setClientId(byte[] clientId) {
		this.clientId = clientId;
	}

	/**
	 * @return true if a clientId has been *explicitly set* (IE not default from
	 *         Riak server) on this connection
	 */
	public synchronized boolean hasClientId()
	{
		return clientId != null && clientId.length > 0;
	}

    /** 
     * The natural ordering is descending by idle start time
     */ 
    public int compareTo(RiakConnection c)
    {
        if (c.getIdleStartTimeNanos() < this.getIdleStartTimeNanos()) {
            return -1;
        } else if (c.getIdleStartTimeNanos() > this.getIdleStartTimeNanos()) {
            return 1;
        } else {
            return 0;
        }
    }
}
