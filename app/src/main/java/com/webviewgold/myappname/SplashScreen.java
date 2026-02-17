package com.webviewgold.myappname;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import pl.droidsonroids.gif.GifImageView;

public class SplashScreen extends AppCompatActivity {

    private static SplashScreen instance;
    private GifImageView splashImage;

    public static SplashScreen getInstance() {
        return instance;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            Bitmap bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(getResources().getColor(R.color.colorWhite));
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
            getWindow().setBackgroundDrawable(bitmapDrawable);
        }
        setContentView(R.layout.splash_screen);
        instance = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Config.blackStatusBarText) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
        if (Config.PREVENT_SLEEP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        splashImage = findViewById(R.id.splash);

        try {
            subScribePushChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (Config.SPLASH_SCREEN_ACTIVATED) {
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
            Handler handler = new Handler();

            if (!Config.REMAIN_SPLASH_OPTION && Config.SPLASH_FADE_OUT == 0) {
                handler.postDelayed(this::finish, Config.SPLASH_TIMEOUT);
            }

            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorWhite));
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorWhite));

        } else {
            splashImage.setVisibility(View.GONE);
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                finish();
            }, 0);
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
                                if (BuildConfig.IS_DEBUG_MODE)
                                    Log.d("Token", String.valueOf(FirebaseMessaging.getInstance()));
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

    public void animateFinish() {
        findViewById(android.R.id.content).animate()
                .alpha(0f)
                .setDuration(Config.SPLASH_FADE_OUT)
                .withEndAction(this::finish)
                .start();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
