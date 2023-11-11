package ch.erzberger.emulation.common;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Hp82240aCharsetProviderTest {
    Charset hp82240a = Hp82240aCharsetProvider.hp82240a();

    @Test
    void encode() {
        assertEquals((byte) 32, hp82240a.encode(" ").get());
        assertEquals((byte) 254, hp82240a.encode("±").get());
    }

    @Test
    void decode() {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[]{(byte) 254});
        assertEquals('±', hp82240a.decode(byteBuffer).get());
        byteBuffer = ByteBuffer.wrap(new byte[]{(byte) 255});
        assertEquals('�', hp82240a.decode(byteBuffer).get());
    }
}