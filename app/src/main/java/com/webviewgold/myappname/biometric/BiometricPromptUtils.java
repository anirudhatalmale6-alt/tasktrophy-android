package com.webviewgold.myappname.biometric;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.webviewgold.myappname.R;

import java.util.concurrent.Executor;


public class BiometricPromptUtils {

    private static final String TAG = "BiometricPromptUtils";

    public static BiometricPrompt createBiometricPrompt(
            AppCompatActivity activity,
            BiometricPrompt.AuthenticationCallback processSuccess
    ) {
        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {

            @Override
            public void onAuthenticationError(int errCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errCode, errString);
                processSuccess.onAuthenticationError(errCode, errString);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                processSuccess.onAuthenticationFailed();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Log.d(TAG, "Authentication was successful");
                processSuccess.onAuthenticationSucceeded(result);
            }
        };

        return new BiometricPrompt(activity, executor, callback);
    }

    public static BiometricPrompt.PromptInfo createPromptInfo(AppCompatActivity activity) {
        return new BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.prompt_info_title))
                .setSubtitle(activity.getString(R.string.prompt_info_subtitle))
                .setDescription(activity.getString(R.string.prompt_info_description))
                .setConfirmationRequired(false)
                .setNegativeButtonText(activity.getString(R.string.prompt_info_cancel))
                .build();
    }
}
