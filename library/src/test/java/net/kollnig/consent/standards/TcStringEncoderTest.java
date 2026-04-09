package net.kollnig.consent.standards;

import android.util.Base64;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class TcStringEncoderTest {

    @Test
    public void encode_blanketConsent_returnsNonEmpty() {
        String tc = TcStringEncoder.encode(42, 1, "EN", "DE", true);
        assertNotNull(tc);
        assertFalse(tc.isEmpty());
    }

    @Test
    public void encode_blanketDeny_returnsNonEmpty() {
        String tc = TcStringEncoder.encode(42, 1, "EN", "DE", false);
        assertNotNull(tc);
        assertFalse(tc.isEmpty());
    }

    @Test
    public void encode_consentAndDeny_areDifferent() {
        String consentTc = TcStringEncoder.encode(42, 1, "EN", "DE", true);
        String denyTc = TcStringEncoder.encode(42, 1, "EN", "DE", false);
        assertNotEquals("Consent and deny TC strings should differ", consentTc, denyTc);
    }

    @Test
    public void encode_isValidBase64Url() {
        String tc = TcStringEncoder.encode(42, 1, "EN", "DE", true);
        // Base64url should not contain +, /, or =
        assertFalse("Should not contain +", tc.contains("+"));
        assertFalse("Should not contain /", tc.contains("/"));
        assertFalse("Should not contain =", tc.contains("="));
    }

    @Test
    public void encode_decodesToValidBits() {
        String tc = TcStringEncoder.encode(42, 1, "EN", "DE", true);
        byte[] decoded = Base64.decode(tc, Base64.URL_SAFE);
        assertNotNull(decoded);
        assertTrue("Decoded bytes should be non-empty", decoded.length > 0);
    }

    @Test
    public void encode_versionIs2() {
        String tc = TcStringEncoder.encode(42, 1, "EN", "DE", true);
        byte[] decoded = Base64.decode(tc, Base64.URL_SAFE);
        // Version is the first 6 bits
        int version = (decoded[0] >> 2) & 0x3F;
        assertEquals("Version should be 2", 2, version);
    }

    @Test
    public void encode_perPurpose_returnsNonEmpty() {
        boolean[] purposes = new boolean[24];
        purposes[0] = true;  // Purpose 1
        purposes[6] = true;  // Purpose 7
        boolean[] purposesLI = new boolean[24];
        boolean[] specialFeatures = new boolean[12];

        String tc = TcStringEncoder.encode(42, 1, "EN", "DE",
                purposes, purposesLI, specialFeatures, 0);
        assertNotNull(tc);
        assertFalse(tc.isEmpty());
    }

    @Test
    public void encode_cmpIdIsEncoded() {
        // Encode with CMP ID 42 and 100 — they should produce different strings
        String tc42 = TcStringEncoder.encode(42, 1, "EN", "DE", true);
        String tc100 = TcStringEncoder.encode(100, 1, "EN", "DE", true);
        assertNotEquals("Different CMP IDs should produce different strings", tc42, tc100);
    }

    @Test
    public void encode_differentLanguages_differ() {
        String enTc = TcStringEncoder.encode(42, 1, "EN", "DE", true);
        String frTc = TcStringEncoder.encode(42, 1, "FR", "DE", true);
        assertNotEquals("Different languages should produce different strings", enTc, frTc);
    }

    @Test
    public void bitWriter_writeInt_correctBits() {
        TcStringEncoder.BitWriter bw = new TcStringEncoder.BitWriter();
        bw.writeInt(2, 6); // Binary: 000010
        String base64 = bw.toBase64Url();
        byte[] decoded = Base64.decode(base64, Base64.URL_SAFE);
        // 000010 + 00 padding = 00001000 = 8
        assertEquals(8, decoded[0] & 0xFF);
    }

    @Test
    public void bitWriter_writeBit_correctBits() {
        TcStringEncoder.BitWriter bw = new TcStringEncoder.BitWriter();
        bw.writeBit(true);
        bw.writeBit(false);
        bw.writeBit(true);
        bw.writeBit(false);
        bw.writeBit(true);
        bw.writeBit(false);
        bw.writeBit(true);
        bw.writeBit(false);
        String base64 = bw.toBase64Url();
        byte[] decoded = Base64.decode(base64, Base64.URL_SAFE);
        // 10101010 = 0xAA = 170
        assertEquals(0xAA, decoded[0] & 0xFF);
    }

    @Test
    public void bitWriter_writeLanguage_EN() {
        TcStringEncoder.BitWriter bw = new TcStringEncoder.BitWriter();
        bw.writeLanguage("EN");
        // E = 4 (000100), N = 13 (001101)
        // 000100 001101 = 12 bits
        String base64 = bw.toBase64Url();
        byte[] decoded = Base64.decode(base64, Base64.URL_SAFE);
        // 00010000 1101xxxx (padded to 16 bits)
        assertEquals(0x10, decoded[0] & 0xFF); // 000100 00
        assertEquals(0xD0, decoded[1] & 0xF0); // 1101 0000
    }
}
