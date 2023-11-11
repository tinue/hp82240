package ch.erzberger.emulation.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class FileParserHelperTest {

    @Test
    void parseFileAsByteArray() {
        FileParserHelper helper = new FileParserHelper(Paths.get("src/test/resources/test.yaml"));
        // Special characters; Check for proper conversion and foe line feed at the end
        readNextAndCompare(helper, "41b6830a");
        // Graphic bytes 010204080F. Will get an additional graphic and length indicator of 5
        readNextAndCompare(helper, "1b05010204080f04");
        // 8 graphic sequences.
        readNextAndCompare(helper, "1bff1bfe1bfd1bfc1bfb1bfa1bf91bf804");
        // invalid graphic sequence.
        readNextAndCompare(helper, "1b0004");
        // raw hex values
        readNextAndCompare(helper, "000102030a");
        assertFalse(helper.hasNextLine());
    }

    private void readNextAndCompare(FileParserHelper helper, String expected) {
        assertTrue(helper.hasNextLine());
        HexFormat hex = HexFormat.of();
        String hexString = hex.formatHex(helper.readNextLine());
        assertEquals(expected, hexString);
    }
}