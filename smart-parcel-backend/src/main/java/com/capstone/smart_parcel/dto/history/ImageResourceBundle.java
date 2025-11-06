package com.capstone.smart_parcel.dto.history;

/**
 * Simple DTO wrapper that exposes the three image variants used by the web UI.
 */
public record ImageResourceBundle(
        String original,
        String thumbnail,
        String snapshot
) {

    public static ImageResourceBundle single(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return new ImageResourceBundle(null, null, null);
        }
        return new ImageResourceBundle(baseUrl, null, null);
    }

    public static ImageResourceBundle of(String original, String thumbnail, String snapshot) {
        return new ImageResourceBundle(original, thumbnail, snapshot);
    }
}
