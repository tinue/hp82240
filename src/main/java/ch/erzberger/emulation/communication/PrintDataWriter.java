package ch.erzberger.emulation.communication;

/**
 * A PrintDataWriter sends data to a stream device (serial port, File stream, stdout etc.).
 * If necessary it implements flow control in order to not overload the physical device's buffer.
 */
public interface PrintDataWriter extends PrintDataDevice {
    /**
     * Sends a block of data
     * @param sendBuffer the data to be sent. Note: The entire buffer is sent, so it must be sized properly.
     */
    void sendBytes(byte[] sendBuffer);

    /**
     * Flushes the IO device to guarantee that all data is sent
     */
    void flush();
}
