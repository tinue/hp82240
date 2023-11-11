package ch.erzberger.emulation.communication;

/**
 * A PrintDataDevice is the parent for senders or receivers. It covers common functionality.
 */
public interface PrintDataDevice {
    /**
     * Retrieves the name of the IO device
     *
     * @return Name of the IO device
     */
    String getDeviceName();

    /**
     * Blocking wait until the device is ready.
     *
     * @param timeout Maximum wait time in milliseconds
     * @return true if the device is ready; false might be returned if the timeout occurs before the device became ready
     */
    boolean waitUntilReady(long timeout);
}
