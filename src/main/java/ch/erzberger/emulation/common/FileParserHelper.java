package ch.erzberger.emulation.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.java.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.logging.Level;

/**
 * Helper class for file reading / parsing
 */
@Log
public class FileParserHelper {
    private final List<Hp82240PrintData> lines;

    private int currentLine = 0;

    public FileParserHelper(Path file) {
        var mapper = new ObjectMapper(new YAMLFactory());
        Hp84440PrinterFile printerFile;
        try {
            printerFile = mapper.readValue(file.toFile(), Hp84440PrinterFile.class);
            lines = printerFile.getHp82240PrintData();
        } catch (IOException e) {
            throw new NoClassDefFoundError(String.format("Cannot initialize the yaml file parser. Error: %s", e.getMessage()));
        }
        log.log(Level.INFO, "File title: {0}", printerFile.getTitle());
        log.log(Level.INFO, "File purpose: {0}", printerFile.getPurpose());
    }

    public byte[] readEntireFile() {
        List<Byte> buffer = new ArrayList<>(1000);
        byte[] lineBuffer;
        while (hasNextLine()) {
            lineBuffer = readNextLine();
            for (byte currentByte : lineBuffer) {
                buffer.add(currentByte);
            }
        }
        byte[] retVal = new byte[buffer.size()];
        for (int i = 0; i < retVal.length; i++) {
            retVal[i] = buffer.get(i);
        }
        return retVal;
    }

    /**
     * Reads one pseudo-line of printer data. It returns the next chunk of data, until a carriage return is encountered.
     * The very last byte will be the CR (0x04 or 0x1A). It will return an empty array if the entire file has been read.
     *
     * @return One pseudo-line of binary printer data. Last byte will be the carriage return.
     */
    public byte[] readNextLine() {
        byte[] lineBuffer = new byte[0];
        while (hasNextLine()) {
            byte[] yamlLineBuffer = readNextYamlLine();
            byte[] lineBufferCombined = new byte[lineBuffer.length + yamlLineBuffer.length];
            System.arraycopy(lineBuffer, 0, lineBufferCombined, 0, lineBuffer.length);
            System.arraycopy(yamlLineBuffer, 0, lineBufferCombined, lineBuffer.length, yamlLineBuffer.length);
            lineBuffer = lineBufferCombined;
            if (endsWithLineFeed(lineBuffer)) {
                return  lineBuffer;
            }
        }
        // The yaml file ended without a line feed. Log the face, but still return the buffer
        log.log(Level.WARNING, "YAML file ends without a line feed. Printer output will be incomplete");
        return lineBuffer;
    }

    private boolean endsWithLineFeed(byte[] buffer) {
        if (buffer == null || buffer.length == 0) {
            return false;
        }
        byte lastByte = buffer[buffer.length-1];
        return (lastByte == 0x04 || lastByte == 0x0A);
    }

    private byte[] readNextYamlLine() {
        if (currentLine >= lines.size()) {
            // At the end, no more data
            return new byte[0];
        }
        Hp82240PrintData line = lines.get(currentLine);
        byte[] lineBuffer;
        if (line.getText() != null) {
            // Simple text. Convert the UTF-8 string to bytes using the HP charset
            lineBuffer = line.getText().getBytes(Hp82240aCharsetProvider.hp82240a());
        } else if (line.getGraphic() != null) {
            // Graphic bytes; Each byte is a vertical row of 8 pixels. Add the length indicator.
            byte[] graphicsSequence = HexFormat.of().parseHex(line.getGraphic());
            // Add the graphic character and the length indicator
            lineBuffer = new byte[graphicsSequence.length + 2];
            lineBuffer[0] = 0x1B;  // The graphic character
            lineBuffer[1] = (byte)graphicsSequence.length;
            System.arraycopy(graphicsSequence, 0, lineBuffer, 2, graphicsSequence.length);
        } else if (line.getLinefeed() != null) {
            // Line feed. Send HP or regular line feed byte
            lineBuffer = new byte[1];
            if ("hp".equals(line.getLinefeed())) {
                lineBuffer[0] = 0x04;
            } else {
                lineBuffer[0] = 0x0A;
            }
        } else if (line.getEsc() != null) {
            // graphic sequence. Convert the indicator into the proper graphic sequence
            lineBuffer = new byte[2];
            lineBuffer[0] = 0x1B;
            Hp82240EscapeCodes code = Hp82240EscapeCodes.getEscapeCodeByTextVersion(line.getEsc());
            if (code == Hp82240EscapeCodes.GRAPHICS_MODE) {
                log.log(Level.SEVERE, "Unknown graphic sequence in file: {0}", line.getEsc());
            }
            lineBuffer[1] = code.getEscCode();
        } else if (line.getHex() != null) {
            // Hex values are simply converted from String to byte. No alteration is done.
            lineBuffer = HexFormat.of().parseHex(line.getHex());
        } else {
            lineBuffer = new byte[0];
        }
        currentLine++;
        return lineBuffer;
    }

    public boolean hasNextLine() {
        return lines.size() > currentLine;
    }
}
