package ch.erzberger.emulation.communication;

import ch.erzberger.emulation.common.ByteProcessor;

/**
 * A PrintDataReader reads data from a stream device (serial port, File stream, stdin etc.).
 * Data received is forwarded to a registered callback.
 */
public interface PrintDataReader extends PrintDataDevice {
    /**
     * Register the callback that gets the received data
     * @param byteProcessor The callback that receives the data
     */
    void registerCallback(ByteProcessor byteProcessor);
}
