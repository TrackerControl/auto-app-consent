package net.kollnig.consent.standards;

import android.util.Base64;

/**
 * Encodes IAB TCF v2.2 TC Strings (Transparency & Consent strings).
 *
 * The TC String is a base64url-encoded binary format that encodes the full
 * consent record: CMP info, purpose consents, vendor consents, etc.
 * Many SDKs (notably Google Ads) check IABTCF_TCString before reading
 * the individual IABTCF_PurposeConsents fields.
 *
 * Format reference:
 * https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/
 * blob/master/TCFv2/IAB%20Tech%20Lab%20-%20Consent%20string%20and%20vendor%20list%20formats%20v2.md
 */
public class TcStringEncoder {

    // TCF epoch: 2020-01-01T00:00:00Z in milliseconds
    private static final long TCF_EPOCH_MS = 1577836800000L;

    private final BitWriter bits = new BitWriter();

    /**
     * Encode a TC String for a blanket consent/deny decision.
     *
     * @param cmpId              CMP SDK ID (0 = unregistered)
     * @param cmpVersion         CMP SDK version
     * @param consentLanguage    Two-letter ISO 639-1 language code (e.g. "EN")
     * @param publisherCC        Two-letter ISO 3166-1 publisher country code
     * @param purposeConsents    boolean[24] — purposes 1-24 (true = consent)
     * @param purposeLI          boolean[24] — purposes 1-24 legitimate interest
     * @param specialFeatures    boolean[12] — special features 1-12
     * @param vendorListVersion  Version of the GVL used (0 if none)
     * @return Base64url-encoded TC String (core segment only)
     */
    public static String encode(int cmpId, int cmpVersion,
                                String consentLanguage, String publisherCC,
                                boolean[] purposeConsents, boolean[] purposeLI,
                                boolean[] specialFeatures,
                                int vendorListVersion) {

        BitWriter bw = new BitWriter();

        long now = System.currentTimeMillis();
        long deciseconds = (now - TCF_EPOCH_MS) / 100;

        // Core segment
        bw.writeInt(2, 6);                          // Version = 2
        bw.writeLong(deciseconds, 36);              // Created
        bw.writeLong(deciseconds, 36);              // LastUpdated
        bw.writeInt(cmpId, 12);                     // CmpId
        bw.writeInt(cmpVersion, 12);                // CmpVersion
        bw.writeInt(1, 6);                          // ConsentScreen (1 = first screen)
        bw.writeLanguage(consentLanguage);           // ConsentLanguage (12 bits)
        bw.writeInt(vendorListVersion, 12);          // VendorListVersion
        bw.writeInt(4, 6);                          // TcfPolicyVersion = 4 (v2.2)
        bw.writeBit(true);                           // IsServiceSpecific = true
        bw.writeBit(false);                          // UseNonStandardTexts = false

        // SpecialFeatureOptIns — 12 bits
        for (int i = 0; i < 12; i++) {
            bw.writeBit(i < specialFeatures.length && specialFeatures[i]);
        }

        // PurposesConsent — 24 bits
        for (int i = 0; i < 24; i++) {
            bw.writeBit(i < purposeConsents.length && purposeConsents[i]);
        }

        // PurposesLITransparency — 24 bits
        for (int i = 0; i < 24; i++) {
            bw.writeBit(i < purposeLI.length && purposeLI[i]);
        }

        // PurposeOneTreatment — 1 bit
        bw.writeBit(false);

        // PublisherCC — 12 bits
        bw.writeLanguage(publisherCC);

        // Vendor consent section — empty (no specific vendor consents)
        bw.writeInt(0, 16);    // MaxVendorConsentId = 0
        bw.writeBit(false);     // IsRangeEncoding = false (bitfield, but empty)

        // Vendor legitimate interest section — empty
        bw.writeInt(0, 16);    // MaxVendorLIId = 0
        bw.writeBit(false);     // IsRangeEncoding = false

        // Publisher restrictions — 0 restrictions
        bw.writeInt(0, 12);    // NumPubRestrictions = 0

        return bw.toBase64Url();
    }

    /**
     * Convenience: encode a blanket consent/deny for all purposes.
     */
    public static String encode(int cmpId, int cmpVersion,
                                String consentLanguage, String publisherCC,
                                boolean consent) {
        boolean[] purposes = new boolean[24];
        boolean[] purposesLI = new boolean[24];
        boolean[] specialFeatures = new boolean[12];

        if (consent) {
            // Consent to all 11 TCF purposes (indices 0-10)
            for (int i = 0; i < TcfConsentManager.PURPOSE_COUNT; i++) {
                purposes[i] = true;
                purposesLI[i] = true;
            }
            // Opt in to both special features
            for (int i = 0; i < TcfConsentManager.SPECIAL_FEATURE_COUNT; i++) {
                specialFeatures[i] = true;
            }
        }

        return encode(cmpId, cmpVersion, consentLanguage, publisherCC,
                purposes, purposesLI, specialFeatures, 0);
    }

    /**
     * Bit-level writer that accumulates bits and encodes to base64url.
     */
    static class BitWriter {
        private final StringBuilder bits = new StringBuilder();

        void writeBit(boolean value) {
            bits.append(value ? '1' : '0');
        }

        void writeInt(int value, int numBits) {
            for (int i = numBits - 1; i >= 0; i--) {
                bits.append(((value >> i) & 1) == 1 ? '1' : '0');
            }
        }

        void writeLong(long value, int numBits) {
            for (int i = numBits - 1; i >= 0; i--) {
                bits.append(((value >> i) & 1) == 1 ? '1' : '0');
            }
        }

        /**
         * Write a two-letter code as 12 bits (two 6-bit values, A=0 ... Z=25).
         */
        void writeLanguage(String twoLetterCode) {
            if (twoLetterCode == null || twoLetterCode.length() != 2) {
                writeInt(0, 12);
                return;
            }
            String upper = twoLetterCode.toUpperCase();
            writeInt(upper.charAt(0) - 'A', 6);
            writeInt(upper.charAt(1) - 'A', 6);
        }

        /**
         * Convert the accumulated bits to a base64url string (no padding).
         */
        String toBase64Url() {
            // Pad to multiple of 8 bits
            while (bits.length() % 8 != 0) {
                bits.append('0');
            }

            byte[] bytes = new byte[bits.length() / 8];
            for (int i = 0; i < bytes.length; i++) {
                int b = 0;
                for (int j = 0; j < 8; j++) {
                    b = (b << 1) | (bits.charAt(i * 8 + j) == '1' ? 1 : 0);
                }
                bytes[i] = (byte) b;
            }

            // Base64url encoding (no padding, URL-safe)
            return Base64.encodeToString(bytes,
                    Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        }
    }
}
