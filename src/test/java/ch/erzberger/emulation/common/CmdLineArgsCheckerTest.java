package ch.erzberger.emulation.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static ch.erzberger.emulation.common.CmdLineArgsChecker.MODELA;
import static ch.erzberger.emulation.common.CmdLineArgsChecker.PORTARG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CmdLineArgsCheckerTest {
    CmdLineArgsChecker checker;

    @BeforeEach
    void setUp() {
        checker = new CmdLineArgsChecker();
    }

    @Test
    void checkPortPositive() {
        assertEquals("com1:", checker.checkReceiverArgs(new String[]{"-p=com1:"}).get(PORTARG));
        assertEquals("com1:", checker.checkReceiverArgs(new String[]{"-p com1:"}).get(PORTARG));
        assertEquals("COM:", checker.checkReceiverArgs(new String[]{"-p=COM:"}).get(PORTARG));
        assertEquals("ttyACM0", checker.checkReceiverArgs(new String[]{"-p=ttyACM0"}).get(PORTARG));
        assertEquals("usbmodem4101", checker.checkReceiverArgs(new String[]{"-p=usbmodem4101"}).get(PORTARG));
        assertEquals("", checker.checkReceiverArgs(new String[]{}).get(PORTARG));
    }

    @Test
    void checkPortNegative() {
        assertNull(checker.checkReceiverArgs(new String[]{"-p=/dev/tty.usbmodem4101"}).get(PORTARG));
        assertNull(checker.checkReceiverArgs(new String[]{"-p=tty.usbmodem4101"}).get(PORTARG));
        assertNull(checker.checkReceiverArgs(new String[]{"-p=/dev/ttyACM0"}).get(PORTARG));
    }

    @Test void checkModelA() {
        assertEquals(MODELA, checker.checkReceiverArgs(new String[]{"-a"}).get(MODELA));
        assertNull(checker.checkReceiverArgs(new String[]{"-p=/dev/ttyACM0"}).get(MODELA));
    }
}