package ch.erzberger.emulation.common;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.spi.CharsetProvider;
import java.text.Normalizer;
import java.util.*;

/**
 * Mapping from the HP 82240B RPL printer character set to Unicode.
 * To find the correct mapping, I consulted this Wikipedia Article:
 * <a href="https://en.wikipedia.org/wiki/RPL_character_set">RPL character set</a>
 * The template for this CharsetProvider comes from a Stack Overflow answer:
 * <a href="https://stackoverflow.com/questions/76936549/charset-not-present-in-used-jdk/76936879#76936879">Charset not present in used JDK</a>
 */
public class RplCharsetProvider extends CharsetProvider {
    private static final Charset RPL = new RplCharset();
    private static final List<Charset> RPL_LIST = Collections.singletonList(RPL);

    /**
     * Use this helper to get access to the charset provider. Alternatively, register the provider formally.
     *
     * @return The provider for the RPL charset
     */
    public static Charset Rpl() {
        return RPL;
    }

    @Override
    public Iterator<Charset> charsets() {
        return RPL_LIST.iterator();
    }

    @Override
    public Charset charsetForName(String charsetName) {
        if (charsetName.equals(RPL.name())) return RPL;
        if (RPL.aliases().contains(charsetName)) return RPL;

        return null;
    }
}

/**
 * The mapping table. Character 0x81 is special. It would need two UTF codes: An 'x', plus a modifier for the
 * "combine overline" (0305). The data structure is only set up for a single char, though. For now, return
 * a similar character (U2A30, ⨰).
 */
@SuppressWarnings({"UnnecessaryUnicodeEscape"})
class RplCharset extends Charset {
    private static final char specialX = Normalizer.normalize("\u2A30x", Normalizer.Form.NFC).charAt(0);
    private static final char[] Rpl = {
            '\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007',
            '\b', '\t', '\n', '\u000b', '\f', '\r', '\u000e', '\u000f',
            '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015', '\u0016', '\u0017',
            '\u0018', '\u0019', '\u001a', '\u001b', '\u001c', '\u001d', '\u001e', '\u001f',
            ' ', '!', '"', '#', '$', '%', '&', '\'',
            '(', ')', '*', '+', ',', '-', '.', '/',
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', ':', ';', '<', '=', '>', '?',
            '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G',
            'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
            'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
            'X', 'Y', 'Z', '[', '\\', ']', '^', '_',
            '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
            'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
            'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
            'x', 'y', 'z', '{', '|', '}', '~', '\u2592',
            '\u2221', specialX , '\u2207', '\u221a', '\u222b', '\u03a3', '\u25b6', '\u03c0',
            '\u2202', '\u2264', '\u2265', '\u2260', '\u03b1', '\u2192', '\u2190', '\u2193',
            '\u2191', '\u03b3', '\u03b4', '\u03b5', '\u03b7', '\u03b8', '\u03bb', '\u03c1',
            '\u03c3', '\u03c4', '\u03c9', '\u0394', '\u03a0', '\u03a9', '\u25a0', '\u221e',
            '\u00a0', '\u00a1', '\u00a2', '\u00a3', '\u00a4', '\u00a5', '\u00a6', '\u00a7',
            '\u00a8', '\u00a9', '\u00aa', '\u00ab', '\u00ac', '\u00ad', '\u00ae', '\u00af',
            '\u00b0', '\u00b1', '\u00b2', '\u00b3', '\u00b4', '\u00b5', '\u00b6', '\u00b7',
            '\u00b8', '\u00b9', '\u00ba', '\u00bb', '\u00bc', '\u00bd', '\u00be', '\u00bf',
            '\u00c0', '\u00c1', '\u00c2', '\u00c3', '\u00c4', '\u00c5', '\u00c6', '\u00c7',
            '\u00c8', '\u00c9', '\u00ca', '\u00cb', '\u00cc', '\u00cd', '\u00ce', '\u00cf',
            '\u00d0', '\u00d1', '\u00d2', '\u00d3', '\u00d4', '\u00d5', '\u00d6', '\u00d7',
            '\u00d8', '\u00d9', '\u00da', '\u00db', '\u00dc', '\u00dd', '\u00de', '\u00df',
            '\u00e0', '\u00e1', '\u00e2', '\u00e3', '\u00e4', '\u00e5', '\u00e6', '\u00e7',
            '\u00e8', '\u00e9', '\u00ea', '\u00eb', '\u00ec', '\u00ed', '\u00ee', '\u00ef',
            '\u00f0', '\u00f1', '\u00f2', '\u00f3', '\u00f4', '\u00f5', '\u00f6', '\u00f7',
            '\u00f8', '\u00f9', '\u00fa', '\u00fb', '\u00fc', '\u00fd', '\u00fe', '\u00ff'
    };
    private static final Map<Character, Byte> LOOKUP;

    static {
        Map<Character, Byte> map = new HashMap<>();
        for (int i = 0; i < Rpl.length; i++) map.put(Rpl[i], (byte) i);
        LOOKUP = Collections.unmodifiableMap(map);
    }

    public RplCharset() {
        super("HP-RPL", new String[]{"HpRpl", "CpHpRpl", "Cp-Hp-Rpl"});
    }

    @Override
    public boolean contains(Charset cs) {
        return cs.name().equals("HP-RPL"); // It isn't even a subset of ASCII!
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new CharsetDecoder(this, 1F, 1F) {
            @Override
            protected CoderResult decodeLoop(ByteBuffer from, CharBuffer to) {
                while (from.hasRemaining()) {
                    if (!to.hasRemaining()) return CoderResult.OVERFLOW;
                    byte c = from.get();
                    char d = Rpl[c & 0xFF];
                    to.put(d);
                }

                return CoderResult.UNDERFLOW;
            }
        };
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new CharsetEncoder(this, 1F, 1F) {
            @Override
            protected CoderResult encodeLoop(CharBuffer from, ByteBuffer to) {
                while (from.hasRemaining()) {
                    if (!to.hasRemaining()) return CoderResult.OVERFLOW;
                    char d = from.get();
                    Byte v = LOOKUP.get(d);
                    if (v == null) {
                        // 'un'consume the character we consumed
                        from.position(from.position() - 1);
                        return CoderResult.unmappableForLength(1);
                    }
                    to.put(v);
                }

                return CoderResult.UNDERFLOW;
            }
        };
    }
}
