package com.webviewgold.myappname.deeplinking;

import android.content.Intent;
import android.net.Uri;

import com.webviewgold.myappname.Config;

public final class Deeplinking {

    private Deeplinking() {}

    public static String constructDeeplink(Intent intent) {
        Uri data = intent.getData();

        String fullDeepLinkingURL = data.getScheme() + "://" + data.getHost() + data.getPath();

        // Get the entire query string, if any
        String query = data.getQuery();

        // If the query string is not null or empty, append it to the URL
        if (query != null && !query.isEmpty()) {
            fullDeepLinkingURL += "?" + query; // The query string already contains the correct format
        }

        return fullDeepLinkingURL;
    }

    public static String convertSchemeToHttps(String schemeUrl) {
        return schemeUrl.replace(
                Config.CUSTOM_DEEPLINK_SCHEME + "://",
                "https://");
    }

    public static boolean isValidSchemeURL(String schemeUrl) {
        return schemeUrl.startsWith(Config.CUSTOM_DEEPLINK_SCHEME + "://");
    }
}
