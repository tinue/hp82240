package ch.erzberger.emulation.communication;

import ch.erzberger.emulation.common.ByteProcessor;
import ch.erzberger.emulation.common.FileParserHelper;
import lombok.extern.java.Log;

import java.nio.file.Path;

/**
 * The StdInReaderImpl reads from stdin. Write requests are ignored.
 */
@Log
public class FileReaderImpl implements PrintDataReader {
    final Path file;
    ByteProcessor byteProcessor; // Callback that gets the received bytes sent to one by one

    public FileReaderImpl(Path file) {
        if (file == null) {
            throw new NoClassDefFoundError("File cannot be null");
        }
        this.file = file;
    }

    @Override
    public void registerCallback(ByteProcessor byteProcessor) {
        this.byteProcessor = byteProcessor;
        FileParserHelper helper = new FileParserHelper(file);
        byte[] buffer = helper.readEntireFile();
        byteProcessor.processBytes(buffer);
    }

    @Override
    public String getDeviceName() {
        return String.format("File: %s", file);
    }

    @Override
    public boolean waitUntilReady(long timeout) {
        return true; // A file is always ready
    }
}
