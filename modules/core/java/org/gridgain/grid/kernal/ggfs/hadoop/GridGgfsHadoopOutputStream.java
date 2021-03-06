/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.ggfs.hadoop;

import org.apache.commons.logging.*;
import org.gridgain.grid.*;

import java.io.*;

/**
 * GGFS Hadoop output stream implementation.
 */
public class GridGgfsHadoopOutputStream extends OutputStream implements GridGgfsStreamEventListener {
    /** Log instance. */
    private Log log;

    /** Client logger. */
    private GridGgfsHadoopLogger clientLog;

    /** Log stream ID. */
    private long logStreamId;

    /** Grid remote client. */
    private GridGgfsHadoop rmtClient;

    /** Server stream ID. */
    private long streamId;

    /** Closed flag. */
    private volatile boolean closed;

    /** Flag set if stream was closed due to connection breakage. */
    private boolean connBroken;

    /** Error message. */
    private volatile String errMsg;

    /** Read time. */
    private long writeTime;

    /** User time. */
    private long userTime;

    /** Last timestamp. */
    private long lastTs;

    /** Amount of written bytes. */
    private long total;

    /**
     * Creates light output stream.
     *
     * @param rmtClient Remote client to use.
     * @param streamId Stream ID.
     * @param log Logger to use.
     * @param clientLog Client logger.
     */
    public GridGgfsHadoopOutputStream(GridGgfsHadoop rmtClient, long streamId, Log log,
        GridGgfsHadoopLogger clientLog, long logStreamId) {
        this.rmtClient = rmtClient;
        this.streamId = streamId;
        this.log = log;
        this.clientLog = clientLog;
        this.logStreamId = logStreamId;

        lastTs = System.nanoTime();

        rmtClient.addEventListener(streamId, this);
    }

    /**
     * Read start.
     */
    private void writeStart() {
        long now = System.nanoTime();

        userTime += now - lastTs;

        lastTs = now;
    }

    /**
     * Read end.
     */
    private void writeEnd() {
        long now = System.nanoTime();

        writeTime += now - lastTs;

        lastTs = now;
    }

    /** {@inheritDoc} */
    @Override public void write(byte[] b, int off, int len) throws IOException {
        check();

        writeStart();

        try {
            rmtClient.writeData(streamId, b, off, len);

            total += len;
        }
        catch (GridException e) {
            throw new IOException(e);
        }
        finally {
            writeEnd();
        }
    }

    /** {@inheritDoc} */
    @Override public void write(int b) throws IOException {
        write(new byte[] {(byte)b});

        total++;
    }

    /** {@inheritDoc} */
    @Override public void close() throws IOException {
        try {
            if (!closed) {
                if (log.isDebugEnabled())
                    log.debug("Closing output stream: " + streamId);

                writeStart();

                rmtClient.closeStream(streamId).get();

                markClosed(false);

                writeEnd();

                if (clientLog.isLogEnabled())
                    clientLog.logCloseOut(logStreamId, userTime, writeTime, total);

                if (log.isDebugEnabled())
                    log.debug("Closed output stream [streamId=" + streamId + ", writeTime=" + writeTime / 1000 +
                        ", userTime=" + userTime / 1000 + ']');
            }
            else if(connBroken)
                throw new IOException(
                    "Failed to close stream, because connection was broken (data could have been lost).");
        }
        catch (GridException e) {
            throw new IOException(e);
        }
    }

    /**
     * Marks stream as closed.
     *
     * @param connBroken {@code True} if connection with server was lost.
     */
    private void markClosed(boolean connBroken) {
        // It is ok to have race here.
        if (!closed) {
            closed = true;

            rmtClient.removeEventListener(streamId);

            this.connBroken = connBroken;
        }
    }

    /**
     * @throws IOException If check failed.
     */
    private void check() throws IOException {
        String errMsg0 = errMsg;

        if (errMsg0 != null)
            throw new IOException(errMsg0);

        if (closed) {
            if (connBroken)
                throw new IOException("Server connection was lost.");
            else
                throw new IOException("Stream is closed.");
        }
    }

    /** {@inheritDoc} */
    @Override public void onClose() throws GridException {
        markClosed(true);
    }

    /** {@inheritDoc} */
    @Override public void onError(String errMsg) throws GridException {
        this.errMsg = errMsg;
    }
}
