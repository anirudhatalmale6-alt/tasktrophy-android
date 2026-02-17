package com.webviewgold.myappname.advertisement;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.google.android.ump.ConsentForm.OnConsentFormDismissedListener;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

/**
 * The Google Mobile Ads SDK provides the User Messaging Platform (Google's IAB Certified consent
 * management platform) as one solution to capture consent for users in GDPR impacted countries.
 * This is an example and you can choose another consent management platform to capture consent.
 */
public class GoogleMobileAdsConsentManager {
    private static GoogleMobileAdsConsentManager instance;
    private final ConsentInformation consentInformation;

    /** Private constructor */
    private GoogleMobileAdsConsentManager(Context context) {
        this.consentInformation = UserMessagingPlatform.getConsentInformation(context);
    }

    /** Public constructor */
    public static GoogleMobileAdsConsentManager getInstance(Context context) {
        if (instance == null) {
            instance = new GoogleMobileAdsConsentManager(context);
        }

        return instance;
    }

    /** Interface definition for a callback to be invoked when consent gathering is complete. */
    public interface OnConsentGatheringCompleteListener {
        void consentGatheringComplete(FormError error);
    }

    /** Helper variable to determine if the app can request ads. */
    public boolean canRequestAds() {
        return consentInformation.canRequestAds();
    }

    // [START is_privacy_options_required]
    /** Helper variable to determine if the privacy options form is required. */
    public boolean isPrivacyOptionsRequired() {
        return consentInformation.getPrivacyOptionsRequirementStatus()
                == PrivacyOptionsRequirementStatus.REQUIRED;
    }

    // [END is_privacy_options_required]

    /**
     * Helper method to call the UMP SDK methods to request consent information and load/present a
     * consent form if necessary.
     */
    public void gatherConsent(
            Activity activity, OnConsentGatheringCompleteListener onConsentGatheringCompleteListener) {
        // Production settings - no debug geography, consent is determined by user's actual location
        ConsentRequestParameters params =
                new ConsentRequestParameters.Builder()
                        .setTagForUnderAgeOfConsent(false)
                        .build();

        // [START gather_consent]
        // Requesting an update to consent information should be called on every app launch.
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () ->
                        // Consent has been gathered.
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                                activity,
                                onConsentGatheringCompleteListener::consentGatheringComplete),
                onConsentGatheringCompleteListener::consentGatheringComplete);
        // [END gather_consent]
    }

    /** Helper method to call the UMP SDK method to present the privacy options form. */
    public void showPrivacyOptionsForm(
            Activity activity, OnConsentFormDismissedListener onConsentFormDismissedListener) {
        // [START present_privacy_options_form]
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener);
        // [END present_privacy_options_form]
    }


    @VisibleForTesting
    public void reset(){
        consentInformation.reset();
    }
}
