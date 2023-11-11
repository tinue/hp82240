package ch.erzberger.emulation.common;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.spi.CharsetProvider;
import java.util.*;

/**
 * Mapping from the HP 82240A printer character set to Unicode.
 * To find the correct mapping, I consulted this Wikipedia Article:
 * <a href="https://en.wikipedia.org/wiki/HP_Roman">HP Roman</a>
 * Specifically for the extra characters 0x80 - 0x9F, see this section:
 * <a href="https://en.wikipedia.org/wiki/HP_Roman#Modified_Roman-8">Modified Roman 8</a>
 * The template for this CharsetProvider comes from a Stack Overflow answer:
 * <a href="https://stackoverflow.com/questions/76936549/charset-not-present-in-used-jdk/76936879#76936879">Charset not present in used JDK</a>
 */
public class Hp82240aCharsetProvider extends CharsetProvider {
    private static final Charset HP82240A = new Hp82240aCharset();
    private static final List<Charset> HP82240A_LIST = Collections.singletonList(HP82240A);

    /**
     * Use this helper to get access to the charset provider. Alternatively, register the provider formally.
     * @return The provider for the HP-82240A charset
     */
    public static Charset hp82240a() {
        return HP82240A;
    }

    @Override
    public Iterator<Charset> charsets() {
        return HP82240A_LIST.iterator();
    }

    @Override
    public Charset charsetForName(String charsetName) {
        if (charsetName.equals(HP82240A.name())) return HP82240A;
        if (HP82240A.aliases().contains(charsetName)) return HP82240A;

        return null;
    }
}

/**
 * The mapping table.
 */
@SuppressWarnings({"UnnecessaryUnicodeEscape"})
class Hp82240aCharset extends Charset {
    private static final char[] HP82240A = {
            '\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007',
            '\b',     '\t',     '\n',     '\u000b', '\f',     '\r',     '\u000e', '\u000f',
            '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015', '\u0016', '\u0017',
            '\u0018', '\u0019', '\u001a', '\u001b', '\u001c', '\u001d', '\u001e', '\u001f',
            ' ',      '!',      '"',      '#',      '$',      '%',      '&',      '\'',
            '(',      ')',      '*',      '+',      ',',      '-',      '.',      '/',
            '0',      '1',      '2',      '3',      '4',      '5',      '6',      '7',
            '8',      '9',      ':',      ';',      '<',      '=',      '>',      '?',
            '@',      'A',      'B',      'C',      'D',      'E',      'F',      'G',
            'H',      'I',      'J',      'K',      'L',      'M',      'N',      'O',
            'P',      'Q',      'R',      'S',      'T',      'U',      'V',      'W',
            'X',      'Y',      'Z',      '[',      '\\',     ']',     '^',     '_',
            '`',      'a',      'b',      'c',      'd',      'e',      'f',      'g',
            'h',      'i',      'j',      'k',      'l',      'm',      'n',      'o',
            'p',      'q',      'r',      's',      't',      'u',      'v',      'w',
            'x',      'y',      'z',      '{',      '|',      '}',      '~',      '\u2592',
            '\u00a0', '\u00f7', '\u00d7', '\u221a', '\u222b', '\u03a3', '\u25b6', '\u03c0',
            '\u2202', '\u2264', '\u2265', '\u2260', '\u03b1', '\u2192', '\u2190', '\u03bc',
            '\u240a', '\u00b0', '\u00ab', '\u00bb', '\u22a6', '\u2081', '\u2082', '\u00b2',
            '\u00b3', '\u1d62', '\u2c7c', '\u2025', '\u2071', '\u02b2', '\u1d4f', '\u207f',
            '\u2221', '\u00c0', '\u00c2', '\u00c8', '\u00ca', '\u00cb', '\u00ce', '\u00cf',
            '\u00b4', '\u02cb', '\u02c6', '\u00a8', '\u02dc', '\u00d9', '\u00db', '\u20a4',
            '\u00af', '\u00dd', '\u00fd', '\u00b0', '\u00c7', '\u00e7', '\u00d1', '\u00f1',
            '\u00a1', '\u00bf', '\u00a4', '\u00a3', '\u00a5', '\u00a7', '\u0192', '\u00a2',
            '\u00e2', '\u00ea', '\u00f4', '\u00fb', '\u00e1', '\u00e9', '\u00f3', '\u00fa',
            '\u00e0', '\u00e8', '\u00f2', '\u00f9', '\u00e4', '\u00eb', '\u00f6', '\u00fc',
            '\u00c5', '\u00ee', '\u00d8', '\u00c6', '\u00e5', '\u00ed', '\u00f8', '\u00e6',
            '\u00c4', '\u00ec', '\u00d6', '\u00dc', '\u00c9', '\u00ef', '\u00df', '\u00d4',
            '\u00c1', '\u00c3', '\u00e3', '\u00d0', '\u00f0', '\u00cd', '\u00cc', '\u00d3',
            '\u00d2', '\u00d5', '\u00f5', '\u0160', '\u0161', '\u00da', '\u0178', '\u00ff',
            '\u00de', '\u00fe', '\u00b7', '\u00b5', '\u00b6', '\u00be', '\u2014', '\u00bc',
            '\u00bd', '\u00aa', '\u00ba', '\u00ab', '\u25a0', '\u00bb', '\u00b1', '�'
    };
    private static final Map<Character, Byte> LOOKUP;

    static {
        Map<Character, Byte> map = new HashMap<>();
        for (int i = 0; i < HP82240A.length; i++) map.put(HP82240A[i], (byte) i);
        LOOKUP = Collections.unmodifiableMap(map);
    }

    public Hp82240aCharset() {
        super("HP-82240A", new String[]{"HP82240A", "Cp82240A", "Cp-82240A"});
    }

    @Override
    public boolean contains(Charset cs) {
        return cs.name().equals("HP-82240A"); // It isn't even a subset of ASCII!
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new CharsetDecoder(this, 1F, 1F) {
            @Override
            protected CoderResult decodeLoop(ByteBuffer from, CharBuffer to) {
                while (from.hasRemaining()) {
                    if (!to.hasRemaining()) return CoderResult.OVERFLOW;
                    byte c = from.get();
                    char d = HP82240A[c & 0xFF];
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
