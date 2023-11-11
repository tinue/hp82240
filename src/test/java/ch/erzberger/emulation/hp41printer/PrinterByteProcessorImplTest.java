package ch.erzberger.emulation.hp41printer;

import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class PrinterByteProcessorImplTest {
    private PrinterByteProcessorImpl printerByteProcessorImplState;
    private TestPaperImpl paper;

    @BeforeEach
    void setUp() {
        paper = new TestPaperImpl();
        printerByteProcessorImplState = new PrinterByteProcessorImpl(paper, true);
    }

    @Test
    void doubleWide() {
        printerByteProcessorImplState.processByte((byte) 27); // ESC
        printerByteProcessorImplState.processByte((byte) 253); // Double Wide
        assertTrue(printerByteProcessorImplState.isDoubleWide());
        printerByteProcessorImplState.processBytes(new byte[]{(byte) 27, (byte) 252});
        assertFalse(printerByteProcessorImplState.isDoubleWide());
        // Test that ESC has been reset
        printerByteProcessorImplState.processByte((byte) 253); // Double Wide, but no preceding ESC
    }

    @Test
    void underline() {
        printerByteProcessorImplState.processByte((byte) 27); // ESC
        printerByteProcessorImplState.processByte((byte) 251); // Underline
        assertTrue(printerByteProcessorImplState.isUnderline());
        printerByteProcessorImplState.processByte((byte) 27); // ESC
        printerByteProcessorImplState.processByte((byte) 250); // Stop underline
        assertFalse(printerByteProcessorImplState.isUnderline());
    }

    @Test
    void iso8859() {
        printerByteProcessorImplState.processByte((byte) 27); // ESC
        printerByteProcessorImplState.processByte((byte) 249); // ISO 8859
        assertTrue(printerByteProcessorImplState.isIso8859());
        printerByteProcessorImplState.processByte((byte) 27); // ESC
        printerByteProcessorImplState.processByte((byte) 248); // Stop ISO8859
        assertFalse(printerByteProcessorImplState.isIso8859());
    }

    @Test
    void graphics() {
        String hexBytes = "1B83000303000000020404040300020404040300010000040402010000040402010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000705050509000804020F00000804020F000007050505090";
        byte[] graphicLine = HexFormat.of().parseHex(hexBytes);
        for (byte oneByte : graphicLine) {
            printerByteProcessorImplState.processByte(oneByte);
        }
        printerByteProcessorImplState.processByte((byte) 0x04);
        assertFalse(printerByteProcessorImplState.isIso8859());
    }

    @Test
    void mixed() {
        // Mixed text / graphics on one line. Supposed to be "    X <UNITS= 1.> " plus a down arrow
        String hexBytes = "2020202058203C554E4954533D20312E3E201B070010207E20100004";
        byte[] mixedLine = HexFormat.of().parseHex(hexBytes);
        for (byte oneByte : mixedLine) {
            printerByteProcessorImplState.processByte(oneByte);
        }
        assertNotNull(paper.getLine());
        boolean[][] bitmap = paper.getBitmap();
        assertNotNull(bitmap);
        assertEquals(166, bitmap.length);
    }

    @Test
    void reset() {
        // Set flags
        printerByteProcessorImplState.processByte((byte) 27); // ESC
        printerByteProcessorImplState.processByte((byte) 253); // Double Wide
        printerByteProcessorImplState.processByte((byte) 27); // ESC
        printerByteProcessorImplState.processByte((byte) 251); // Underline
        printerByteProcessorImplState.processByte((byte) 27); // ESC
        printerByteProcessorImplState.processByte((byte) 249); // ISO8859
        assertTrue(printerByteProcessorImplState.isDoubleWide());
        assertTrue(printerByteProcessorImplState.isUnderline());
        assertTrue(printerByteProcessorImplState.isIso8859());
        // Reset
        printerByteProcessorImplState.processByte((byte) 27); // ESC
        printerByteProcessorImplState.processByte((byte) 255); // Reset
        assertFalse(printerByteProcessorImplState.isDoubleWide());
        assertFalse(printerByteProcessorImplState.isUnderline());
        assertFalse(printerByteProcessorImplState.isIso8859());
    }

    @Test
    void selftest() {
        printerByteProcessorImplState.processByte((byte) 27); // ESC
        printerByteProcessorImplState.processByte((byte) 254); // Selftest
        assertFalse(printerByteProcessorImplState.isDoubleWide());
        assertFalse(printerByteProcessorImplState.isUnderline());
        assertFalse(printerByteProcessorImplState.isIso8859());
    }

    @Getter
    private static class TestPaperImpl implements Paper {
        private String line;
        private boolean[][] bitmap;

        @Override
        public void printLine(String line) {
            this.line = line;
        }

        @Override
        public void printGraphic(boolean[][] bitmap) {
            this.bitmap = bitmap;
        }
    }
}