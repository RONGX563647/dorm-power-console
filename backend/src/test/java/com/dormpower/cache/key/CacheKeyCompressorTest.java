package com.dormpower.cache.key;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class CacheKeyCompressorTest {

    private CacheKeyCompressor compressor;

    @BeforeEach
    void setUp() {
        compressor = new CacheKeyCompressor();
        ReflectionTestUtils.setField(compressor, "keyLengthThreshold", 100);
        ReflectionTestUtils.setField(compressor, "compressionEnabled", true);
    }

    @Test
    void testCompress_ShortKey_ReturnsOriginal() {
        String shortKey = "shortKey";

        String result = compressor.compress(shortKey);

        assertEquals(shortKey, result);
    }

    @Test
    void testCompress_LongKey_ReturnsHashed() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            sb.append("a");
        }
        String longKey = sb.toString();

        String result = compressor.compress(longKey);

        assertTrue(result.startsWith("hash:"));
        assertTrue(result.length() < longKey.length());
    }

    @Test
    void testCompress_NullKey_ReturnsNull() {
        String result = compressor.compress(null);

        assertNull(result);
    }

    @Test
    void testCompress_Disabled_ReturnsOriginal() {
        ReflectionTestUtils.setField(compressor, "compressionEnabled", false);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            sb.append("a");
        }
        String longKey = sb.toString();

        String result = compressor.compress(longKey);

        assertEquals(longKey, result);
    }

    @Test
    void testNeedsCompression_ShortKey_ReturnsFalse() {
        assertFalse(compressor.needsCompression("shortKey"));
    }

    @Test
    void testNeedsCompression_LongKey_ReturnsTrue() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            sb.append("a");
        }
        String longKey = sb.toString();

        assertTrue(compressor.needsCompression(longKey));
    }

    @Test
    void testNeedsCompression_NullKey_ReturnsFalse() {
        assertFalse(compressor.needsCompression(null));
    }

    @Test
    void testSameInput_SameHash() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            sb.append("a");
        }
        String longKey = sb.toString();

        String result1 = compressor.compress(longKey);
        String result2 = compressor.compress(longKey);

        assertEquals(result1, result2);
    }
}
