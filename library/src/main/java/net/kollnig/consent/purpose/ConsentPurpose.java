package net.kollnig.consent.purpose;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines consent purposes (categories) and maps SDKs to them.
 *
 * Under GDPR/TCF, consent is organized by PURPOSE, not by vendor.
 * Users consent to "Analytics" or "Advertising", not to "InMobi" or "AppsFlyer".
 *
 * Each purpose maps to one or more TCF purpose IDs and one or more SDK library IDs.
 * When a user grants/denies consent to a purpose, all associated SDKs are affected.
 */
public class ConsentPurpose {

    /** Unique identifier for this purpose. */
    public final String id;

    /** String resource ID for the display name (e.g. "Analytics"). */
    public final int nameResId;

    /** String resource ID for the description shown in the consent dialog. */
    public final int descriptionResId;

    /** TCF v2.2 purpose IDs associated with this purpose (1-indexed). */
    public final int[] tcfPurposeIds;

    /** SDK library IDs that belong to this purpose. */
    public final List<String> libraryIds;

    /** Whether this purpose is strictly necessary (no consent needed). */
    public final boolean essential;

    public ConsentPurpose(String id, int nameResId, int descriptionResId,
                          int[] tcfPurposeIds, List<String> libraryIds, boolean essential) {
        this.id = id;
        this.nameResId = nameResId;
        this.descriptionResId = descriptionResId;
        this.tcfPurposeIds = tcfPurposeIds;
        this.libraryIds = Collections.unmodifiableList(libraryIds);
        this.essential = essential;
    }

    /**
     * Returns the default purpose definitions with SDK mappings.
     * Ordered as they should appear in the consent dialog.
     */
    public static Map<String, ConsentPurpose> getDefaults() {
        // Use LinkedHashMap to preserve insertion order for dialog display
        Map<String, ConsentPurpose> purposes = new LinkedHashMap<>();

        purposes.put(PURPOSE_ANALYTICS, new ConsentPurpose(
                PURPOSE_ANALYTICS,
                net.kollnig.consent.R.string.purpose_analytics_name,
                net.kollnig.consent.R.string.purpose_analytics_desc,
                new int[]{7, 8, 9},  // Measure ad perf, content perf, market research
                Arrays.asList("firebase_analytics", "flurry", "appsflyer"),
                false
        ));

        purposes.put(PURPOSE_ADVERTISING, new ConsentPurpose(
                PURPOSE_ADVERTISING,
                net.kollnig.consent.R.string.purpose_advertising_name,
                net.kollnig.consent.R.string.purpose_advertising_desc,
                new int[]{2, 3, 4},  // Basic ads, personalized ads profile, select personalized ads
                Arrays.asList("google_ads", "applovin", "inmobi", "adcolony",
                        "ironsource", "vungle"),
                false
        ));

        purposes.put(PURPOSE_CRASH_REPORTING, new ConsentPurpose(
                PURPOSE_CRASH_REPORTING,
                net.kollnig.consent.R.string.purpose_crash_reporting_name,
                net.kollnig.consent.R.string.purpose_crash_reporting_desc,
                new int[]{10},  // Develop and improve products
                Arrays.asList("crashlytics"),
                false
        ));

        purposes.put(PURPOSE_SOCIAL, new ConsentPurpose(
                PURPOSE_SOCIAL,
                net.kollnig.consent.R.string.purpose_social_name,
                net.kollnig.consent.R.string.purpose_social_desc,
                new int[]{1},  // Store and/or access information on a device
                Arrays.asList("facebook_sdk"),
                false
        ));

        purposes.put(PURPOSE_IDENTIFICATION, new ConsentPurpose(
                PURPOSE_IDENTIFICATION,
                net.kollnig.consent.R.string.purpose_identification_name,
                net.kollnig.consent.R.string.purpose_identification_desc,
                new int[]{},  // Maps to Special Feature 2 (device scanning), not a purpose
                Arrays.asList("google_ads_identifier"),
                false
        ));

        return purposes;
    }

    // Purpose ID constants
    public static final String PURPOSE_ANALYTICS = "analytics";
    public static final String PURPOSE_ADVERTISING = "advertising";
    public static final String PURPOSE_CRASH_REPORTING = "crash_reporting";
    public static final String PURPOSE_SOCIAL = "social";
    public static final String PURPOSE_IDENTIFICATION = "identification";
}
