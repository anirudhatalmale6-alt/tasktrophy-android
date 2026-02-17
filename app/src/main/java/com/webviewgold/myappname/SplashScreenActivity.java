package com.webviewgold.myappname;

import static com.webviewgold.myappname.Config.REMAIN_SPLASH_OPTION;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import pl.droidsonroids.gif.GifImageView;
@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends BaseActivity {

    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        String userAgent = WebSettings.getDefaultUserAgent(this);
        // Phone orientation setting for Android 8 (Oreo)
        if (userAgent.contains("Mobile") && android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            if (Config.PHONE_ORIENTATION == "auto") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            } else if (Config.PHONE_ORIENTATION == "portrait") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            } else if (Config.PHONE_ORIENTATION == "landscape") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            }
            // Phone orientation setting for all other Android versions
        } else if (userAgent.contains("Mobile")) {
            if (Config.PHONE_ORIENTATION == "auto") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            } else if (Config.PHONE_ORIENTATION == "portrait") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else if (Config.PHONE_ORIENTATION == "landscape") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            // Tablet/Other orientation setting
        } else {
            if (Config.TABLET_ORIENTATION == "auto") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            } else if (Config.TABLET_ORIENTATION == "portrait") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else if (Config.TABLET_ORIENTATION == "landscape") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
        setContentView(R.layout.splash_screen);

        GifImageView splashImage = findViewById(R.id.splash);
        if (!Config.SPLASH_SCREEN_ACTIVATED) {
            splashImage.setVisibility(View.GONE);
            Handler handlerskip = new Handler();
            handlerskip.postDelayed(this::openNextActivity, 1);
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        if (Config.SCALE_SPLASH_IMAGE != 100) {
            // Do not run this if SCALE_SPLASH_IMAGE = 100 as otherwise no real full-screen
            ViewGroup.LayoutParams params = splashImage.getLayoutParams();
            if (width < height
                    /*&& (display.getRotation() == Surface.ROTATION_0
                    || display.getRotation() == Surface.ROTATION_180)*/) {
                if (0 <= Config.SCALE_SPLASH_IMAGE && Config.SCALE_SPLASH_IMAGE <= 100) {
                    params.width = (int) (width * Config.SCALE_SPLASH_IMAGE / 100);
                } else {
                    params.width = width;
                }
            } else {
                if (0 <= Config.SCALE_SPLASH_IMAGE && Config.SCALE_SPLASH_IMAGE <= 100) {
                    params.width = (int) (height * Config.SCALE_SPLASH_IMAGE / 100);
                } else {
                    params.width = height;
                }
            }
            params.height = params.width;
            splashImage.setLayoutParams(params);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Config.blackStatusBarText) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (BuildConfig.IS_DEBUG_MODE) Log.d("TAG", "onCreate: RUN");
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
        getWindow().setNavigationBarColor(getResources().getColor(R.color.colorWhite));
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorWhite));

        ConstraintLayout btnUnlock = findViewById(R.id.btnUnlock);
        btnUnlock.setOnClickListener(v -> showBiometricPrompt());
        if (Config.enableBioMetricAuth) {
            btnUnlock.setVisibility(View.VISIBLE);
            showBiometricPrompt();
        } else {
            btnUnlock.setVisibility(View.GONE);
            Handler handler = new Handler();
            if (!REMAIN_SPLASH_OPTION && Config.SPLASH_FADE_OUT == 0){
                handler.postDelayed(this::openNextActivity, Config.SPLASH_TIMEOUT);
            }
        }

        try {
            subScribePushChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (REMAIN_SPLASH_OPTION || Config.SPLASH_FADE_OUT != 0) {
            // There are another functionality to let the user wait on splash. So we have to navigate the user to the next screen.
            Handler handler = new Handler();
            handler.post(() -> {
                // double-post means the screen rotation is triggered when next activity starts
                handler.post(this::openNextActivity);
            });
        }
    }

    private void subScribePushChannel() {
        try {
            if (Config.FIREBASE_PUSH_ENABLED) {
                if (BuildConfig.IS_DEBUG_MODE) Log.d("Splash_Screen", "Subscribing to topic");

                // [START subscribe_topics]
                FirebaseMessaging.getInstance().subscribeToTopic(Config.firebasechanneltopic)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                String msg = getString(R.string.msg_subscribed);
                                if (BuildConfig.IS_DEBUG_MODE) Log.d("Token", String.valueOf(FirebaseMessaging.getInstance()));
                                if (!task.isSuccessful()) {
                                    msg = getString(R.string.msg_subscribe_failed);
                                }
                                if (BuildConfig.IS_DEBUG_MODE) Log.d("Splash_Screen", msg);
                            }
                        });
                // [END subscribe_topics]
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showBiometricPrompt() {
        super.showBiometricPrompt(this::openNextActivity);
    }


    private void openNextActivity() {

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if (getIntent().getExtras() != null) {
            String url = getIntent().getExtras().getString("url");
            if (url != null) {
                if (!url.isEmpty()) {
                    intent.putExtra("ONESIGNAL_URL", url);
                }
            }
        }
        intent.putExtra(MainActivity.KEY_SKIP_BIOMETRIC, true);
        startActivity(intent);
        overridePendingTransition(0, 0);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                finish();
                overridePendingTransition(0, 0);
            }
        }, 30);

    }
}
