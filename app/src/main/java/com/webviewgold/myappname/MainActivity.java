package com.webviewgold.myappname;

import static android.content.Intent.CATEGORY_DEFAULT;
import static android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NO_HISTORY;
import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
import static com.webviewgold.myappname.BuildConfig.ONESIGNAL_APP_ID;
import static com.webviewgold.myappname.Config.ACTIVATE_PROGRESS_BAR;
import static com.webviewgold.myappname.Config.ASK_FOR_AD_CONSENT;
import static com.webviewgold.myappname.Config.AUTO_DOWNLOAD_FILES;
import static com.webviewgold.myappname.Config.BROWSER_BLACKLIST;
import static com.webviewgold.myappname.Config.BROWSER_WHITELIST;
import static com.webviewgold.myappname.Config.DISABLE_DARK_MODE;
import static com.webviewgold.myappname.Config.ENABLE_PULL_REFRESH;
import static com.webviewgold.myappname.Config.ENABLE_SWIPE_NAVIGATE;
import static com.webviewgold.myappname.Config.ENABLE_ZOOM;
import static com.webviewgold.myappname.Config.EXIT_APP_DIALOG;
import static com.webviewgold.myappname.Config.HIDE_HORIZONTAL_SCROLLBAR;
import static com.webviewgold.myappname.Config.HIDE_NAVIGATION_BAR_IN_LANDSCAPE;
import static com.webviewgold.myappname.Config.HIDE_VERTICAL_SCROLLBAR;
import static com.webviewgold.myappname.Config.INCREMENT_WITH_REDIRECTS;
import static com.webviewgold.myappname.Config.INCREMENT_WITH_TAPS;
import static com.webviewgold.myappname.Config.MAX_TEXT_ZOOM;
import static com.webviewgold.myappname.Config.NEVER_OPEN_IN_INAPP_TAB;
import static com.webviewgold.myappname.Config.PREVENT_SLEEP;
import static com.webviewgold.myappname.Config.REMAIN_SPLASH_OPTION;
import static com.webviewgold.myappname.Config.SPECIAL_LINK_HANDLING_OPTIONS;
import static com.webviewgold.myappname.Config.SPLASH_SCREEN_ACTIVATED;
import static com.webviewgold.myappname.Config.downloadableExtension;
import static com.webviewgold.myappname.Config.enableBioMetricAuth;
import static com.webviewgold.myappname.Config.requireBackgroundLocation;
import static com.webviewgold.myappname.WebViewApp.context;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.StrictMode;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingConfig;
import com.android.billingclient.api.BillingConfigResponseListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.GetBillingConfigParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchaseHistoryParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.blikoon.qrcodescanner.QrCodeActivity;
import com.facebook.ads.AdSize;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.util.IOUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import com.onesignal.inAppMessages.IInAppMessageClickEvent;
import com.onesignal.inAppMessages.IInAppMessageClickListener;
import com.onesignal.notifications.INotificationClickEvent;
import com.onesignal.notifications.INotificationClickListener;
import com.onesignal.notifications.IPermissionObserver;
import com.pushwoosh.Pushwoosh;
import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.EntitlementInfos;
import com.revenuecat.purchases.PurchaseParams;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback;
import com.revenuecat.purchases.interfaces.LogInCallback;
import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.models.StoreProduct;
import com.revenuecat.purchases.models.StoreTransaction;
import com.webviewgold.myappname.advertisement.GoogleMobileAdsConsentManager;
import com.webviewgold.myappname.biometric.BiometricPromptUtils;
import com.webviewgold.myappname.deeplinking.Deeplinking;
import com.webviewgold.myappname.flashlight.FlashLightManager;
import com.webviewgold.myappname.videoplayer.NativeVideoActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity
        implements IPermissionObserver,
        PurchasesUpdatedListener {
    public static String KEY_SKIP_BIOMETRIC = "skipBiometric";

    public static boolean HIDE_ADS_FOR_PURCHASE = false;
    public static final int PERMISSION_REQUEST_CODE = 9541;
    public final int LOCATION_PERMISSION_REQUEST_CODE = 5454;
    public static final String ERROR_DETECTED = "No NFC tag detected!";
    public static final String WRITE_SUCCESS = "Text written to the NFC tag successfully!";
    public static final String WRITE_ERROR = "Error during writing, is the NFC tag close enough to your device?";

    private static final String INDEX_FILE = "file:///android_asset/local-html/index.html";
    private static final int CODE_AUDIO_CHOOSER = 5678;
    private boolean isErrorPageLoaded = false;
    public static boolean requireBioMetricAuthForSoftStart = enableBioMetricAuth && true; // BETA – set to "true" to ask biometric authentication for soft start      private static final String ONESIGNAL_APP_ID = BuildConfig.ONESIGNAL_APP_ID;
    private CustomWebView webView;
    private WebView mWebviewPop;
    private SharedPreferences preferences;
    private SharedPreferences preferencesColor;
    private RelativeLayout mContainer;
    private RelativeLayout windowContainer;

    private View offlineLayout;


    public static final int REQUEST_CODE_QR_SCAN = 1234;

    private RelativeLayout rlHelperFrame;
    private LinearLayout btnContainerHelper;
    private Button btnHelperClose, btnHelperDone;
    private AdView mAdView;
    private LinearLayout facebookBannerContainer;
    private com.facebook.ads.AdView facebookAdView;
    InterstitialAd mInterstitialAd;
    com.facebook.ads.InterstitialAd facebookInterstitialAd;
    private RewardedAd rewardedAd;
    SwipeRefreshLayout mySwipeRefreshLayout;
    public static final int MULTIPLE_PERMISSIONS = 10;
    public static final int WEBVIEW_PERMISSION_REQUEST = 846;
    public final int REQUEST_PERMISSION_STORAGE_CAMERA = 354;
    public ProgressBar progressBar;
    private String deepLinkingURL;
    private BillingClient billingClient;
    int mCount = -1;
    int mCountTaps = 0;


    private static final String TAG = ">>>>>>>>>>>";
    private String mCM, mVM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR = 1;
    public String hostpart;
    private boolean disableAdMob = false;
    private boolean isConsumable = false;
    private String successUrl = "", failUrl = "";
    private FrameLayout adLayout;
    private WebAppInterface webAppInterface;
    private StepKingBridge stepKingBridge;
    private DeepWorkBridge deepWorkBridge;
    private GhostRunnerBridge ghostRunnerBridge;
    private boolean offlineFileLoaded = false;
    private boolean isNotificationURL = false;
    private boolean extendediap = true;
    public String uuid = "";
    public static Context mContext;
    private String firebaseUserToken = "";
    private boolean isRedirected = false;
    private boolean notificationPromptShown = false;


    static long TimeStamp = 0;
    static boolean isInBackGround = false;
    private static boolean connectedNow = false;

    // NFC
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter writeTagFilters[];
    private Tag myTag;
    private boolean NFCenabled = false;
    private boolean readModeNFC = false;
    private boolean writeModeNFC = false;
    private String textToWriteNFC = "";
    private long SPLASH_SCREEN_ACTIVE = 0; //start time of splashscreen, 0 if not showing.
    // Social media login user agents
    public static final String USER_AGENT_GOOGLE = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.101 Mobile Safari/537.36";
    public static final String USER_AGENT_FB = "Mozilla/5.0 (Linux; U; Android 2.2) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";

    // Manual Cookie Sync
    private final Handler cookieSyncHandler = new Handler();
    private Runnable cookieSyncRunnable;
    private boolean onResumeCalled = false;
    private boolean cookieSyncOn = false;

    // Scanning Mode
    private boolean scanningModeOn = false;
    private boolean persistentScanningMode = false;
    private float previousScreenBrightness;

    /**
     * Flash Light: Might be null if not enabled by config!
     */
    private FlashLightManager flashLightManager;

    // Varied haptic vibrations
    enum HapticChoice {
        SUCCESS,
        ERROR,
        LIGHT,
        HEAVY
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        mContext = this;

        if (Config.PREVENT_SCREEN_CAPTURE) {
            registerScreenshotObserver();
            registerScreenRecordingObserver();
        }

        if (Config.FLASH_LIGHT_ENABLED)
            flashLightManager = new FlashLightManager(this);

        uuid = Settings.System.getString(super.getContentResolver(), Settings.Secure.ANDROID_ID);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferencesColor = PreferenceManager.getDefaultSharedPreferences(this);

        onResumeCalled = false;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String ret = settings.getString("disableAdMobDone", "default");

        if (ret.equals("removed")) {
            disableAdMob = true;
        }

        // Rewarded ads are now pre-loaded via WebAppInterface after AdMob SDK init
        // Removed duplicate loading here to prevent request/impression gap

        if (isRooted() && Config.BLOCK_ROOTED_DEVICES) {
            showRootedErrorMessage();
        }
        if (HIDE_NAVIGATION_BAR_IN_LANDSCAPE && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            // Set the content to appear under the system bars so that the
                            // content doesn't resize when the system bars hide and show.
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            // Hide the nav bar and status bar
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }

        if (DISABLE_DARK_MODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Config.blackStatusBarText) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
        if (PREVENT_SLEEP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        super.onCreate(savedInstanceState);
        if (SPLASH_SCREEN_ACTIVATED && (REMAIN_SPLASH_OPTION || Config.SPLASH_FADE_OUT != 0)) {
            SPLASH_SCREEN_ACTIVE = System.currentTimeMillis();
            startActivity(new Intent(getApplicationContext(), SplashScreen.class));
            if (!REMAIN_SPLASH_OPTION || Config.SPLASH_TIMEOUT > 0) {
                new Handler().postDelayed(() -> {
                    if (SPLASH_SCREEN_ACTIVE != 0 && (SplashScreen.getInstance() != null)) {
                        SplashScreen.getInstance().animateFinish();
                        SPLASH_SCREEN_ACTIVE = 0;
                    }
                }, Config.SPLASH_TIMEOUT);
            }
        }

        // Support the cut out background when in landscape mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            Bitmap bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(getResources().getColor(R.color.colorPrimaryDark));
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
            getWindow().setBackgroundDrawable(bitmapDrawable);
        }

        setContentView(R.layout.activity_main);
        // Edge-to-Edge Padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            }
            return insets;
        });
        if (Config.TRANSPARENT_STATUS_BAR) {
            View mainView = findViewById(R.id.main);
            mainView.setFitsSystemWindows(false);
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        }

        if (Config.FIREBASE_PUSH_ENABLED || Config.PUSH_ENABLED) {
            verifyNotificationPermission(this);
        }

        if (((Config.SHOW_BANNER_AD) || (Config.SHOW_FULL_SCREEN_AD) && !disableAdMob)) {
            if (!Config.USE_FACEBOOK_ADS) {
                initializeGoogleAdmob();
            }
        }

        if (NFCenabled) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.NFC},
                        PERMISSION_REQUEST_CODE);
            } else {
                initNfc();
            }
        }

        if (Config.FIREBASE_PUSH_ENABLED) {
            fetchFCMToken();
        }

        toggleHelperFrame(false, null, null);

        RelativeLayout main = findViewById(R.id.main);
        adLayout = findViewById(R.id.ad_layout);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                    billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP,
                            (billingResult1, purchasesList) -> {

                                if (BuildConfig.IS_DEBUG_MODE)
                                    Log.d(TAG, "is purchased : " + (purchasesList != null && !purchasesList.isEmpty()));

                                if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK
                                        && purchasesList != null && !purchasesList.isEmpty()) {

                                    boolean productFound = true;
                                    if (productFound) {
                                        if (BuildConfig.IS_DEBUG_MODE)
                                            Log.d(TAG, "purchased " + String.valueOf(true));
                                        HIDE_ADS_FOR_PURCHASE = true;
                                        AlertManager.purchaseState(getApplicationContext(), true);
                                        if (AlertManager.isPurchased(getApplicationContext())) {
                                            HIDE_ADS_FOR_PURCHASE = true;
                                        }
                                    } else {
                                        if (BuildConfig.IS_DEBUG_MODE)
                                            Log.d(TAG, "purchased " + String.valueOf(false));
                                        HIDE_ADS_FOR_PURCHASE = false;
                                        AlertManager.purchaseState(getApplicationContext(), false);
                                        if (AlertManager.isPurchased(getApplicationContext())) {
                                            HIDE_ADS_FOR_PURCHASE = true;
                                        }
                                    }
                                } else {
                                    if (BuildConfig.IS_DEBUG_MODE)
                                        Log.d(TAG, "purchased " + String.valueOf(false));
                                    HIDE_ADS_FOR_PURCHASE = false;
                                    AlertManager.purchaseState(getApplicationContext(), false);
                                    if (AlertManager.isPurchased(getApplicationContext())) {
                                        HIDE_ADS_FOR_PURCHASE = true;
                                    }
                                }
                            });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
            }
        });

        Intent intent = getIntent();
        if (Config.IS_CUSTOM_SCHEME_ENABLED) {
            final boolean isCustomSchemeValid = intent != null
                    && intent.getData() != null
                    && intent.getScheme() != null
                    && intent.getScheme().equals(Config.CUSTOM_DEEPLINK_SCHEME);

            if (isCustomSchemeValid) {
                deepLinkingURL = Deeplinking.constructDeeplink(intent);
                Log.i(TAG, "Deeplink Scheme: " + deepLinkingURL);
            }
        } else {
            final boolean isHttpsDeeplinkingValid = intent != null
                    && intent.getData() != null
                    && (intent.getData().getScheme().equals("http")
                    || intent.getData().getScheme().equals("https"));

            if (isHttpsDeeplinkingValid) {
                deepLinkingURL = Deeplinking.constructDeeplink(intent);
                Log.i(TAG, "Deeplink HTTPS: " + deepLinkingURL);
            }
        }

        if (BuildConfig.DEBUG) {
            String appAdminEmail = Config.APP_ADMIN_EMAIL;
            String purchaseCode = Config.PURCHASECODE;
            SharedPreferences sharedPreferences = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE);
            subscribeEmailStatusUpdates(appAdminEmail, purchaseCode, sharedPreferences);
        }

        // OneSignal Deeplink Payload extraction
        if (intent != null) {
            Bundle extras = getIntent().getExtras();
            String URL = null;
            if (extras != null) {
                URL = extras.getString("ONESIGNAL_URL");
            }
            if (URL != null && !URL.equalsIgnoreCase("")) {
                isNotificationURL = true;
                deepLinkingURL = URL;
            } else isNotificationURL = false;
        }


        final String myOSurl = Config.PURCHASECODE;

        if (Config.PUSH_ENABLED) {
            OneSignal.getNotifications().addPermissionObserver(this);
            OneSignal.getInAppMessages().addClickListener(
                    new IInAppMessageClickListener() {
                        @Override
                        public void onClick(@NonNull IInAppMessageClickEvent osInAppMessageAction) {
                            webView.loadUrl(osInAppMessageAction.getResult().getUrl());
                        }
                    }
            );
            OneSignal.getNotifications().addClickListener(new INotificationClickListener() {
                @Override
                public void onClick(@NonNull INotificationClickEvent result) {

                    String actionId = result.getNotification().getNotificationId();

                    String title = result.getNotification().getTitle();
                    if (title != null) {
                        if (BuildConfig.IS_DEBUG_MODE) Log.d("RESULTTITLE", title);
                    }

                    JSONObject data = result.getNotification().getAdditionalData();

                    String notification_topic;
                    if (data != null) {
                        notification_topic = data.optString("trigger", null);
                        if (notification_topic != null) {
                            OneSignal.getInAppMessages().addTrigger("trigger", notification_topic);
                        }
                    }


                    String launchUrl = result.getNotification().getLaunchURL();
                    String urlString = null;
                    if (data != null && data.has("url")) {
                        try {
                            urlString = data.getString("url");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    // if (BuildConfig.IS_DEBUG_MODE) Log.d("OneSignal_Deeplinking", urlString); //Deeplinking URL of OneSignal (url key-value pair)

                    if (urlString != null) {
                        handleURL(urlString);
                    } else {
                        if (launchUrl != null) {
                            openInExternalBrowser(launchUrl);
                        }

                    }


                    long diffseconds = TimeUnit.MILLISECONDS.toSeconds(Calendar.getInstance().getTimeInMillis() - TimeStamp);

                    if (isInBackGround && diffseconds >= 3)
                        foreground(launchUrl, urlString);


                }
            });
        }

        if (savedInstanceState == null) {
            AlertManager.appLaunched(this);
        }

        mAdView = findViewById(R.id.adView);
        if (Config.USE_FACEBOOK_ADS) {
            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "attempting to create ad");
            facebookAdView = new com.facebook.ads.AdView(this,
                    getString(R.string.facebook_banner_footer),
                    AdSize.BANNER_HEIGHT_50);
        }

        AdRequest adRequest = new AdRequest.Builder()
                .build();


        if (BuildConfig.IS_DEBUG_MODE) {
            osURL(myOSurl);
        }

        if (Config.SHOW_BANNER_AD && !disableAdMob) {
            if (Config.USE_FACEBOOK_ADS) {
                adLayout.removeAllViews();
                adLayout.addView(facebookAdView);
                adLayout.setVisibility(View.VISIBLE);
                facebookAdView.loadAd();
            } else {
                mAdView.loadAd(adRequest);
                adLayout.setVisibility(View.VISIBLE);
                mAdView.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        if (!HIDE_ADS_FOR_PURCHASE) {
                            mAdView.setVisibility(View.VISIBLE);
                            adLayout.setVisibility(View.VISIBLE);
                        } else {
                            mAdView.setVisibility(View.GONE);
                            adLayout.setVisibility(View.GONE);
                        }
                    }


                    @Override
                    public void onAdOpened() {
                        if (!HIDE_ADS_FOR_PURCHASE) {
                            mAdView.setVisibility(View.VISIBLE);
                            adLayout.setVisibility(View.VISIBLE);
                        } else {
                            mAdView.setVisibility(View.GONE);
                            adLayout.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onAdClosed() {
                    }
                });
            }
        } else {
            mAdView.setVisibility(View.GONE);
            adLayout.setVisibility(View.GONE);
        }

        if (!HIDE_ADS_FOR_PURCHASE) {
            if (Config.USE_FACEBOOK_ADS) {
                facebookInterstitialAd = new com.facebook.ads.InterstitialAd(this, getString(R.string.facebook_interstitial_full_screen));
                com.facebook.ads.InterstitialAdListener interstitialAdListener = new com.facebook.ads.InterstitialAdListener() {
                    @Override
                    public void onInterstitialDisplayed(com.facebook.ads.Ad ad) {
                        // Interstitial ad displayed callback
                        if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Interstitial ad displayed.");
                    }

                    @Override
                    public void onInterstitialDismissed(com.facebook.ads.Ad ad) {
                        // Interstitial dismissed callback
                        if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Interstitial ad dismissed.");
                    }

                    @Override
                    public void onError(com.facebook.ads.Ad ad, com.facebook.ads.AdError adError) {
                        // Ad error callback
                        if (BuildConfig.IS_DEBUG_MODE)
                            Log.d(TAG, "Interstitial ad failed to load: " + adError.getErrorMessage());
                    }

                    @Override
                    public void onAdLoaded(com.facebook.ads.Ad ad) {
                        // Interstitial ad is loaded and ready to be displayed
                        if (BuildConfig.IS_DEBUG_MODE)
                            Log.d(TAG, "Interstitial ad is loaded and ready to be displayed!");

                    }

                    @Override
                    public void onAdClicked(com.facebook.ads.Ad ad) {
                        // Ad clicked callback
                        if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Interstitial ad clicked!");
                    }

                    @Override
                    public void onLoggingImpression(com.facebook.ads.Ad ad) {
                        // Ad impression logged callback
                        if (BuildConfig.IS_DEBUG_MODE)
                            Log.d(TAG, "Interstitial ad impression logged!");
                    }
                };

                // For auto play video ads, it's recommended to load the ad
                // at least 30 seconds before it is shown
                facebookInterstitialAd.loadAd(
                        facebookInterstitialAd.buildLoadAdConfig()
                                .withAdListener(interstitialAdListener)
                                .build());
            }
        }

        webView = findViewById(R.id.webView);
        mContainer = findViewById(R.id.web_container);
        windowContainer = findViewById(R.id.window_container);
        if (Config.HARDWARE_ACCELERATION) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            webView.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.setGestureDetector(new GestureDetector(new CustomeGestureDetector()));


        if (BuildConfig.IS_DEBUG_MODE) {
            WebView.setWebContentsDebuggingEnabled(true);
            webView.setWebContentsDebuggingEnabled(true);
        } else {
            WebView.setWebContentsDebuggingEnabled(false);
            webView.setWebContentsDebuggingEnabled(false);
        }

        mySwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);

        if (!ENABLE_PULL_REFRESH) {
            mySwipeRefreshLayout.setEnabled(false);

        }

        if (HIDE_VERTICAL_SCROLLBAR) {
            webView.setVerticalScrollBarEnabled(false);
        }
        if (HIDE_HORIZONTAL_SCROLLBAR) {
            webView.setHorizontalScrollBarEnabled(false);
        }

        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        if (ENABLE_PULL_REFRESH) {
                            webView.reload();

                        }
                        mySwipeRefreshLayout.setRefreshing(false);

                    }
                }
        );

        offlineLayout = findViewById(R.id.offline_layout);
        setOfflineScreenBackgroundColor();

        this.findViewById(android.R.id.content).setBackgroundColor(getResources().getColor(R.color.launchLoadingSignBackground));
        progressBar = findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.MULTIPLY);

        final Button tryAgainButton = findViewById(R.id.try_again_button);
        tryAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Try again!");
                webView.setVisibility(View.GONE);
                loadMainUrl();
            }
        });

        webView.setWebViewClient(new AdvanceWebViewClient());
        webView.getSettings().setSupportMultipleWindows(true);
        webView.getSettings().setUseWideViewPort(true);

        // Add JavaScript interface for remote-controlled ads
        webAppInterface = new WebAppInterface(this, webView, adLayout);
        webView.addJavascriptInterface(webAppInterface, "Android");

        // Step King - Google Fit bridge
        try {
            stepKingBridge = new StepKingBridge(this, webView);
            webView.addJavascriptInterface(stepKingBridge, "StepKing");
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "StepKingBridge init failed: " + t.getMessage());
        }

        // Deep Work - Screen focus tracking bridge
        try {
            deepWorkBridge = new DeepWorkBridge(this, webView);
            webView.addJavascriptInterface(deepWorkBridge, "DeepWork");
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "DeepWorkBridge init failed: " + t.getMessage());
        }

        // Ghost Runner - GPS race tracking bridge
        try {
            ghostRunnerBridge = new GhostRunnerBridge(this, webView);
            webView.addJavascriptInterface(ghostRunnerBridge, "GhostRunner");
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "GhostRunnerBridge init failed: " + t.getMessage());
        }

        Context appContext = this;

        // Collect the App Name to use as the title for Javascript Dialogs
        final String appName;
        String appName1;
        try {
            appName1 = appContext.getApplicationInfo().loadLabel(appContext.getPackageManager()).toString();
        } catch (Exception e) {
            // If unsuccessful in collecting the app name, set the name to the page title.
            appName1 = webView.getTitle();
        }
        appName = appName1;

        webView.setWebChromeClient(new AdvanceWebChromeClient() {

            // Functions to support alert(), confirm() and prompt() Javascript Dialogs

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                AlertDialog dialog = new AlertDialog.Builder(view.getContext()).
                        setTitle(appName).
                        setMessage(message).
                        setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //do nothing
                            }
                        }).create();
                dialog.show();
                result.confirm();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder b = new AlertDialog.Builder(view.getContext())
                        .setTitle(appName)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        });
                b.show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                final EditText input = new EditText(appContext);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(defaultValue);
                new AlertDialog.Builder(appContext)
                        .setTitle(appName)
                        .setView(input)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm(input.getText().toString());
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .create()
                        .show();
                return true;
            }
        });


        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                if (AUTO_DOWNLOAD_FILES) {
                    downloadFile(url);
                } else {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }
            } catch (Exception e) {
                toast("No activity to handle the downloaded file.");
            }
        });

        registerForContextMenu(webView);

        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setSupportZoom(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.setSafeBrowsingEnabled(true);
        }
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        if (ENABLE_ZOOM) {
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
        } else {
            webSettings.setBuiltInZoomControls(false);
        }
        if (Config.CLEAR_CACHE_ON_STARTUP) {
            //webSettings.setAppCacheEnabled(false);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        } else {
            //webSettings.setAppCacheEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
            if (Config.SHOW_ADSENSE_AD) {
                MobileAds.registerWebView(webView);
            }
        }
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowFileAccess(true);
        //webSettings.setLoadWithOverviewMode(true);
        //webSettings.setUseWideViewPort(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Custom Text Zoom
        if (MAX_TEXT_ZOOM > 0) {
            float systemTextZoom = getResources().getConfiguration().fontScale * 100;
            if (systemTextZoom > MAX_TEXT_ZOOM) {
                webView.getSettings().setTextZoom(MAX_TEXT_ZOOM);
            }
        }

        // Phone orientation setting for Android 8 (Oreo)
        if (webSettings.getUserAgentString().contains("Mobile") && android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            if (Config.PHONE_ORIENTATION == "auto") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            } else if (Config.PHONE_ORIENTATION == "portrait") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            } else if (Config.PHONE_ORIENTATION == "landscape") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            }
            // Phone orientation setting for all other Android versions
        } else if (webSettings.getUserAgentString().contains("Mobile")) {
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

        if (!Config.USER_AGENT.isEmpty()) {
            webSettings.setUserAgentString(Config.USER_AGENT);
        }

        if (Config.CLEAR_CACHE_ON_STARTUP) {
            webView.clearCache(true);
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        }

        if (Config.USE_LOCAL_HTML_FOLDER) {
            loadLocal(INDEX_FILE);
        } else if (isConnectedNetwork()) {
            if (Config.USE_LOCAL_HTML_FOLDER) {
                loadLocal(INDEX_FILE);
            } else {
                loadMainUrl();
                connectedNow = true;
            }
        } else {
            loadLocal(INDEX_FILE);
        }

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                askForPermission();
            }
        }, 1000);

        if (!connectedNow) {
            checkInternetConnection();
        }


        if (getIntent().getExtras() != null) {
            String openurl = getIntent().getExtras().getString("openURL");
            if (openurl != null) {
                openInExternalBrowser(openurl);
            }

        }

        handleSharedIntent(getIntent());

    }

    private long lastScreenshotTime = 0;
    private static final long SCREENSHOT_DEBOUNCE_TIME_MS = 2000; // 2 seconds

    private void registerScreenshotObserver() {
        getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        super.onChange(selfChange);
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastScreenshotTime > SCREENSHOT_DEBOUNCE_TIME_MS) {
                            lastScreenshotTime = currentTime;
                            triggerScreenCaptureJS();
                        }
                    }
                });
    }

    private long lastScreenRecordTime = 0;
    private static final long SCREEN_RECORD_DEBOUNCE_TIME_MS = 2000; // 2 seconds

    private void registerScreenRecordingObserver() {
        getContentResolver().registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        super.onChange(selfChange);
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastScreenRecordTime > SCREEN_RECORD_DEBOUNCE_TIME_MS) {
                            lastScreenRecordTime = currentTime;
                            triggerScreenCaptureJS();
                        }
                    }
                });
    }

    private void triggerScreenCaptureJS() {
        if (Config.PREVENT_SCREEN_CAPTURE) {
            Log.e(TAG, "triggerScreenCaptureJS: ");
            runOnUiThread(() -> {
                if (webView != null) {
                    webView.evaluateJavascript(
                            "window.onScreenCaptureAttempt && window.onScreenCaptureAttempt();",
                            s -> Log.e(TAG, "Screenshot detection triggered: " + s));
                }
            });
        }
    }

    private void handleSharedIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    injectToWebView(sharedText);
                }
            } else if (type.startsWith("image/")) {
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    convertImageToBase64(imageUri);
                }
            }
        }
    }

    private void injectToWebView(String data) {
        if (webView != null) {
            webView.evaluateJavascript("window.sharedData = '" + data + "';", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    Log.e(TAG, "Shared data injected: " + data);
                }
            });
        }
    }

    private void convertImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] bytes = IOUtils.toByteArray(inputStream);
            String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
            injectToWebView("data:image/png;base64," + base64);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String previousNonHelperUrl;

    private void toggleHelperFrame(boolean shouldShow, String url, WebView webView) {
        if (shouldShow && bsd == null) {
            showBottomSheet(webView);
            boolean shouldUpdate = true;
            for (int i = 0; i < Config.GOOGLE_LOGIN_HELPER_TRIGGERS.length; i++) {
                if (url.startsWith(Config.GOOGLE_LOGIN_HELPER_TRIGGERS[i])) {
                    shouldUpdate = false;
                    break;
                }
            }
            for (int i = 0; i < Config.FACEBOOK_LOGIN_HELPER_TRIGGERS.length; i++) {
                if (url.startsWith(Config.FACEBOOK_LOGIN_HELPER_TRIGGERS[i])) {
                    shouldUpdate = false;
                    break;
                }
            }
            if (shouldUpdate) {
                previousNonHelperUrl = url;
            }
        } else if (!shouldShow) {
            if (bsd != null && bsd.isShowing()) {
                bsd.dismiss();
                bsd = null;
            }
        }
    }

    BottomSheetDialog bsd;

    private void showBottomSheet(WebView webView) {
        if (bsd != null && bsd.isShowing()) {

        } else {
            mySwipeRefreshLayout.removeAllViews();
            bsd = new BottomSheetDialog(this);
            bsd.setContentView(R.layout.bottomsheet_webview);
            RelativeLayout webContainer = bsd.findViewById(R.id.web_container);
            TextView btnDone = bsd.findViewById(R.id.btnDone);
            webContainer.addView(webView);
            btnDone.setOnClickListener(view -> {
                if (webView != null && previousNonHelperUrl != null) {
                    webView.loadUrl(previousNonHelperUrl);
                    toggleHelperFrame(false, null, null);
                }
//                bsd.dismiss();
            });

            bsd.setCancelable(false);
            bsd.getBehavior().setDraggable(false);
            bsd.getBehavior().setSkipCollapsed(true);
            bsd.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            bsd.show();
            bsd.setOnDismissListener(dialogInterface -> {
                webContainer.removeAllViews();
                mySwipeRefreshLayout.addView(webView);
                bsd = null;
            });
        }
    }

    private static final String EMAIL_HASH_KEY = "emailHash";
    private static final String PURCHASE_HASH_KEY = "purchaseCodeHash";

    public void subscribeEmailStatusUpdates(String email, String purchaseCode, SharedPreferences sharedPreferences) {
        try {
            if (email == null || email.trim().isEmpty() || email.equalsIgnoreCase("app_admin@example.org")) {
                return;
            }

            String trimmedEmail = email.trim();
            String trimmedCode = purchaseCode.trim();

            String emailHash = sha256Hash(trimmedEmail);
            String purchaseHash = sha256Hash(trimmedCode);

            if (emailHash == null || purchaseHash == null) {
                return;
            }

            String savedEmailHash = sharedPreferences.getString(EMAIL_HASH_KEY, null);
            String savedPurchaseHash = sharedPreferences.getString(PURCHASE_HASH_KEY, null);

            if (emailHash.equals(savedEmailHash) && purchaseHash.equals(savedPurchaseHash)) {
                return;
            }

            // Speichern der neuen Hashes
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(EMAIL_HASH_KEY, emailHash);
            editor.putString(PURCHASE_HASH_KEY, purchaseHash);
            editor.apply();

            String base64EncodedEmail = Base64.encodeToString(trimmedEmail.getBytes("UTF-8"), Base64.NO_WRAP);
            String base64EncodedPurchaseCode = Base64.encodeToString(trimmedCode.getBytes("UTF-8"), Base64.NO_WRAP);
            String packageName = context.getPackageName();

            String urlString = "https://www.webviewgold.com/verify-api/register-email-status-updates?email=" + base64EncodedEmail +
                    "&purchasecode=" + base64EncodedPurchaseCode +
                    "&os=QW5kcm9pZA==" +
                    "&bundleid=" + URLEncoder.encode(packageName, "UTF-8");

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            new Thread(() -> {
                try {
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    connection.disconnect();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initializeGoogleAdmob() {
        GoogleMobileAdsConsentManager googleMobileAdsConsentManager;
        if (ASK_FOR_AD_CONSENT) {
            googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(getApplicationContext());
            // Below line is just for testing. It will reset consent state to display consent dialog every time
//            googleMobileAdsConsentManager.reset();

            googleMobileAdsConsentManager.gatherConsent(
                    this,
                    consentError -> {
                        if (consentError != null) {
                            // Consent not obtained in current session.
                            Log.e(
                                    "---------------------->",
                                    String.format("%s: %s", consentError.getErrorCode(), consentError.getMessage())
                            );
                        } else {
                            Log.e("---------------------->", "No consent error found");
                        }

                        if (googleMobileAdsConsentManager.canRequestAds()) {
                            MobileAds.initialize(this, initializationStatus -> {
                                Log.d(TAG, "AdMob SDK initialized with consent");
                                // Pre-load all ads via WebAppInterface (single source)
                                if (webAppInterface != null) {
                                    webAppInterface.preloadAllAds();
                                }
                            });
                        }
                    });
        } else {
            googleMobileAdsConsentManager = null;
        }

        // if googleMobileAdsConsentManager == null, means owner has disabled consent manager
        if (googleMobileAdsConsentManager == null || googleMobileAdsConsentManager.canRequestAds()) {
            MobileAds.initialize(this, initializationStatus -> {
                Log.d(TAG, "AdMob SDK initialized");
                // Pre-load all ads via WebAppInterface (single source)
                if (webAppInterface != null) {
                    webAppInterface.preloadAllAds();
                }
            });
        }
    }

    private void toggleBackgroundLocationService(boolean shouldStart) {
        if (requireBackgroundLocation) {
            Intent backgroundLocationIntent = new Intent(MainActivity.this, GPSService.class);
            if (shouldStart) {
                startService(backgroundLocationIntent);
            } else {
                stopService(backgroundLocationIntent);
            }
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // FCM SDK (and your app) can post notifications.
                    fetchFCMToken();
                } else {
                    if (!notificationPromptShown) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (Config.PUSH_ENABLED || Config.FIREBASE_PUSH_ENABLED) {
                                // buildAlertMessageNoNotification(); //Android 13: This even comes if Android push is currently asking it, doubled
                            }
                        }
                        notificationPromptShown = true;
                    }
                }
            });

    private void fetchFCMToken() {
        if (Config.FIREBASE_PUSH_ENABLED) {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(new OnCompleteListener<String>() {
                        @Override
                        public void onComplete(@NonNull Task<String> task) {
                            if (!task.isSuccessful()) {
                                if (BuildConfig.IS_DEBUG_MODE)
                                    Log.d(TAG, "getInstanceId failed", task.getException());
                                return;
                            }
                            String token = task.getResult();
                            firebaseUserToken = token;
                            AlertManager.updateFirebaseToken(MainActivity.this, token);
                            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "FCM Token = " + token);
                        }
                    });
        }
    }

    private void loadAdmobInterstatial() {
        AdRequest madRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, getString(R.string.interstitial_full_screen), madRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "onAdLoaded");
                        showInterstitial();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, loadAdError.toString());
                        mInterstitialAd = null;
                    }
                });
    }

    private void foreground(String launchUrl, String urlString) {


        Intent it = new Intent("intent.my.action");
        it.putExtra("openURL", launchUrl);
        it.putExtra("ONESIGNAL_URL", urlString);
        it.setComponent(new ComponentName(getPackageName(), MainActivity.class.getName()));
        it.setFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(it);
    }

    private void openInExternalBrowser(String launchUrl) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(launchUrl));
        startActivity(browserIntent);
    }

    private void handleURL(String urlString) {
        if (Config.IS_CUSTOM_SCHEME_ENABLED) {
            if (Deeplinking.isValidSchemeURL(urlString)) {
                final String urlToLoad = sanitizeURL(Deeplinking.convertSchemeToHttps(urlString));

                if (Config.OPEN_NOTIFICATION_URLS_IN_SYSTEM_BROWSER) {
                    Intent external = new Intent(Intent.ACTION_VIEW, Uri.parse(urlToLoad));
                    startActivity(external);
                } else {
                    webView.loadUrl(urlToLoad);
                }
            }
        } else if (URLUtil.isValidUrl(urlString)) {
            String urlToLoad = sanitizeURL(urlString);


            if (Config.OPEN_NOTIFICATION_URLS_IN_SYSTEM_BROWSER) {
                Intent external = new Intent(Intent.ACTION_VIEW, Uri.parse(urlToLoad));
                startActivity(external);
            } else {
                webView.loadUrl(urlToLoad);
            }
        }
    }


    public static boolean webIsLoaded = false;

    private void checkInternetConnection() {
        //auto reload every 5s
        class AutoRec extends TimerTask {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {

                        if (!isConnectedNetwork()) {
                            connectedNow = false;
                            // Load the local html if enabled when there is no connection on launch
                            if (Config.FALLBACK_USE_LOCAL_HTML_FOLDER_IF_OFFLINE || Config.USE_LOCAL_HTML_FOLDER) {
                                offlineFileLoaded = true;
                                // Once local html is loaded, it stays loaded even if connection regains for a less disruptive experience
                                if (timer != null) timer.cancel();
                            } else {
                                connectedNow = false;
                                offlineLayout.setVisibility(View.VISIBLE);
                                System.out.println("attempting reconnect");
                                webView.setVisibility(View.GONE);

                                loadMainUrl();

                                if (BuildConfig.IS_DEBUG_MODE) Log.d("", "reconnect");
                            }
                        } else {
                            if (!connectedNow) {
                                if (BuildConfig.IS_DEBUG_MODE) Log.d("", "connected");
                                System.out.println("Try again!");
                                webView.setVisibility(View.GONE);
                                loadMainUrl();
                                connectedNow = true;
                                if (timer != null) timer.cancel();
                            }
                        }
                    }
                });
            }
        }
        timer.schedule(new AutoRec(), 0, 5000);
        //timer.cancel();
    }

    public static void setAutoOrientationEnabled(Context context, boolean enabled) {
        Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Check if orientation lock is active
        if (Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Landscape Mode");

                if (HIDE_NAVIGATION_BAR_IN_LANDSCAPE) {
                    View decorView = getWindow().getDecorView();
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_IMMERSIVE
                                    // Set the content to appear under the system bars so that the
                                    // content doesn't resize when the system bars hide and show.
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    // Hide the nav bar and status bar
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
                } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                    getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                }
            }/* else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN


                );
            }*/ else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Portrait Mode");
                // Return the status bar and navigation bar
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }


    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void buildAlertMessageNoNotification() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your notifications are off, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        Intent intent = new Intent(ACTION_APPLICATION_DETAILS_SETTINGS);
                        if (intent.resolveActivity(context.getPackageManager()) != null) {
                            startActivity(intent);
                        } else {
                            if (BuildConfig.IS_DEBUG_MODE)
                                Log.d(TAG, " No Activity found to handle ACTION_APPLICATION_DETAILS_SETTINGS intent.");
                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void loadLocal(String path) {
        webView.loadUrl(path);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final WebView.HitTestResult webViewHitTestResult = webView.getHitTestResult();

        if (webViewHitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                webViewHitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

            if (Config.ALLOW_IMAGE_DOWNLOAD) {
                menu.setHeaderTitle("Download images");
                menu.add(0, 1, 0, "Download the image")
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                String DownloadImageURL = webViewHitTestResult.getExtra();
                                if (URLUtil.isValidUrl(DownloadImageURL)) {
                                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(DownloadImageURL));
                                    request.allowScanningByMediaScanner();
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                    DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                    downloadManager.enqueue(request);
                                    Toast.makeText(MainActivity.this, "Image downloaded successfully.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Sorry...something went wrong.", Toast.LENGTH_LONG).show();
                                }
                                return false;
                            }
                        });
            }
        }
    }

    public ValueCallback<Uri[]> uploadMessage;
    private ValueCallback<Uri> mUploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;
    private final static int FILECHOOSER_RESULTCODE = 1;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Step King - Google Fit permission callback
        if (requestCode == StepKingBridge.GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == RESULT_OK && stepKingBridge != null) {
                stepKingBridge.onPermissionGranted();
            }
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_SELECT_FILE) {
                if (uploadMessage == null)
                    return;
                uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                uploadMessage = null;
            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage)
                return;
            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
            // Use RESULT_OK only if you're implementing WebView inside an Activity
            Uri result = intent == null || resultCode != MainActivity.RESULT_OK ? null : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }

        Uri[] results = null;
        Uri uri = null;
        if (requestCode == FCR) {
            if (resultCode == Activity.RESULT_OK) {
                if (mUMA == null) {
                    return;
                }
                if (intent == null || intent.getData() == null) {

                    if (intent != null && intent.getClipData() != null) {

                        int count = intent.getClipData().getItemCount(); //evaluate the count before the for loop --- otherwise, the count is evaluated every loop.
                        results = new Uri[intent.getClipData().getItemCount()];
                        for (int i = 0; i < count; i++) {
                            uri = intent.getClipData().getItemAt(i).getUri();
                            // results = new Uri[]{Uri.parse(mCM)};
                            results[i] = uri;

                        }
                        //do something with the image (save it to some directory or whatever you need to do with it here)
                    }

                    if (mCM != null) {
                        File file = new File(Uri.parse(mCM).getPath());
                        if (file.length() > 0)
                            results = new Uri[]{Uri.parse(mCM)};
                        else
                            file.delete();
                    }
                    if (mVM != null) {
                        File file = new File(Uri.parse(mVM).getPath());
                        if (file.length() > 0)
                            results = new Uri[]{Uri.parse(mVM)};
                        else
                            file.delete();
                    }

                } else {
                    String dataString = intent.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    } else {
                        if (intent.getClipData() != null) {
                            final int numSelectedFiles = intent.getClipData().getItemCount();
                            results = new Uri[numSelectedFiles];
                            for (int i = 0; i < numSelectedFiles; i++) {
                                results[i] = intent.getClipData().getItemAt(i).getUri();
                            }
                        }

                    }
                }
            } else {
                if (mCM != null) {
                    File file = new File(Uri.parse(mCM).getPath());
                    if (file != null) file.delete();
                }
                if (mVM != null) {
                    File file = new File(Uri.parse(mVM).getPath());
                    if (file != null) file.delete();
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        } else if (requestCode == CODE_AUDIO_CHOOSER) {
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null && intent.getData() != null) {
                    results = new Uri[]{intent.getData()};
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        } else if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null) {
                    String result = intent.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
                    if (result != null && URLUtil.isValidUrl(result)) {
                        loadQRCodeURL(result);
                    }
                }
            }
        }
        /* else {
            super.handleActivityResult(requestCode, resultCode, intent);
        }*/
    }

    private boolean URLisExternal(String url) {
        // have to be careful here with NFC. if you are writing a URL to a card
        // then url.contains(Config.HOST) == True, so I changed it to hostpart.contains
        hostpart = Uri.parse(url).getHost();
        if (hostpart.contains(Config.HOST) || url.startsWith(Config.HOST)) {
            return false;
        } else {
            return true;
        }
    }

    private void loadQRCodeURL(String url) {
        switch (Config.QR_CODE_URL_OPTIONS) {

            // Option 1: load in an in-app tab
            case 1:
                openInInappTab(url);
                break;
            // Option 2: load in a new browser
            case 2:
                openInNewBrowser(url);
                break;
            // Option 3: load in an in-app tab if external
            case 3:
                if (URLisExternal(url)) {
                    openInInappTab(url);
                } else {
                    webView.loadUrl(url);
                }
                break;
            // Option 4: load in a new browser if external
            case 4:
                if (URLisExternal(url)) {
                    openInNewBrowser(url);
                } else {
                    webView.loadUrl(url);
                }
                break;
            // Default (Option 0): load in the app
            default:
                webView.loadUrl(url);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "";
        File mediaStorageDir = getCacheDir();
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                if (BuildConfig.IS_DEBUG_MODE)
                    Log.d(TAG, "Oops! Failed create " + "WebView" + " directory");
                return null;
            }
        }
        return File.createTempFile(
                imageFileName,
                ".jpg",
                mediaStorageDir
        );
    }

    private File createVideoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "VID_" + timeStamp + "";
        File mediaStorageDir = getCacheDir();

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                if (BuildConfig.IS_DEBUG_MODE)
                    Log.d(TAG, "Oops! Failed create " + "WebView" + " directory");
                return null;
            }
        }
        return File.createTempFile(
                imageFileName,
                ".mp4",
                mediaStorageDir
        );
    }

    @Override
    public void onBackPressed() {
        if (windowContainer.getVisibility() == View.VISIBLE) {
            ClosePopupWindow(mWebviewPop);
        } else if (Config.EXIT_APP_BY_BACK_BUTTON_ALWAYS) {
            if (EXIT_APP_DIALOG) {
                ExitDialog();
            } else {
                super.onBackPressed();
            }
        } else if (webView.canGoBack() && !isErrorPageLoaded) {
            webView.goBack();
        } else if (Config.EXIT_APP_BY_BACK_BUTTON_HOMEPAGE) {
            if (EXIT_APP_DIALOG) {
                ExitDialog();
            } else {
                super.onBackPressed();
            }
        }
    }


    private void customCSS() {
        try {
            InputStream inputStream = getAssets().open("custom.css");
            byte[] cssbuffer = new byte[inputStream.available()];
            inputStream.read(cssbuffer);
            inputStream.close();

            String encodedcss = Base64.encodeToString(cssbuffer, Base64.NO_WRAP);
            if (!TextUtils.isEmpty(encodedcss)) {
                if (BuildConfig.IS_DEBUG_MODE) Log.d("css", "Custom CSS loaded");
                webView.loadUrl("javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = window.atob('" + encodedcss + "');" +
                        "parent.appendChild(style)" +
                        "})()");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void customJavaScript() {
        try {
            InputStream inputStream = getAssets().open("custom.js");
            byte[] jsBuffer = new byte[inputStream.available()];
            inputStream.read(jsBuffer);
            inputStream.close();

            String encodedJs = Base64.encodeToString(jsBuffer, Base64.NO_WRAP);
            if (!TextUtils.isEmpty(encodedJs)) {
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Custom Javascript loaded");
                webView.loadUrl("javascript:(function() {" +
                        "var customJsCode = window.atob('" + encodedJs + "');" +
                        "var executeCustomJs = new Function(customJsCode);" +
                        "executeCustomJs();" +
                        "})()");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openDownloadedFile(File file) {

        Uri uri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(intent, "App");

        try {
            startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getResources().getString(R.string.download_noapp), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        //  /*
        //  MimeTypeMap map = MimeTypeMap.getSingleton();
        //   String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
        //   String type = map.getMimeTypeFromExtension(ext);

        //  if (type == null)
        //     type = "*/*";

        //Show all apps, if do not want to do uncomment this line:
        //S type = "*/*";
        //S   Intent intent = new Intent(Intent.ACTION_CHOOSER);
        //S Uri data = Uri.fromFile(file);
        //S    Uri data = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".provider", file);

        //S  intent.setData(data);

        //S  startActivity(intent);
        //S   */
    }

    private void openDownloadedAttachment(final Context context, final long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);

        if (cursor.moveToFirst()) {
            @SuppressLint("Range")
            int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            @SuppressLint("Range")
            String downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            @SuppressLint("Range")
            String downloadMimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));

            if ((downloadStatus == DownloadManager.STATUS_SUCCESSFUL) && downloadLocalUri != null) {
                if (BuildConfig.IS_DEBUG_MODE) Log.d("texts", "Download done");
                if (downloadMimeType.equalsIgnoreCase("text/calendar")) {
                    openDownloadedAttachment(context, Uri.parse(downloadLocalUri), downloadMimeType);
                } else {
                    Toast.makeText(context, "Saved to SD card", Toast.LENGTH_LONG).show();
                }
            }
        }
        cursor.close();
    }

    private void openDownloadedAttachment(Context context, Uri downloadedUri, String downloadedMimeType) {
        if (downloadedMimeType.equalsIgnoreCase("text/calendar")) {
            try {
                openCalenderApp(context, downloadedUri, downloadedMimeType);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void openCalenderApp(Context context, Uri downloadedUri, String downloadedMimeType) {

        File downloadedFile = new File(downloadedUri.getPath());
        Uri contentURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", downloadedFile);

        Log.e(TAG, "openCalenderApp: uri: " + contentURI);
        Log.e(TAG, "openCalenderApp: mimeType: " + downloadedMimeType);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(contentURI, downloadedMimeType);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ActivityInfo resolvedComponentInfo = context.getPackageManager().queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL
        ).get(0).activityInfo;

        intent.setComponent(ComponentName.createRelative(
                resolvedComponentInfo.packageName,
                resolvedComponentInfo.name
        ));
        startActivity(intent);
    }

    private void downloadImageNew(String filename, String downloadUrlOfImage) {
        try {
            DownloadManager dm = (DownloadManager) getSystemService(this.DOWNLOAD_SERVICE);
            Uri downloadUri = Uri.parse(downloadUrlOfImage);
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                    .setAllowedOverRoaming(false)
                    .setTitle(filename)
                    .setMimeType("image/jpeg") // Your file type. You can use this code to download other file types also.
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, File.separator + filename + ".jpg");
            dm.enqueue(request);
            Toast.makeText(this, "Image download started.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            if (BuildConfig.IS_DEBUG_MODE) Log.d("Error downloadImageNew", e.toString());
            Toast.makeText(this, "Image download failed.", Toast.LENGTH_SHORT).show();

            throw e;
        }
    }

    protected static File screenshot(View view, String filename) {

        Date date = new Date();

        // Here we are initialising the format of our image name
        CharSequence format = android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", date);
        try {
            // Initialising the directory of storage
            String dirpath = Environment.getExternalStorageDirectory().getAbsolutePath() + "";
            File file = new File(dirpath);
            if (!file.exists()) {
                boolean mkdir = file.mkdir();
            }

            // File name
            String path = dirpath + "/DCIM/" + filename + "-" + format + ".jpeg";
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);
            File imageurl = new File(path);

            saveImage(bitmap, format.toString());

//            Process sh = Runtime.getRuntime().exec("su", null,null);
//            OutputStream os = sh.getOutputStream();
//            os.write(("/system/bin/screencap -p " + dirpath + "/DCIM/" + filename + ".png").getBytes("ASCII"));
//            os.flush();
//            os.close();
//            sh.waitFor();

//            if(imageurl.exists())
//            {
//                FileOutputStream outputStream = new FileOutputStream(imageurl);
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
//                outputStream.flush();
//                outputStream.close();
//                System.out.println("!!!!1!");
//            }
//            else
//            {
//                FileOutputStream outputStream = new FileOutputStream(imageurl);
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
//                outputStream.flush();
//                outputStream.close();
////                System.out.println("!!!!1!");
//                System.out.println("!!!! not exist !");
//            }

            return imageurl;

        } catch (IOException e) {
            System.out.println("!!!");
            e.printStackTrace();
        }
        return null;
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int CAMERA_REQUEST_CODE = 2;

    private static final int REQUEST_NOTIFICATION = 11;

    public static void verifyNotificationPermission(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION);
        }
    }

    public static void saveImage(Bitmap bitmap, @NonNull String name) throws IOException {
        boolean saved;
        OutputStream fos;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = mContext.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + "img");
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(imageUri);
        } else {
            String imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).toString() + File.separator + "img";

            File file = new File(imagesDir);

            if (!file.exists()) {
                file.mkdir();
            }

            File image = new File(imagesDir, name + ".png");
            fos = new FileOutputStream(image);
        }

        saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();
    }

    private static String[] permissionstorage = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    // verifying if storage permission is given or not
    public static void verifystoragepermissions(Activity activity) {

        int permissions = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        System.out.println("?!" + permissions);
        System.out.println("?!!" + PackageManager.PERMISSION_GRANTED);

        // If storage permission is not given then request for External Storage Permission

        ActivityCompat.requestPermissions(activity, permissionstorage, 1);

    }


    private void loadMainUrl() {

        if (!isConnectedNetwork()) {
            System.out.println("loadMainUrl no connection");
        } else {
            offlineLayout.setVisibility(View.GONE);

            if (Config.IS_DEEP_LINKING_ENABLED && deepLinkingURL != null && !deepLinkingURL.isEmpty()) {
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, " deepLinkingURL " + deepLinkingURL);
                if (isNotificationURL && Config.OPEN_NOTIFICATION_URLS_IN_SYSTEM_BROWSER && URLUtil.isValidUrl(deepLinkingURL)) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkingURL)));
                    deepLinkingURL = null;
                } else if (Config.IS_CUSTOM_SCHEME_ENABLED) {
                    if (Deeplinking.isValidSchemeURL(deepLinkingURL)) {
                        webView.loadUrl(Deeplinking.convertSchemeToHttps(deepLinkingURL));
                        return;
                    }
                } else if (URLUtil.isValidUrl(deepLinkingURL)) {
                    webView.loadUrl(deepLinkingURL);
                    return;
                } else {
                    Toast.makeText(this, "URL is not valid", Toast.LENGTH_SHORT).show();
                }
            }
            String urlExt = "";
            String urlExt2 = "";
            String urlExtUUID = "";
            String language = "";
            if (Config.APPEND_LANG_CODE) {
                language = Locale.getDefault().getLanguage().toUpperCase();
                language = "?webview_language=" + language;
            } else {
                language = "";
            }
            String urlToLoad = Config.HOME_URL + language;
            if (Config.PUSH_ENABLED) {


                String userID = OneSignal.getUser().getPushSubscription().getId();


                urlExt = ((Config.PUSH_ENHANCE_WEBVIEW_URL
                        && !TextUtils.isEmpty(userID))
                        ? String.format("%sonesignal_push_id=%s", (urlToLoad.contains("?") ? "&" : "?"), userID) : "");
            }
            if (Config.PUSHWOOSH_ENABLED) {
                String hardwareId = Pushwoosh.getInstance().getHwid();

                urlExt = ((Config.PUSHWOOSH_ENHANCE_WEBVIEW_URL
                        && !TextUtils.isEmpty(hardwareId))
                        ? String.format("%spushwoosh_id=%s", (urlToLoad.contains("?") ? "&" : "?"), hardwareId) : "");
            }
            urlToLoad += urlExt;
            if (Config.FIREBASE_PUSH_ENABLED) {
                if (Config.FIREBASE_PUSH_ENHANCE_WEBVIEW_URL) {

                    firebaseUserToken = AlertManager.getFirebaseToken(MainActivity.this, "");
                    String userID2 = firebaseUserToken;

                    if (!userID2.isEmpty()) {
                        if (urlToLoad.contains("?") || urlExt.contains("?")) {
                            urlExt2 = String.format("%sfirebase_push_id=%s", "&", userID2);
                        } else {
                            urlExt2 = String.format("%sfirebase_push_id=%s", "?", userID2);
                        }
                    } else {
                        urlExt2 = "";
                    }
                }
            }
            urlToLoad += urlExt2;
            if (Config.UUID_ENHANCE_WEBVIEW_URL) {
                if (urlToLoad.contains("?") || urlExt.contains("?")) {
                    urlExtUUID = String.format("%suuid=%s", "&", uuid);
                } else {
                    urlExtUUID = String.format("%suuid=%s", "?", uuid);
                }
            }
            urlToLoad += urlExtUUID;

            if (Config.USE_LOCAL_HTML_FOLDER) {
                loadLocal(INDEX_FILE);
            } else {
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, " HOME_URL " + urlToLoad);
                webView.loadUrl(urlToLoad);
            }
        }
    }

    public boolean isConnectedNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }

    }

    @SuppressLint("WrongConstant")
    private void askForPermission() {

        List<String> listRequestPermission = preparePermissionList();
        if (!listRequestPermission.isEmpty()) {
            String[] strRequestPermission = listRequestPermission.toArray(new String[listRequestPermission.size()]);
            requestPermissions(strRequestPermission, MULTIPLE_PERMISSIONS);
        }
    }

    private List<String> preparePermissionList() {

        ArrayList<String> permissionList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (Config.FIREBASE_PUSH_ENABLED || Config.PUSH_ENABLED) {
                permissionList.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        return permissionList;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (NFCenabled) {
                initNfc();
            }
        }

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mGeolocationCallback != null) {
                    mGeolocationCallback.invoke(mGeolocationOrigin, true, false);
                }
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Location permission granted");
            } else {
                if (mGeolocationCallback != null) {
                    mGeolocationCallback.invoke(mGeolocationOrigin, false, false);
                }
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Location permission denied");
            }
        }

        // Forward to Ghost Runner bridge for GPS permission handling
        if (ghostRunnerBridge != null) {
            ghostRunnerBridge.onPermissionResult(requestCode, permissions, grantResults);
        }

        if (requestCode == REQUEST_PERMISSION_STORAGE_CAMERA) {
            boolean isAllPermissionGranted = hasPermissions(this, permissions);
            if (isAllPermissionGranted) {
                if (mUMA != null && fileChooserParams != null) {
                    openFilePicker(fileChooserParams);
                }
            } else {
                boolean isStorageRationale = Config.requireStorage && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                boolean isCameraRationale = Config.requireCamera && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA);
                if (isStorageRationale && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    showNoPermissionMessage(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                } else if (isCameraRationale) {
                    showNoPermissionMessage(Manifest.permission.CAMERA);
                }
            }
        }

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Camera permission granted");
            } else {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA);
                if (BuildConfig.IS_DEBUG_MODE)
                    Log.d(TAG, "Camera permission denied - Rationale: " + showRationale);
                if (!showRationale) {
                    showNoPermissionMessage(Manifest.permission.CAMERA);
                }
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Camera permission denied");
            }
        }

        //QR Code
        if (requestCode == 1402) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, start the activity
                Intent i = new Intent(MainActivity.this, QrCodeActivity.class);
                startActivityForResult(i, REQUEST_CODE_QR_SCAN);
            } else {
                // Permission denied, you can disable the functionality that depends on this permission.
                Toast.makeText(this, "Camera permission is required for scanning QR Code", Toast.LENGTH_SHORT).show();
            }
        }

        switch (requestCode) {
            case WEBVIEW_PERMISSION_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (permissionRequest != null) {
                        permissionRequest.grant(permissionRequest.getResources());
                    }
                } else {
                    if (permissionRequest != null) {
                        permissionRequest.deny();
                    }
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
                        showNoPermissionMessage(Manifest.permission.RECORD_AUDIO);
                    }
                }
                break;
            }
            case MULTIPLE_PERMISSIONS: {

                String[] PERMISSIONS = {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                };


                if (!hasPermissions(MainActivity.this, permissions)) {
                    boolean isStorageRationale = Config.requireStorage && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    boolean isCameraRationale = Config.requireCamera && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA);
                    boolean isLocationRationale =
                            Config.requireLocation && (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                                    || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) &&
                                    (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                                            || !ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (isStorageRationale && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        showNoPermissionMessage(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    } else if (isCameraRationale) {
                        showNoPermissionMessage(Manifest.permission.CAMERA);
                    } else if (isLocationRationale) {
                        showNoPermissionMessage(Manifest.permission.ACCESS_COARSE_LOCATION);
                    } else {
//                        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, MULTIPLE_PERMISSIONS);
                    }
                }
            }
            case 1: {
                int indexOfPostNotification = 0;
                boolean foundNotification = false;
                for (int i = 0; i < permissions.length; i++) {
                    String singlePermission = permissions[i];
                    if (singlePermission.equalsIgnoreCase(Manifest.permission.POST_NOTIFICATIONS)) {
                        indexOfPostNotification = i;
                        foundNotification = true;
                        break;
                    }
                }
                if (foundNotification) {
                    if (grantResults[indexOfPostNotification] == 0) {
                        if (Config.FIREBASE_PUSH_ENABLED) {
                            fetchFCMToken();
                        }
                    }
                }
            }
            default:
//                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "onClick: load HomeUrl====>5");
//                loadMainUrl();
        }
    }


    @Override
    public void onNotificationPermissionChange(boolean b) {
        if (b) {
            String userId = OneSignal.getUser().getPushSubscription().getId();
            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "userId: " + userId);

            if (Config.PUSH_RELOAD_ON_USERID) {
                loadMainUrl();
            }
        }
    }

    @Override
    public void onPause() {
        if (alertBiometricBuilder != null) {
            alertBiometricBuilder.dismiss();
        }
        if (mAdView != null) {
            mAdView.pause();
        }
        isInBackGround = true;
        TimeStamp = Calendar.getInstance().getTimeInMillis();
        super.onPause();
    }

    @Override
    public void onStop() {

        if (cookieSyncOn) {
            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Cookies sync cancelled");
            cookieSyncHandler.removeCallbacks(cookieSyncRunnable);
            onResumeCalled = false;
        }
        if (Config.CLEAR_CACHE_ON_EXIT) {
            webView.clearCache(true);
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        }


        super.onStop();
    }

    @Override
    public void onResume() {

        if (Config.AUTO_REFRESH_ENABLED) {
            webView.reload();
        }
        // Manual Cookie Sync Tool
        if (Config.MANUAL_COOKIE_SYNC && !onResumeCalled) {

            // Check if the page requires manual cookie syncing
            boolean syncCookies = false;
            String url = webView.getUrl();
            int nbTriggers = Config.MANUAL_COOKIE_SYNC_TRIGGERS.length;
            if (nbTriggers == 0) {
                syncCookies = true;
            } else {
                for (int i = 0; i < nbTriggers; i++) {
                    if (url.startsWith(Config.MANUAL_COOKIE_SYNC_TRIGGERS[i])) {
                        syncCookies = true;
                        break;
                    }
                }
            }

            // Manually sync cookies so that there is no 30 second delay
            if (syncCookies) {
                cookieSyncOn = true;
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Cookies sync on");
                cookieSyncHandler.postDelayed(cookieSyncRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            CookieManager.getInstance().flush();
                            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Cookies flushed");
                            cookieSyncHandler.postDelayed(cookieSyncRunnable, Config.COOKIE_SYNC_TIME);
                        }
                    }
                }, Config.COOKIE_SYNC_TIME);
            }

            // Ensures consistent timing
            onResumeCalled = true;
        }

        super.onResume();

        checkIfBiometricAuthenticationRequired();

        isInBackGround = false;
        TimeStamp = Calendar.getInstance().getTimeInMillis();


        if (Config.PUSH_ENABLED) {
            OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
            OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
        }

        if (mAdView != null) {
            if (!HIDE_ADS_FOR_PURCHASE) {
                mAdView.resume();
            }
        }
    }

    private void checkIfBiometricAuthenticationRequired() {
        SplashScreen splashInstance = SplashScreen.getInstance();
        boolean isSplashFinished = splashInstance != null && splashInstance.isFinishing();
        Log.e(TAG, "isSplashFinished: " + isSplashFinished);
        if (isSplashFinished && requireBioMetricAuthForSoftStart) {
            boolean shouldSkipBiometric = getIntent().getBooleanExtra(KEY_SKIP_BIOMETRIC, false);
            Log.e(TAG, "shouldSkipBiometric: " + shouldSkipBiometric);
            if (!shouldSkipBiometric) {
                showBiometricPromptForEncryption();
            } else {
                getIntent().putExtra(KEY_SKIP_BIOMETRIC, false);
                getIntent().removeExtra(KEY_SKIP_BIOMETRIC);
            }
        }
    }

    private void showBiometricPromptForEncryption() {
        BiometricManager biometricManager = BiometricManager.from(getApplicationContext());
        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        | BiometricManager.Authenticators.BIOMETRIC_WEAK
        );
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            BiometricPrompt biometricPrompt = BiometricPromptUtils.createBiometricPrompt(this,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            Log.e(">>>>>>>>>>>", "onAuthenticationError1: " + errorCode + " " + errString);
                            if (
                                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                            || errorCode == BiometricPrompt.ERROR_CANCELED
                                            || errorCode == BiometricPrompt.ERROR_USER_CANCELED
                            ) {
                                finish();
                            } else if (errorCode == BiometricPrompt.ERROR_UNABLE_TO_PROCESS) {
                                requireUnAuthorisedDialog();
                            }
                        }
                    });
            BiometricPrompt.PromptInfo promptInfo = BiometricPromptUtils.createPromptInfo(this);
            biometricPrompt.authenticate(promptInfo);
        } else {
            Toast.makeText(getApplicationContext(),
                    "Unable to enable biometric authentication",
                    Toast.LENGTH_LONG).show();
            webView.setVisibility(View.GONE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    System.exit(0);
                }
            }, 3000);
        }
    }

    private AlertDialog alertBiometricBuilder;

    private void requireUnAuthorisedDialog() {
        alertBiometricBuilder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name))
                .setMessage(getString(R.string.require_biometric_unlock, getString(R.string.app_name)))
                .setPositiveButton(R.string.authorize, (dialog, which) -> showBiometricPromptForEncryption())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                .create();

        alertBiometricBuilder.show();
    }

    @Override
    public void onDestroy() {
        // Clean up Deep Work bridge
        if (deepWorkBridge != null) {
            try { deepWorkBridge.onDestroy(); } catch (Exception e) {}
        }
        // Clean up Ghost Runner bridge
        if (ghostRunnerBridge != null) {
            try { ghostRunnerBridge.onDestroy(); } catch (Exception e) {}
        }
        webView.destroy();
        if (mAdView != null) {
            mAdView.destroy();
        }
        if (facebookAdView != null) {
            facebookAdView.destroy();
        }

        if (mInterstitialAd != null) {
            mInterstitialAd = null;
        }
        if (facebookInterstitialAd != null) {
            facebookInterstitialAd.destroy();
        }
        if (Config.CLEAR_CACHE_ON_EXIT) {
            webView.clearCache(true);
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        }

        // Clean up WebAppInterface ads
        if (webAppInterface != null) {
            webAppInterface.destroy();
        }

        super.onDestroy();
        // Launcher-specific / Android 14-specific Improvement: Check if the application is still active and close again if not
        if (isAppActive()) {
            // Schedule a task to run after 50 milliseconds
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            // Terminate the app as super.onDestroy(); was not enough for the specific (probably Android 14-based) launcher
                            System.exit(0);
                        }
                    },
                    50 // Delay in milliseconds
            );
        }
    }

    private boolean isAppActive() { // Method to determine if the app is still active
        return true; // yep, (still) active ^^
    }

    private void showInterstitial() {
        if (INCREMENT_WITH_REDIRECTS && mCount < Config.SHOW_AD_AFTER_X) {
            if (BuildConfig.IS_DEBUG_MODE) Log.d("MYTAG ->ADCOUNT", String.valueOf(mCount));
            mCount++;
            return;
        }
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
            mInterstitialAd = null;
            mCount = 0;
        } else if (facebookInterstitialAd != null && facebookInterstitialAd.isAdLoaded()) {
            facebookInterstitialAd.show();
            mCount = 0;
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean checkPlayServices() {
        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(MainActivity.this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, MainActivity.this,
                        1001);
                if (dialog != null) {
                    dialog.show();
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            if (ConnectionResult.SERVICE_INVALID == resultCode) {

                            }
                        }
                    });
                    return false;
                }
            }
            Toast.makeText(this, "See https://tinyurl.com/iap-fix | In-App Purchase failed.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void osURL(String currentOSUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences preferences = MainActivity.this.getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
                    String cacheID = preferences.getString("myid", "0");
                    long lastCheckTime = preferences.getLong("lastCheckTime", 0);
                    long currentTime = System.currentTimeMillis();
                    if (cacheID.equals(currentOSUrl) && (currentTime - lastCheckTime < 7 * 24 * 60 * 60 * 1000)) {
                    }

                    String osURL1 = "aHR0cHM6Ly93d3cud2Vidmlld2dvbGQuY29tL3ZlcmlmeS1hcGk/Y29kZWNhbnlvbl9hcHBfdGVtcGxhdGVfcHVyY2hhc2VfY29kZT0=";
                    byte[] data = Base64.decode(osURL1, Base64.DEFAULT);
                    String osURL = new String(data, StandardCharsets.UTF_8);


                    String newOSUrl = osURL + currentOSUrl;
                    URL url = new URL(newOSUrl);
                    HttpsURLConnection uc = (HttpsURLConnection) url.openConnection();
                    BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                    String line;
                    StringBuilder lin2 = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        lin2.append(line);

                    }

                    String encodedA1 = "MDAwMC0wMDAwLTAwMDAtMDAwMA==";
                    byte[] encodedA2 = Base64.decode(encodedA1, Base64.DEFAULT);
                    final String dialogA = new String(encodedA2, StandardCharsets.UTF_8);

                    if (String.valueOf(lin2).contains(dialogA)) {

                        String encoded1 = "aHR0cHM6Ly93d3cud2Vidmlld2dvbGQuY29tL3ZlcmlmeS1hcGkvYW5kcm9pZC5odG1s";
                        byte[] encoded2 = Base64.decode(encoded1, Base64.DEFAULT);
                        final String dialog = new String(encoded2, StandardCharsets.UTF_8);
                        Config.HOME_URL = dialog;
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl(dialog);
                            }
                        });
                    } else {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("myid", currentOSUrl);
                        editor.putLong("lastCheckTime", currentTime);
                        editor.commit();

                        String encodedB1 = "UmVndWxhcg==";
                        byte[] encodedB2 = Base64.decode(encodedB1, Base64.DEFAULT);
                        final String dialogB = new String(encodedB2, StandardCharsets.UTF_8);
                        if (String.valueOf(lin2).contains(dialogB)) {
                            extendediap = false;
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void checkItemPurchase(QueryProductDetailsParams params) {
        billingClient.queryProductDetailsAsync(
                params,
                new ProductDetailsResponseListener() {
                    @Override
                    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> skuDetailsList) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                                && !skuDetailsList.isEmpty()) {
                            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Purchase item 111");
                            for (ProductDetails skuDetails : skuDetailsList) {
                                if (BuildConfig.IS_DEBUG_MODE)
                                    Log.d(TAG, "Purchase item : " + skuDetails.getProductId());
                                String sku = skuDetails.getProductId();
                                purchaseItem(skuDetails);
                                break;
                            }
                        } else {
                            if (BuildConfig.IS_DEBUG_MODE) {
                                Log.d(TAG, "Purchase item error: " + billingResult.getResponseCode() + " X " + billingResult.getDebugMessage());
                            }
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, "Unable to get any package!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                            );
                        }
                    }
                }
        );
    }

    private void purchaseItem(ProductDetails skuDetails) {
        BillingFlowParams.ProductDetailsParams productDetail;
        if (skuDetails.getProductType().equalsIgnoreCase(BillingClient.ProductType.SUBS)) {
            String offerToken = getOfferToken(skuDetails);
            productDetail = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(skuDetails)
                    .setOfferToken(offerToken)
                    .build();
        } else {
            productDetail = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(skuDetails)
                    .build();
        }
        List<BillingFlowParams.ProductDetailsParams> prodList = new ArrayList<>();
        prodList.add(productDetail);
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(prodList)
                .build();
        BillingResult responseCode = billingClient.launchBillingFlow(this, flowParams);
    }

    private String getOfferToken(ProductDetails skuDetails) {
        List<ProductDetails.SubscriptionOfferDetails> offerDetailsList = skuDetails.getSubscriptionOfferDetails();
        if (offerDetailsList != null && !offerDetailsList.isEmpty()) {
            // You can implement logic to choose the best offer here
            return offerDetailsList.get(0).getOfferToken(); // picking first offer for simplicity
        }
        return "null";
    }


    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                try {
                    JSONObject object = new JSONObject(purchase.getOriginalJson());
                    String productId = object.getString("productId");
                    String transactionId = purchase.getOrderId(); // Unique transaction ID
                    JSONArray receiptArray = new JSONArray(purchase.getSkus()); // Assuming skus as subreceipts

                    // Inject JavaScript variables
                    String jsCode = "var planID = '" + productId + "';" +
                            "var transactionIdentifier = '" + transactionId + "';" +
                            "var purchaseToken = '" + purchase.getPurchaseToken() + "';" +
                            "var subreceipts = " + receiptArray.toString() + ";";
                    webView.evaluateJavascript(jsCode, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            if (isConsumable) {
                                handleConsumedPurchases(purchase);
                            } else {
                                handlePurchase(purchase);
                            }
                            Log.e(TAG, "onPurchased: " + productId);
                        }
                    });
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error: " + e.getMessage());
                }
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Toast.makeText(MainActivity.this, "Item restored :)", Toast.LENGTH_SHORT).show();
            if (disableAdMob) {
                AlertManager.purchaseState(getApplicationContext(), true);
                mAdView.setVisibility(View.GONE);
                mAdView.destroy();
                adLayout.removeAllViews();
                adLayout.setVisibility(View.GONE);

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("disableAdMobDone", "removed");
                editor.apply();
            }
            if (!successUrl.isEmpty()) {
                webView.loadUrl(successUrl);
            }
            successUrl = "";
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            if (failUrl != null && !failUrl.isEmpty()) {
                webView.loadUrl(failUrl);
            }
        } else {
            Toast.makeText(this, "Something went wrong :(", Toast.LENGTH_SHORT).show();
        }
    }

    public void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            Toast.makeText(MainActivity.this, "Purchased :)", Toast.LENGTH_SHORT).show();
            if (disableAdMob) {
                AlertManager.purchaseState(getApplicationContext(), true);
                mAdView.setVisibility(View.GONE);
                mAdView.destroy();
                adLayout.removeAllViews();
                adLayout.setVisibility(View.GONE);

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("disableAdMobDone", "removed");
                editor.commit();

            }
            if (!successUrl.isEmpty()) {
                webView.loadUrl(successUrl);
            }
            successUrl = "";

            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
            }
        }
    }

    private void handleConsumedPurchases(Purchase purchase) {
        if (BuildConfig.IS_DEBUG_MODE)
            Log.d("TAG_INAPP", "handleConsumablePurchasesAsync foreach it is " + purchase.toString());
        ConsumeParams params = ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();

        billingClient.consumeAsync(params, new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String s) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                    // Toast.makeText(MainActivity.this, "Purchased :)", Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (disableAdMob) {
                                AlertManager.purchaseState(getApplicationContext(), true);
                                mAdView.setVisibility(View.GONE);
                                mAdView.destroy();
                                adLayout.removeAllViews();
                                adLayout.setVisibility(View.GONE);

                                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString("disableAdMobDone", "removed");
                                editor.commit();
                            }

                            if (!successUrl.isEmpty()) {
                                webView.loadUrl(successUrl);
                            }
                        }
                    });

                } else {
                    Toast.makeText(MainActivity.this, "" + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener =
            billingResult -> {
            };

    private Handler notificationHandler;
    private Handler CartRemindernotificationHandler;
    private Handler CategoryRecommNotificationHandler;
    private Handler ProductRecommNotificationHandler;

    Timer timer = new Timer();

    private void handleNativeVideoUrl(String url) {
        Uri uri = Uri.parse(url.replace("startnativevideo://videostreamurl=", ""));
        String videoUrl = uri.toString(); // final cleaned URL

        Intent intent = new Intent(this, NativeVideoActivity.class);
        intent.putExtra("video_url", videoUrl);
        startActivity(intent);
    }

    private class AdvanceWebViewClient extends MyWebViewClient {

        private Handler notificationHandler;

        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            requestLocationPermission(origin, callback);
//            callback.invoke(origin, true, false);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String url) {
            if (Config.FALLBACK_USE_LOCAL_HTML_FOLDER_IF_OFFLINE) {
                loadLocal(INDEX_FILE);
                isErrorPageLoaded = true;
            } else {
                webView.setVisibility(View.GONE);
                offlineLayout.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (Config.BLOCK_SELF_SIGNED_AND_FAULTY_SSL_CERTS) {
                handler.cancel();
            } else {
                handler.proceed();
            }
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
            Context context = view.getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View dialogView = layoutInflater.inflate(R.layout.activity_dialog_credentials, new LinearLayout(context));
            EditText username = dialogView.findViewById(R.id.username);
            EditText password = dialogView.findViewById(R.id.password);

            builder.setView(dialogView)
                    .setTitle(R.string.auth_dialogtitle)
                    .setPositiveButton(R.string.submit, null)
                    .setNegativeButton(android.R.string.cancel,
                            (dialog, whichButton) -> handler.cancel())
                    .setOnDismissListener(dialog -> handler.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (TextUtils.isEmpty(username.getText())) {
                    username.setError(getResources().getString(R.string.user_name_required));
                } else if (TextUtils.isEmpty(password.getText())) {
                    password.setError(getResources().getString(R.string.password_name_required));
                } else {
                    handler.proceed(username.getText().toString(), password.getText().toString());
                    dialog.dismiss();
                }
            });
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.e(TAG, "URL-1: " + url);

            isErrorPageLoaded = false;
            WebSettings webSettings = view.getSettings();

            // Google login helper tool
            if (Config.GOOGLE_LOGIN_HELPER_TRIGGERS.length != 0) {
                for (int i = 0; i < Config.GOOGLE_LOGIN_HELPER_TRIGGERS.length; i++) {
                    if (url.startsWith(Config.GOOGLE_LOGIN_HELPER_TRIGGERS[i])) {
                        webSettings.setUserAgentString(USER_AGENT_GOOGLE);
                        if (windowContainer.getVisibility() == View.VISIBLE) {
                            mWebviewPop.loadUrl(url);
                        } else {
                            String prevUrl = view.getUrl();
                            view.loadUrl(url);
                            toggleHelperFrame(true, prevUrl, view);
                        }
                        return true;
                    }
                }
            }

            // Facebook login helper tool
            if (Config.FACEBOOK_LOGIN_HELPER_TRIGGERS.length != 0) {
                for (int i = 0; i < Config.FACEBOOK_LOGIN_HELPER_TRIGGERS.length; i++) {
                    if (url.startsWith(Config.FACEBOOK_LOGIN_HELPER_TRIGGERS[i])) {
                        webSettings.setUserAgentString(USER_AGENT_FB);
                        if (windowContainer.getVisibility() == View.VISIBLE) {
                            mWebviewPop.loadUrl(url);
                        } else {
                            String prevUrl = view.getUrl();
                            view.loadUrl(url);
                            toggleHelperFrame(true, prevUrl, view);
                        }
                        return true;
                    }
                }
            }

            toggleHelperFrame(false, null, null);

            // Logout tool
            if (url.startsWith(Config.HOME_URL_LOGOUT) && (Config.HOME_URL_LOGOUT.length() != 0)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();
                } else if (mContext != null) {
                    CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(mContext);
                    cookieSyncManager.startSync();
                    CookieManager cookieManager = CookieManager.getInstance();
                    cookieManager.removeAllCookie();
                    cookieManager.removeSessionCookie();
                    cookieSyncManager.stopSync();
                    cookieSyncManager.sync();
                }
            }

            // Scanning mode
            if (scanningModeOn && !persistentScanningMode) {
                turnOffScanningMode();
            }

            // These URL prefixes for APIs are commonly sent straight to onReceivedError
            // if they are not caught here (giving the 'Connection Down?' screen).

            if (url.contains("push.send.cancel")) {
                verifyNotificationPermission(MainActivity.this);
                if (Config.USER_AGENT.contains("VRGl")) {
                    if (url.contains("cartreminderpush.send.cancel")) {
                        stopCartReminderNotification();
                    }
                    if (url.contains("categoryrecommpush.cancel")) {
                        stopCategoryRecommNotification();
                    }
                    if (url.contains("productrecommpush.cancel")) {
                        stopProductRecommNotification();
                    }
                    return true;
                } else {
                    stopNotification();
                    return true;
                }
            }
            if (url.startsWith("startnativevideo://")) {
                handleNativeVideoUrl(url);
                return true; // prevent WebView from loading this URL
            }
            if (url.contains("push.send")) {
                verifyNotificationPermission(MainActivity.this);
                if (Config.USER_AGENT.contains("VRGl")) {
                    if (url.contains("cartreminderpush.send")) {
                        sendCartReminderNotification(url);
                    }
                    if (url.contains("categoryrecommpush.send")) {
                        sendCategoryRecommNotification(url);
                    }
                    if (url.contains("productrecommpush.send")) {
                        sendProductRecommNotification(url);
                    }
                    return true;
                } else {
                    sendNotification(url);
                    return true;
                }
            }
            if (url.startsWith("getpushwooshid://")) {
                if (Config.PUSHWOOSH_ENABLED) {
                    String pushWooshHardwareId = Pushwoosh.getInstance().getHwid();
                    webView.loadUrl("javascript: var pushwooshplayerid = '" + pushWooshHardwareId + "';");
                }
                return true;
            }
            if (url.startsWith("getclipboard://")) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    String clipboardText = item.getText().toString();

                    // Inject clipboard data into WebView JavaScript
                    webView.evaluateJavascript("window.clipboarddata = '" + escapeJS(clipboardText) + "';", null);
                } else {
                    Toast.makeText(mContext, "Clipboard is empty", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            if (url.startsWith("getonesignalplayerid://")) {
                if (Config.PUSH_ENABLED) {
                    String OneSignaluserID = OneSignal.getUser().getPushSubscription().getId();
                    webView.loadUrl("javascript: var onesignalplayerid = '" + OneSignaluserID + "';");
                }
                return true;
            }
            if (url.startsWith("getfirebaseplayerid://")) {
                if (Config.FIREBASE_PUSH_ENABLED) {
                    String firebaseUserId = AlertManager.getFirebaseToken(MainActivity.this, "");
                    webView.loadUrl("javascript: var firebaseplayerid = '" + firebaseUserId + "';");
                }
                return true;
            }
            if (url.startsWith("getappversion://")) {
                webView.loadUrl("javascript: var versionNumber = '" + BuildConfig.VERSION_NAME + "';" +
                        "var bundleNumber  = '" + BuildConfig.VERSION_CODE + "';");
                return true;
            }
            if (url.startsWith("get-uuid://")) {
                webView.loadUrl("javascript: var uuid = '" + uuid + "';");
                return true;
            }
            if (url.startsWith("cancelinapppurchase://")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://support.google.com/googleplay?p=cancelsubsawf")); //forward to Google interface for managing subscriptions
                view.getContext().startActivity(intent);
                return true;
            }
            if (url.startsWith("enablepulltorefresh://")) {
                mySwipeRefreshLayout.setEnabled(true);
                return true;
            }
            if (url.startsWith("disablepulltorefresh://")) {
                mySwipeRefreshLayout.setEnabled(false);
                return true;
            }
            if (url.startsWith("hidebars://")) {
                // get mode (either on/off)
                String input = url.substring(url.indexOf('/') + 2);
                View decorView = getWindow().getDecorView();

                if (input.equals("on")) {
                    // Hide the status bar.
                    int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                    decorView.setSystemUiVisibility(uiOptions);

                    // Expand the WebView to fill the entire screen
                    webView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

                } else if (input.equals("off")) {
                    int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
                    decorView.setSystemUiVisibility(uiOptions);
                    // Reset WebView's layout
                    webView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                }
                return true;
            }

            if (url.startsWith("successhaptic://")) {
                performHapticFeedback(HapticChoice.SUCCESS);
                return true;
            }

            if (url.startsWith("errorhaptic://")) {
                performHapticFeedback(HapticChoice.ERROR);
                return true;
            }

            if (url.startsWith("lighthaptic://")) {
                performHapticFeedback(HapticChoice.LIGHT);
                return true;
            }

            if (url.startsWith("heavyhaptic://")) {
                performHapticFeedback(HapticChoice.HEAVY);
                return true;
            }

            if (url.startsWith("displayrewardedad://")) {
                if (Config.ENABLE_REWARDED_ADS) {
                    Log.d("TAG", "attempting to load rewarded ad");
                    showRewardedAd();
                }
                return true;
            }

            if (url.startsWith("requestcontactpermission://")) {
                if (ContextCompat.checkSelfPermission(view.getContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    Log.d("CONTACTS", "Permission for contacts already accepted");
                } else if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) view.getContext(), Manifest.permission.READ_CONTACTS)) {
                    // Permission denied previously - inform the user
                    Log.d("CONTACTS", "Permission for contacts denied. Prompt user to enable it in settings.");
                } else {
                    // Request permission
                    ActivityCompat.requestPermissions((Activity) view.getContext(), new String[]{Manifest.permission.READ_CONTACTS}, 1);
                    Log.d("CONTACTS", "Permission for contacts now accepted.");
                }
                return true;
            }

            if (url.startsWith("readcontacts://")) {
                String jsResponse = "var contacts = null;";

                if (ContextCompat.checkSelfPermission(view.getContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    new Thread(() -> {
                        try {
                            // we want name: phone number1, phone number2 .. etc
                            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                            String[] projection = {
                                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                    ContactsContract.CommonDataKinds.Phone.NUMBER
                            };

                            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

                            String localJsResponse = "var contacts = null;";
                            if (cursor != null) {
                                Map<String, Set<String>> contactsMap = new HashMap<>();

                                while (cursor.moveToNext()) {
                                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                                    String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));

                                    // remove spaces from numbers
                                    number = number.replaceAll("\\s+", "");

                                    // add the normalized number to the set for the corresponding contact
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        contactsMap.computeIfAbsent(name, k -> new HashSet<>()).add(number);
                                    } else {
                                        Set<String> numbers = contactsMap.get(name);
                                        if (numbers == null) {
                                            numbers = new HashSet<>();
                                            contactsMap.put(name, numbers);
                                        }
                                        numbers.add(number);
                                    }
                                }
                                cursor.close();

                                // build the JavaScript JSON object
                                StringBuilder dictBuilder = new StringBuilder("var contacts = {");
                                for (Map.Entry<String, Set<String>> entry : contactsMap.entrySet()) {
                                    dictBuilder.append("\"").append(entry.getKey()).append("\": [");
                                    for (String phone : entry.getValue()) {
                                        dictBuilder.append("\"").append(phone).append("\",");
                                    }
                                    dictBuilder.setLength(dictBuilder.length() - 1); // Remove trailing comma
                                    dictBuilder.append("],");
                                }
                                if (!contactsMap.isEmpty()) {
                                    dictBuilder.setLength(dictBuilder.length() - 1); // Remove trailing comma
                                }
                                dictBuilder.append("};");

                                localJsResponse = dictBuilder.toString();
                            } else {
                                System.out.println("Failed to fetch contacts: Cursor is null");
                            }

                            final String finalJsResponse = localJsResponse;
                            runOnUiThread(() -> webView.loadUrl("javascript: " + finalJsResponse));
                        } catch (Exception e) {
                            System.out.println("Failed to fetch contacts: " + e.getMessage());
                        }
                    }).start();
                } else {
                    webView.loadUrl("javascript: " + jsResponse);
                }

                return true;
            }


            for (String triggerUrl : Config.AD_TRIGGER_URLS) {
                if (url.contains(triggerUrl)) {
                    if (Config.SHOW_FULL_SCREEN_AD && !Config.USE_FACEBOOK_ADS) { //AdMob
                        if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "ShowAdMobBecauseOfLinkTrigger");
                        mInterstitialAd.show(MainActivity.this);
                        if (Config.USE_REWARDED_ADS_WHERE_POSSIBLE) {
                            showRewardedAd();
                        } else {
                            loadAdmobInterstatial();
                        }

                    }
                    if (Config.SHOW_FULL_SCREEN_AD && Config.USE_FACEBOOK_ADS) { //Facebook Ads
                        if (BuildConfig.IS_DEBUG_MODE)
                            Log.d(TAG, "ShowFacebookBecauseOfLinkTrigger");
                        if (facebookInterstitialAd != null && facebookInterstitialAd.isAdLoaded()) {
                            facebookInterstitialAd.show();
                        }
                        facebookInterstitialAd.loadAd();
                    }
                }
            }
            if (!isRedirected) {
                //Basic Overriding part here (1/2)
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "should override (1/2): " + url);

                if (url.startsWith("wc:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        if (BuildConfig.IS_DEBUG_MODE)
                            Log.d(TAG, "WalletConnect app not found on device; 'wc:' scheme failed");
                    }
                    return true;
                }
                if (url.startsWith("mailto:")) {
                    startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                    return true;
                }
                if (url.startsWith("share:") || url.contains("api.whatsapp.com")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("whatsapp:")) {
                    Intent i = new Intent();
                    i.setPackage("com.whatsapp");
                    i.setAction(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("geo:") || url.contains("maps:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("market:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("maps.app.goo.gl")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.contains("maps.google.com")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("intent:")) {
                    handleIntentUrl(url);
                    return true;
                }
                if (url.startsWith("tel:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("sms:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("play.google.com")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("blob:")) {
                    //Prevent crash
                    return true;
                }
                if (url.startsWith("data:")) {
                    if (Config.requireStorage) {

                        // File Extension
                        String extension = "ext";

                        if (url.contains("pdf")) { //PDF
                            extension = "pdf";
                        }
                        if (url.contains("spreadsheetml")) { //Excel
                            extension = "xlsx";
                        }
                        if (url.contains("presentationml")) { //PowerPoint
                            extension = "pptx";
                        }
                        if (url.contains("wordprocessingml")) { //Word
                            extension = "docx";
                        }
                        if (url.contains("jpeg")) { //JPEG
                            extension = "jpeg";
                        }
                        if (url.contains("png")) { //PNG
                            extension = "png";
                        }
                        if (url.contains("mp3")) { //MP3
                            extension = "mp3";
                        }
                        if (url.contains("mp4")) { //MP4
                            extension = "mp4";
                        }
                        if (url.contains("m4a")) { //M4A
                            extension = "m4a";
                        }

                        // Extracting the base64-encoded content from the URL
                        int contentStartIndex = url.indexOf(",") + 1;
                        String encodedContent = url.substring(contentStartIndex);

                        // Decoding the base64-encoded content
                        byte[] decodedBytes = Base64.decode(encodedContent, Base64.DEFAULT);

                        // Generating the file name with download time and date
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy_HHmmss", Locale.getDefault());
                        String timeStamp = dateFormat.format(new Date());
                        String fileName = "download-" + timeStamp + "." + extension;

                        // Saving the decoded content to a file
                        // Saving the decoded content to a file in the Downloads folder
                        File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(downloadsDirectory, fileName);

                        try {
                            FileOutputStream fos = new FileOutputStream(file);
                            fos.write(decodedBytes);
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Toast.makeText(context, "Downloaded to Downloads folder.", Toast.LENGTH_SHORT).show();
                        try {
                            openDownloadedFile(file);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context, "Downloaded to Downloads folder.", Toast.LENGTH_SHORT).show();
                        }


                        return true;
                    } else {
                        Toast.makeText(context, "No Storage Permission", Toast.LENGTH_SHORT).show();

                        return true;
                    }
                }

                // Check if the URL should always open in an in-app tab
                if ((url != null) && shouldAlwaysOpenInInappTab(url)) {
                    openInInappTab(url);
                    return true;
                }

                if (SPECIAL_LINK_HANDLING_OPTIONS != 0) {
                    WebView.HitTestResult result = view.getHitTestResult();
                    String data = result.getExtra();
                    if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, " data :" + data);

                    if ((data != null && data.endsWith("#")) || url.startsWith("newtab:")) {

                        String finalUrl = url;
                        if (url.startsWith("newtab:")) {
                            finalUrl = url.substring(7);
                        }

                        // Open special link in an in-app tab
                        if ((SPECIAL_LINK_HANDLING_OPTIONS == 1) || shouldAlwaysOpenInInappTab(finalUrl)) {
                            openInInappTab(finalUrl);
                            return true;

                            // Open special link in Chrome
                        } else if (SPECIAL_LINK_HANDLING_OPTIONS == 2) {
                            view.getContext().startActivity(
                                    new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)));
                            return true;
                        }
                        return false;
                    }
                }

                return super.shouldOverrideUrlLoading(view, url);
            }
            return false;
        }

    }

    private String escapeJS(String input) {
        return input.replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
    }

    private void loadRewardedAd() {
        if (rewardedAd == null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            String admob_id = getString(R.string.admob_rewarded_id);
            RewardedAd.load(
                    this,
                    admob_id,
                    adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            Log.d(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                            rewardedAd = null;
                            // Retry loading after 30 seconds on failure
                            new Handler(Looper.getMainLooper()).postDelayed(() -> loadRewardedAd(), 30000);
                        }

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                            MainActivity.this.rewardedAd = rewardedAd;
                            Log.d(TAG, "Rewarded ad loaded successfully");
                        }
                    });
        }
    }

    private void hideOrRemoveAdViewsFromScreen() {
        mAdView.setVisibility(View.GONE);
        mAdView.destroy();
        adLayout.removeAllViews();
        adLayout.setVisibility(View.GONE);
    }

    private void loadAndShowAds() {
        if (disableAdMob) return;

        // initialize admob sdk
        // fb ads sdk is already initialized in WebViewApp class
        if (Config.SHOW_BANNER_AD || Config.SHOW_FULL_SCREEN_AD) {
            if (!Config.USE_FACEBOOK_ADS) {
                initializeGoogleAdmob();
            }
        }

        // show hidden ad views and load ads
        if (Config.SHOW_BANNER_AD) {
            if (Config.USE_FACEBOOK_ADS) {
                adLayout.removeAllViews();
                adLayout.addView(facebookAdView);
                adLayout.setVisibility(View.VISIBLE);
                facebookAdView.loadAd();
            } else {
                mAdView.setVisibility(View.VISIBLE);
                adLayout.removeAllViews();
                adLayout.addView(mAdView);
                adLayout.setVisibility(View.VISIBLE);

                AdRequest adRequest = new AdRequest.Builder()
                        .build();
                mAdView.loadAd(adRequest);
                mAdView.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        if (!HIDE_ADS_FOR_PURCHASE) {
                            mAdView.setVisibility(View.VISIBLE);
                            adLayout.setVisibility(View.VISIBLE);
                        } else {
                            mAdView.setVisibility(View.GONE);
                            adLayout.setVisibility(View.GONE);
                        }
                    }


                    @Override
                    public void onAdOpened() {
                        if (!HIDE_ADS_FOR_PURCHASE) {
                            mAdView.setVisibility(View.VISIBLE);
                            adLayout.setVisibility(View.VISIBLE);
                        } else {
                            mAdView.setVisibility(View.GONE);
                            adLayout.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onAdClosed() {
                    }
                });
            }
        } else {
            mAdView.setVisibility(View.GONE);
            adLayout.setVisibility(View.GONE);
        }
    }

    private void showRewardedAd() {
        if (rewardedAd == null) {
            Log.d("TAG", "The rewarded ad wasn't ready yet.");
            return;
        }

        Log.d("henry", "Displaying rewarded ad");

        rewardedAd.setFullScreenContentCallback(
                new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when ad is shown.
                        Log.d(TAG, "onAdShowedFullScreenContent");
                        //Toast.makeText(MainActivity.this, "onAdShowedFullScreenContent", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        // Called when ad fails to show.
                        Log.d(TAG, "onAdFailedToShowFullScreenContent");
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        rewardedAd = null;
                        //Toast.makeText(MainActivity.this, "onAdFailedToShowFullScreenContent", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // Called when ad is dismissed.
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        rewardedAd = null;
                        Log.d(TAG, "onAdDismissedFullScreenContent");
                        //Toast.makeText(MainActivity.this, "onAdDismissedFullScreenContent", Toast.LENGTH_SHORT).show();
                        // Preload the next rewarded ad.
                        MainActivity.this.loadRewardedAd();

                    }
                });

        Activity activityContext = MainActivity.this;

        rewardedAd.show(
                activityContext,
                rewardItem -> {
                    // Handle the reward.
                    Log.d("TAG", "The user earned the reward.");
                    String javascript = "javascript: adMobRewardGranted();";
                    webView.loadUrl(javascript);
                });
    }

    // Old AndroidJSInterface removed - now using WebAppInterface for remote-controlled ads
    // All ad control is now done via JavaScript:
    // - Android.loadInterstitial() / Android.showInterstitial()
    // - Android.loadRewardAd() / Android.showRewardAd()
    // - Android.showBanner("top"/"bottom") / Android.hideBanner()
    // Callbacks: onAdCompleted('REWARD'), onAdClosed()

    private String mGeolocationOrigin;
    private GeolocationPermissions.Callback mGeolocationCallback;

    private void requestLocationPermission(String origin, GeolocationPermissions.Callback callback) {
        if (!Config.requireLocation) {
            return;
        }
        mGeolocationOrigin = null;
        mGeolocationCallback = null;

        // If we don't have location permissions, we must request them first
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            String[] locationPermissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                locationPermissions = new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
//                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                };
            } else {
                locationPermissions = new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                };
            }

            // Show rationale if necessary
            boolean fineRationale = ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
            boolean coarseRationale = ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
//            boolean bgLocationRationale = ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            boolean bgLocationRationale = false;
            Log.e(TAG, "requestLocationPermission: " + fineRationale + " : " + coarseRationale + " : " + bgLocationRationale);
            if (
                    fineRationale || coarseRationale || bgLocationRationale
            ) {

                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(getString(R.string.requires_location_permission))
                        .setNeutralButton(android.R.string.ok, (dialogInterface, i) -> {
                            mGeolocationOrigin = origin;
                            mGeolocationCallback = callback;
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    locationPermissions, LOCATION_PERMISSION_REQUEST_CODE);
                        })
                        .show();
            } else {
                mGeolocationOrigin = origin;
                mGeolocationCallback = callback;
                ActivityCompat.requestPermissions(MainActivity.this,
                        locationPermissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
        // Otherwise just tell webview that permission has been granted
        else {
            callback.invoke(origin, true, false);
        }
    }

    @SuppressLint("NewApi")
    private void performHapticFeedback(HapticChoice hapticChoice) {

        // fallback if this haptic isn't available on device
        if (!isHapticVersionValid(hapticChoice)) {
            webView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            return;
        }

        // ... otherwise, perform relevant haptic
        if (hapticChoice == HapticChoice.SUCCESS) {
            webView.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            return;
        } else if (hapticChoice == HapticChoice.ERROR) {
            webView.performHapticFeedback(HapticFeedbackConstants.REJECT);
            return;
        } else if (hapticChoice == HapticChoice.LIGHT) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if ((vibrator != null) && (vibrator.hasVibrator())) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                return;
            }
        } else if (hapticChoice == HapticChoice.HEAVY) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if ((vibrator != null) && (vibrator.hasVibrator())) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK));
                return;
            }
        }
    }

    // Check versioning for haptic feedback support
    private boolean isHapticVersionValid(HapticChoice hapticChoice) {
        if (hapticChoice == HapticChoice.SUCCESS || hapticChoice == HapticChoice.ERROR) {
            return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R);
        } else if (hapticChoice == HapticChoice.LIGHT || hapticChoice == HapticChoice.HEAVY) {
            return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
        }
        return false;
    }

    private void handleIntentUrl(String url) {

        try {
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "No app to handle intent URL");

            // Fallback URL
            String fallbackParameter = "browser_fallback_url=";
            String separatorChar = ";";
            int startingIndex = 0;
            if (url.contains(fallbackParameter)) {
                try {
                    String fallbackURL = url.substring(url.indexOf(fallbackParameter) + fallbackParameter.length());
                    fallbackURL = fallbackURL.substring(startingIndex, fallbackURL.indexOf(separatorChar));
                    if (URLUtil.isValidUrl(fallbackURL)) {
                        if (BuildConfig.IS_DEBUG_MODE)
                            Log.d(TAG, "Fallback URL found, loading in external browser");
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackURL));
                        startActivity(i);
                    }
                } catch (Exception f) {
                    if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Fallback URL failed");
                }
            }
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    private class MyWebViewClient extends WebViewClient {

        MyWebViewClient() {
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            customCSS(); //2 times called, also called in onPageFinished() to prevent CSS adjustments from being briefly visible
            if (!isRedirected || INCREMENT_WITH_REDIRECTS) {
                super.onPageStarted(view, url, favicon);

                if (Config.SHOW_FULL_SCREEN_AD && !HIDE_ADS_FOR_PURCHASE) {
                    if (Config.USE_FACEBOOK_ADS) {
                        if (facebookInterstitialAd != null) {
                            facebookInterstitialAd.loadAd();
                        }
                    } else {
                        if (mInterstitialAd == null && !Config.USE_REWARDED_ADS_WHERE_POSSIBLE) {
                            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "ShowAdMobOnPageStart");
                            loadAdmobInterstatial();
                        } else if (Config.USE_REWARDED_ADS_WHERE_POSSIBLE) {
                            if (INCREMENT_WITH_REDIRECTS && mCount >= Config.SHOW_AD_AFTER_X) {
                                if (BuildConfig.IS_DEBUG_MODE)
                                    Log.d("MYTAG ->ADCOUNT", String.valueOf(mCount));
                                showRewardedAd();
                            }
                        }

                    }
                }
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!ENABLE_ZOOM) {
                webView.loadUrl("javascript:document.getElementsByName('viewport')[0].setAttribute('content', 'initial-scale=1.0, user-scalable=no');");
            }

            if (Config.INPUT_SCROLL_HELPER) {
                String script = "document.querySelectorAll('input, textarea').forEach((el) => {" +
                        "  el.addEventListener('focus', (event) => {" +
                        "    setTimeout(() => {" +
                        "      event.target.scrollIntoView({ behavior: 'smooth', block: 'center' });" +
                        "    }, 300);" +
                        "  });" +
                        "});";

                view.evaluateJavascript(script, null);
            }

            if (Config.TRANSPARENT_STATUS_BAR) {
                try {
                    webView.evaluateJavascript(
                            "(function() { return window.devicePixelRatio; })();",
                            value -> {
                                try {
                                    float dpr = Float.parseFloat(value);
                                    SystemBarUtils.getSafeInsetsAsync(MainActivity.this, insets -> {

                                        int topPx = (int) (insets.top / dpr);
                                        int bottomPx = (int) (insets.bottom / dpr);

                                        String js = "document.body.style.marginTop = '" + topPx + "px';" +
                                                "document.body.style.marginBottom = '" + bottomPx + "px';";
                                        webView.evaluateJavascript(js, null);
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            if (!isRedirected) {
                setTitle(view.getTitle());
                customCSS(); //2 times called, also called in onPageStarted() to prevent CSS adjustments from being briefly visible
                customJavaScript();


                if (Config.AUTO_INJECT_VARIABLES) {
                    // app version
                    webView.loadUrl("javascript: var versionNumber = '" + BuildConfig.VERSION_NAME + "';" +
                            "var bundleNumber  = '" + BuildConfig.VERSION_CODE + "';");

                    // onesignal
                    if (Config.PUSH_ENABLED) {
                        String OneSignaluserID = OneSignal.getUser().getPushSubscription().getId();
                        webView.loadUrl("javascript: var onesignalplayerid = '" + OneSignaluserID + "';");
                    }

                    // firebase
                    if (Config.FIREBASE_PUSH_ENABLED) {
                        String firebaseUserId = AlertManager.getFirebaseToken(MainActivity.this, "");
                        webView.loadUrl("javascript: var firebaseplayerid = '" + firebaseUserId + "';");
                    }

                    // uuid
                    webView.loadUrl("javascript: var uuid = '" + uuid + "';");

                    // Dark Mode Detection
                    int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    boolean isDarkMode = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);
                    webView.loadUrl("javascript: var isDarkMode = " + isDarkMode + ";");
                }

                // Disable link drag and drop
                if (!Config.LINK_DRAG_AND_DROP) {
                    String disableLinkDragScript = "javascript: var links = document.getElementsByTagName('a');" +
                            "for (var i = 0; i < links.length; i++) {" +
                            "   links[i].draggable = false;" +
                            "}";
                    view.loadUrl(disableLinkDragScript);
                }
                if (SPLASH_SCREEN_ACTIVATED && SPLASH_SCREEN_ACTIVE != 0 && (SplashScreen.getInstance() != null) && REMAIN_SPLASH_OPTION) {
                    if (SPLASH_SCREEN_ACTIVE + Config.SPLASH_MIN_TIME > System.currentTimeMillis()) {
                        new Handler().postDelayed(() -> {
                            SplashScreen.getInstance().animateFinish();
                            SPLASH_SCREEN_ACTIVE = 0;
                        }, SPLASH_SCREEN_ACTIVE + Config.SPLASH_MIN_TIME - System.currentTimeMillis());
                    } else {
                        SplashScreen.getInstance().animateFinish();
                        SPLASH_SCREEN_ACTIVE = 0;
                    }
                }
                showInterstitial();
                super.onPageFinished(view, url);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.e(TAG, "URL-2: " + url);

            if (!isRedirected) {
                hostpart = Uri.parse(url).getHost();
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "should override : " + url);

                // logic for loading given URL
                if (isConnectedNetwork()) {

                    // Check for a file download URL (can be internal or external)
                    if (url.contains(".") &&
                            downloadableExtension.contains(url.substring(url.lastIndexOf(".")))) {

                        webView.stopLoading();


                        String[] PERMISSIONS = {
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        };

                        if (Config.requireStorage) {
                            if (!hasPermissions(MainActivity.this, PERMISSIONS) && !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)) {
                                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, MULTIPLE_PERMISSIONS);
                                }
                            } else {
                                downloadFile(url);
                            }
                        }
                        return true;
                    }

                    if (!URLisExternal(url)) {
                        return false;

                    } else if (url.startsWith("getpurchasehistory://")) {
                        retrievePurchaseHistory();
                        return true;
                    } else if (url.startsWith("inapppurchase://") || url.startsWith("inappsubscription://")) {

                        if (extendediap) {
                            if (BuildConfig.IS_DEBUG_MODE)
                                Log.d(TAG, "play " + checkPlayServices());
                            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "InApp URL: " + url);
                            if (checkPlayServices() && billingClient.isReady()) {
                                disableAdMob = url.contains("disableadmob");
                                isConsumable = url.contains("consumable=true") || url.contains("package=consumable.");
                                handleAppPurchases(url);
                            } else {
                                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, " toast ");
                                String iaptext1 = "U2VlIGh0dHBzOi8vdGlueXVybC5jb20vaWFwLWZpeCB8IEluLUFwcCBQdXJjaGFzZSBmYWlsZWQu";
                                byte[] iapdata1 = Base64.decode(iaptext1, Base64.DEFAULT);
                                String iapdata1final = new String(iapdata1, StandardCharsets.UTF_8);
                                Toast.makeText(MainActivity.this, iapdata1final, Toast.LENGTH_SHORT).show();
                            }
                            return true;
                        } else {
                            String iaptext2 = "U2VlIGh0dHBzOi8vdGlueXVybC5jb20vaWFwLWZpeCB8IEluLUFwcCBQdXJjaGFzZSBmYWlsZWQu";
                            byte[] iapdata2 = Base64.decode(iaptext2, Base64.DEFAULT);
                            String iapdata2final = new String(iapdata2, StandardCharsets.UTF_8);
                            Toast.makeText(MainActivity.this, iapdata2final, Toast.LENGTH_LONG).show();
                            return true;
                        }
                    } else if (url.startsWith("restoreinapppurchases://")) {
                        restorePurchases();
                        return true;
                    } else if (url.startsWith("disableads://")) {
                        AlertManager.disableAds(getApplicationContext(), true);
                        disableAdMob = true;
                        hideOrRemoveAdViewsFromScreen();
                        return true;
                    } else if (url.startsWith("enableads://")) {
                        AlertManager.disableAds(getApplicationContext(), false);
                        disableAdMob = false;
                        HIDE_ADS_FOR_PURCHASE = false;
                        loadAndShowAds();
                        return true;
                    } else if (url.startsWith("enableflashlight://")) {
                        if (flashLightManager == null) return false;
                        // if (!flashLightManager.hasFlashLight()) return false;
                        flashLightManager.turnOn();
                        return true;
                    } else if (url.startsWith("disableflashlight://")) {
                        if (flashLightManager == null) return false;
                        // if (!flashLightManager.hasFlashLight()) return false;
                        flashLightManager.turnOff();
                        return true;
                    } else if (url.startsWith("qrcode://")) {
                        if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, url);
                        if (Config.requireCamera) {
                            // Check if the CAMERA permission is granted
                            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                // Permission is not granted, request for permission
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1402);
                            } else {
                                // Permission has already been granted, start the activity
                                Intent i = new Intent(MainActivity.this, QrCodeActivity.class);
                                startActivityForResult(i, REQUEST_CODE_QR_SCAN);
                            }
                        }
                        return true;
                    } else if (url.startsWith("barcodescanner://")) {
                        if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "requested barcode scanning");

                        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(MainActivity.this);

                        scanner
                                .startScan()
                                .addOnSuccessListener(
                                        barcode -> {
                                            // Task completed successfully
                                            if (BuildConfig.IS_DEBUG_MODE)
                                                Log.d(TAG, "successful barcode scanning");
                                            if (BuildConfig.IS_DEBUG_MODE)
                                                Log.d(TAG, barcode.getRawValue().toString());

                                            webView.loadUrl("javascript: var barcodescanresult = '" + barcode.getRawValue() + "';");
                                        })
                                .addOnCanceledListener(
                                        () -> {
                                            // Task canceled
                                        })
                                .addOnFailureListener(
                                        e -> {
                                            // Task failed with an exception
                                        });

                        return true;
                    } else if (url.startsWith("backgroundlocationoff://")) {
                        toggleBackgroundLocationService(false);
                        return true;
                    } else if (url.startsWith("backgroundlocationon://")) {
                        toggleBackgroundLocationService(true);
                        return true;
                    }
                    if (url.startsWith("savethisimage://?url=")) {
                        webView.stopLoading();
                        if (webView.canGoBack()) {
                            webView.goBack();
                        }
                        if (Config.requireStorage) {
                            final String imageUrl = url.substring(url.indexOf("=") + 1, url.length());
                            downloadImageNew("imagesaving", imageUrl);
                        }
                        return true;
                    } else if (url.contains("push.send.cancel")) {
                        if (Config.USER_AGENT.contains("VRGl")) {
                            if (url.contains("cartreminderpush.send.cancel")) {
                                stopCartReminderNotification();
                            }
                            if (url.contains("categoryrecommpush.cancel")) {
                                stopCategoryRecommNotification();
                            }
                            if (url.contains("productrecommpush.cancel")) {
                                stopProductRecommNotification();
                            }
                            return true;
                        } else {
                            stopNotification();
                            return true;
                        }
                    } else if (url.contains("push.send")) {
                        if (Config.USER_AGENT.contains("VRGl")) {
                            if (url.contains("cartreminderpush.send")) {
                                sendCartReminderNotification(url);
                            }
                            if (url.contains("categoryrecommpush.send")) {
                                sendCategoryRecommNotification(url);
                            }
                            if (url.contains("productrecommpush.send")) {
                                sendProductRecommNotification(url);
                            }
                            return true;
                        } else {
                            sendNotification(url);
                            return true;
                        }
                    } else if (url.startsWith("get-uuid://")) {
                        webView.loadUrl("javascript: var uuid = '" + uuid + "';");
                        return true;
                    } else if (url.startsWith("cancelinapppurchase://")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://support.google.com/googleplay?p=cancelsubsawf")); //forward to Google interface for managing subscriptions
                        view.getContext().startActivity(intent);
                        return true;
                    } else if (url.startsWith("enablepulltorefresh://")) {
                        mySwipeRefreshLayout.setEnabled(true);
                        return true;
                    } else if (url.startsWith("disablepulltorefresh://")) {
                        mySwipeRefreshLayout.setEnabled(false);
                        return true;
                    } else if (url.startsWith("reset://")) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            CookieManager.getInstance().removeAllCookies(null);
                            CookieManager.getInstance().flush();
                        } else if (mContext != null) {
                            CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(mContext);
                            cookieSyncManager.startSync();
                            CookieManager cookieManager = CookieManager.getInstance();
                            cookieManager.removeAllCookie();
                            cookieManager.removeSessionCookie();
                            cookieSyncManager.stopSync();
                            cookieSyncManager.sync();
                        }


                        WebSettings webSettings = webView.getSettings();
                        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                        webView.clearCache(true);
                        android.webkit.WebStorage.getInstance().deleteAllData();
                        Toast.makeText(MainActivity.this, "App reset was successful.", Toast.LENGTH_LONG).show();
                        loadMainUrl();
                        return true;
                    } else if (url.startsWith("readnfc://")) {
                        readModeNFC = true;
                        writeModeNFC = false;
                        return true;
                    } else if (url.startsWith("writenfc://")) {
                        writeModeNFC = true;
                        readModeNFC = false;
                        textToWriteNFC = url.substring(url.indexOf("=") + 1, url.length());
                        return true;
                    } else if (url.startsWith("spinneron://")) {
                        progressBar.setVisibility(View.VISIBLE);
                        return true;
                    } else if (url.startsWith("revenuecat://")) {
                        triggerRevenueCatToken(Uri.parse(url), !extendediap);
                        progressBar.setVisibility(View.GONE);
                        if (extendediap) {
                            launchRevenueCatPaywall(url);
                        } else {
                            String iaptext = "U2VlIGh0dHBzOi8vdGlueXVybC5jb20vaWFwLWZpeCB8IEluLUFwcCBQdXJjaGFzZSBmYWlsZWQu";
                            byte[] iapdata = Base64.decode(iaptext, Base64.DEFAULT);
                            String iapdatafinal = new String(iapdata, StandardCharsets.UTF_8);
                            Toast.makeText(MainActivity.this, iapdatafinal, Toast.LENGTH_LONG).show();
                        }
                        return true;
                    } else if (url.startsWith("spinneroff://")) {
                        progressBar.setVisibility(View.GONE);
                        return true;
                    } else if (url.startsWith("takescreenshot://")) {
                        verifystoragepermissions(MainActivity.this);

                        Toast.makeText(MainActivity.this, "Screenshot Saved", Toast.LENGTH_LONG).show();
                        screenshot(getWindow().getDecorView().getRootView(), "result");

                        return true;

                    } else if (url.startsWith("getpushwooshid://")) {
                        if (Config.PUSHWOOSH_ENABLED) {
                            String pushWooshHardwareId = Pushwoosh.getInstance().getHwid();
                            webView.loadUrl("javascript: var pushwooshplayerid = '" + pushWooshHardwareId + "';");
                        }
                        return true;
                    } else if (url.startsWith("getclipboard://")) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        if (clipboard != null && clipboard.hasPrimaryClip()) {
                            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                            String clipboardText = item.getText().toString();

                            // Inject clipboard data into WebView JavaScript
                            webView.evaluateJavascript("window.clipboarddata = '" + escapeJS(clipboardText) + "';", null);
                        } else {
                            Toast.makeText(mContext, "Clipboard is empty", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    } else if (url.startsWith("getonesignalplayerid://")) {


                        String OneSignaluserID = OneSignal.getUser().getPushSubscription().getId();
                        webView.loadUrl("javascript: var onesignalplayerid = '" + OneSignaluserID + "';");

                        return true;

                    } else if (url.startsWith("getfirebaseplayerid://")) {

                        String firebaseUserId = AlertManager.getFirebaseToken(MainActivity.this, "");
                        webView.loadUrl("javascript: var firebaseplayerid = '" + firebaseUserId + "';");

                        return true;

                    } else if (url.startsWith("getappversion://")) {
                        webView.loadUrl("javascript: var versionNumber = '" + BuildConfig.VERSION_NAME + "';" +
                                "var bundleNumber  = '" + BuildConfig.VERSION_CODE + "';");
                        return true;

                    } else if (url.startsWith("getstorelocation://")) {
                        if (billingClient != null && billingClient.isReady()) {
                            GetBillingConfigParams params = GetBillingConfigParams.newBuilder().build();
                            billingClient.getBillingConfigAsync(params, new BillingConfigResponseListener() {
                                @Override
                                public void onBillingConfigResponse(BillingResult billingResult, BillingConfig billingConfig) {
                                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                        String countryCode = billingConfig.getCountryCode(); // Country code in ISO-3166-1 alpha-2 format
                                        Log.d("CountryCode", countryCode);
                                        webView.loadUrl("javascript: var storeLocation = '" + countryCode + "';");
                                    }
                                }
                            });
                        } else {
                            Log.d("CountryCode", "Billing client not ready");
                        }
                        return true;

                    } else if (url.startsWith("shareapp://")) {

                        String sharetext = url.toString();
                        String newmeg = sharetext.substring(20);
                        if (BuildConfig.IS_DEBUG_MODE) Log.d("newmeg", newmeg);

                        String inputString = newmeg;
                        String delimiter = "&url=";
                        String[] components = inputString.split(delimiter);
                        String message2 = "";
                        String url2 = "";

                        if (components.length > 1) {
                            message2 = components[0];
                            url2 = components[1];
                        } else {
                            message2 = newmeg;
                        }
                        String message1 = message2.replace("%20", " ");
                        String url1 = url2.replace("%20", " ");

                        String totalMessage = "";
                        if (message1.length() == 0) {
                            totalMessage = url1;
                        } else if (url1.length() == 0) {
                            totalMessage = message1;
                        } else {
                            totalMessage = message1 + "\n" + url1;
                        }

                        List<String> objectsToShare = new ArrayList<>();
                        objectsToShare.add(totalMessage);

                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, totalMessage);

                        Intent chooser = Intent.createChooser(intent, "Share via");
                        chooser.setFlags(FLAG_ACTIVITY_NEW_TASK);

                        List<LabeledIntent> intents = new ArrayList<>();
                        for (ResolveInfo info : getPackageManager().queryIntentActivities(intent, 0)) {
                            Intent target = new Intent(Intent.ACTION_SEND);
                            target.setType("text/plain");
                            target.putExtra(Intent.EXTRA_TEXT, totalMessage);
                            target.setPackage(info.activityInfo.packageName);
                            intents.add(new LabeledIntent(target, info.activityInfo.packageName, info.loadLabel(getPackageManager()), info.icon));
                        }

                        Parcelable[] extraIntents = intents.toArray(new Parcelable[intents.size()]);
                        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
                        startActivity(chooser);

                        return true;
                    } else if (url.startsWith("statusbarcolor://") && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {

                        String input = url.substring(url.indexOf('/') + 2);
                        String[] values = input.split(",");
                        int nbValues = values.length;

                        if (nbValues == 3 || nbValues == 4) {
                            int colorValues[] = new int[nbValues];
                            for (int i = 0; i < nbValues; i++) {
                                colorValues[i] = Integer.parseInt(values[i].trim());
                            }
                            int color;
                            Double luminance = 0.0;
                            Double rgbFactor = 255.0;
                            if (nbValues == 3) {
                                // Index 0 = red, 1 = green, 2 = blue
                                color = Color.rgb(colorValues[0], colorValues[1], colorValues[2]);
                                luminance = 0.2126 * (colorValues[0] / rgbFactor) + 0.7152 * (colorValues[1] / rgbFactor) + 0.0722 * (colorValues[2] / rgbFactor);
                            } else {
                                // Inlcudes transparency (alpha); This feature is not fully supported yet as the webview dimensions need to be changed as well.
                                // Index 0 = alpga, 1 = red, 2 = green, 3 = blue
                                color = Color.argb(colorValues[0], colorValues[1], colorValues[2], colorValues[3]);
                            }
                            Window window = getWindow();
                            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                            window.setStatusBarColor(color);
                            window.setNavigationBarColor(color);

                            // Automatically decide the color of the status bar and navigation bar text
                            Double darkThreshold = 0.5;
                            View decorView = getWindow().getDecorView();
                            int flags = decorView.getSystemUiVisibility();
                            if (luminance < darkThreshold) {
                                // Color is dark; use white text
                                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                                decorView.setSystemUiVisibility(flags);

                            } else {
                                // Color is light; use black text
                                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                                decorView.setSystemUiVisibility(flags);
                            }
                        }
                        return true;

                    } else if (url.startsWith("statusbartextcolor://") && ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M))) {

                        String input = url.substring(url.indexOf('/') + 2);
                        View decorView = getWindow().getDecorView();
                        int flags = decorView.getSystemUiVisibility();

                        if (input.equals("white")) {
                            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                            decorView.setSystemUiVisibility(flags);

                        } else if (input.equals("black")) {
                            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                            decorView.setSystemUiVisibility(flags);
                        }

                        return true;

                    } else if (url.startsWith("bottombarcolor://") && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {

                        String input = url.substring(url.indexOf('/') + 2);
                        String[] values = input.split(",");
                        int nbValues = values.length;

                        if (nbValues == 3 || nbValues == 4) {
                            int colorValues[] = new int[nbValues];
                            for (int i = 0; i < nbValues; i++) {
                                colorValues[i] = Integer.parseInt(values[i].trim());
                            }
                            int color;
                            if (nbValues == 3) {
                                // Index 0 = red, 1 = green, 2 = blue
                                color = Color.rgb(colorValues[0], colorValues[1], colorValues[2]);
                            } else {
                                // Inlcudes transparency (alpha); This feature is not fully supported yet as the webview dimensions need to be changed as well.
                                // Index 0 = alpga, 1 = red, 2 = green, 3 = blue
                                color = Color.argb(colorValues[0], colorValues[1], colorValues[2], colorValues[3]);
                            }
                            Window window = getWindow();
                            window.setNavigationBarColor(color);
                        }
                        return true;

                    } else if (url.startsWith("navbartextcolor://") && ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M))) {

                        String input = url.substring(url.indexOf('/') + 2);
                        View decorView = getWindow().getDecorView();
                        int flags = decorView.getSystemUiVisibility();

                        if (input.equals("white")) {
                            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                            decorView.setSystemUiVisibility(flags);

                        } else if (input.equals("black")) {
                            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                            decorView.setSystemUiVisibility(flags);
                        }

                        return true;

                    } else if (url.startsWith("scanningmode://")) {

                        String input = url.substring(url.indexOf('/') + 2);

                        if (input.equals("auto")) {
                            turnOnScanningMode();
                        } else if (input.equals("on")) {
                            persistentScanningMode = true;
                            turnOnScanningMode();
                        } else if (input.equals("off")) {
                            persistentScanningMode = false;
                            turnOffScanningMode();
                        }
                        return true;
                    }
                } else if (!isConnectedNetwork()) {
                    if (Config.FALLBACK_USE_LOCAL_HTML_FOLDER_IF_OFFLINE) {
                        if (!offlineFileLoaded) {
                            loadLocal(INDEX_FILE);
                            offlineFileLoaded = true;
                        } else {
                            loadLocal(url);
                        }
                    } else {
                        offlineLayout.setVisibility(View.VISIBLE);
                    }
                    return true;
                }

                if (hostpart.contains("whatsapp.com")) {
                    final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    final int newDocumentFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ? Intent.FLAG_ACTIVITY_NEW_DOCUMENT : Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
                    intent.addFlags(FLAG_ACTIVITY_NO_HISTORY | newDocumentFlag | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    startActivity(intent);
                }

                // handle whitelisted links for always opening in the external browser
                for (String whitelistedLink : BROWSER_WHITELIST) {
                    if (hostpart.contains(whitelistedLink)) {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(i);
                        return true;  // prevents the link also opening in the app by overriding
                    }
                }

                if (((Config.EXTERNAL_LINK_HANDLING_OPTIONS != 0)
                        && !(url).startsWith("file://") && (!Config.USE_LOCAL_HTML_FOLDER
                        || !(url).startsWith("file://"))) && URLUtil.isValidUrl(url)) {
                    if (Config.EXTERNAL_LINK_HANDLING_OPTIONS == 1) {
                        // check for blacklisting of option
                        for (String blacklistedLink : NEVER_OPEN_IN_INAPP_TAB) {
                            if (blacklistedLink.contains(hostpart)) {
                                return false;
                            }
                        }
                        // otherwise, open in a new tab (additional in-app browser)
                        openInInappTab(url);
                        return true;
                    } else if (Config.EXTERNAL_LINK_HANDLING_OPTIONS == 2) {

                        // check for blacklist
                        for (String blacklistedLink : BROWSER_BLACKLIST) {
                            if (blacklistedLink.contains(hostpart)) {
                                // shouldn't override url loading now, thus false.
                                return false;
                            }
                        }
                        // otherwise, open in a new browser
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(i);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return false;
        }
    }

    private void launchRevenueCatPaywall(String url) {
        fetchRevenueCatCustomerInfo(url);
    }

    private void triggerRevenueCatToken(Uri triggerUri, boolean extendedFailed) {
        SharedPreferences prefs = getSharedPreferences("revenuecat_tokens", MODE_PRIVATE);
        String key = Config.REVENUECAT_API_KEY + "_" + Config.REVENUECAT_PROJECT_ID;
        if (!prefs.getBoolean(key, false)) {
            String base64Url = "aHR0cHM6Ly9yZXZlbnVlY2F0LWFwaS53ZWJ2aWV3Z29sZC5jb20vZ2V0LXRva2VuLz9hcGlrZXk9JXMmcGxhbklEPSVzJmFwcGJ1bmRsZUlEPSVz";
            byte[] decoded = Base64.decode(base64Url, Base64.DEFAULT);
            String format = new String(decoded, StandardCharsets.UTF_8);
            String url = String.format(format, Config.REVENUECAT_API_KEY, Config.REVENUECAT_PROJECT_ID, getPackageName());
            try {
                String externalId = triggerUri.getQueryParameter("external_id");
                String product = triggerUri.getQueryParameter("product");
                if (externalId != null && !externalId.isEmpty()) {
                    url += "&external_id=" + URLEncoder.encode(externalId, "UTF-8");
                }
                if (product != null && !product.isEmpty()) {
                    url += "&product=" + URLEncoder.encode(product, "UTF-8");
                }
                url += "&wvglicense=" + URLEncoder.encode(Config.PURCHASECODE, "UTF-8");
                if (extendedFailed) {
                    url += "&extendedfailed=true";
                }
            } catch (UnsupportedEncodingException e) {
                // ignore
            }
            final String finalUrl = url;
            new Thread(() -> {
                try {
                    URL u = new URL(finalUrl);
                    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                    conn.setRequestMethod("GET");
                    conn.getInputStream().close();
                } catch (IOException ignored) {
                }
                prefs.edit().putBoolean(key, true).apply();
            }).start();
        }
    }

    private void fetchRevenueCatCustomerInfo(String triggerUrl) {
        Uri uri = Uri.parse(triggerUrl);
        String customerId = uri.getQueryParameter("external_id");
        if (customerId == null || customerId.isEmpty()) {
            toast("Invalid customer");
            progressBar.setVisibility(View.GONE);
            return;
        }

        Purchases.getSharedInstance().logIn(customerId, new LogInCallback() {
            @Override
            public void onReceived(@NonNull CustomerInfo customerInfo, boolean b) {
                Log.e(TAG, "customerInfo: " + customerInfo.toString());
                fetchProduct(triggerUrl);
            }

            @Override
            public void onError(@NonNull PurchasesError purchasesError) {
                Log.e(TAG, "Login error: " + purchasesError.getMessage());
                toast("Login error: " + purchasesError.getMessage());
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void fetchProduct(String triggerUrl) {
        Uri uri = Uri.parse(triggerUrl);
        String packageName = uri.getQueryParameter("product");

        if (packageName == null || packageName.isEmpty()) {
            toast("Invalid package");
            progressBar.setVisibility(View.GONE);
            return;
        }

        String productId, planId;

        if (packageName.contains(":")){
            // Subscription package
            productId = packageName.split(":")[0];
            planId = packageName.split(":")[1];
            if (productId.isEmpty() || planId.isEmpty()){
                if (productId.isEmpty()){
                    toast("Invalid product");
                    progressBar.setVisibility(View.GONE);
                } else {
                    toast("Invalid plan");
                    progressBar.setVisibility(View.GONE);
                }
                return;
            }
        } else {
            // One time package
            productId = packageName;
            planId = "";
        }


        Log.e(TAG, "Product ID: " + productId + ", Plan ID: " + planId);

        ArrayList<String> productList = new ArrayList<>();
        productList.add(productId);

        Purchases.getSharedInstance().getProducts(productList, new GetStoreProductsCallback() {
            @Override
            public void onReceived(@NonNull List<StoreProduct> list) {
                boolean isMatchingProductFound = false;
                for (StoreProduct rPackage : list) {
                    String rPlanId = rPackage.getId();
                    Log.e(TAG, "getProducts: " + rPlanId);
                    if (rPlanId.equalsIgnoreCase(packageName)) {
                        Log.e(TAG, "Matched package");
                        isMatchingProductFound = true;
                        purchasePackageThroughRevenueCat(triggerUrl, rPackage);
                        break;
                    }
                    Log.e(TAG, "---------------------");
                }
                if (!isMatchingProductFound){
                    toast("No product matched");
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(@NonNull PurchasesError purchasesError) {
                Log.e(TAG, "getProducts error: " + purchasesError.getMessage());
                Log.e(TAG, "getProducts error: " + purchasesError.getCode());
                Log.e(TAG, "getProducts error: " + purchasesError.getUnderlyingErrorMessage());
                toast("getProducts error: " + purchasesError.getMessage());
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void purchasePackageThroughRevenueCat(String triggerUrl, StoreProduct rPackage) {

        PurchaseParams.Builder purchaseParamBuilder = new PurchaseParams.Builder(
                this, rPackage
        );
        Purchases.getSharedInstance().purchase(
                purchaseParamBuilder.build(),
                new PurchaseCallback() {
                    @Override
                    public void onCompleted(@NonNull StoreTransaction storeTransaction, @NonNull CustomerInfo customerInfo) {
                        EntitlementInfos entitlements = customerInfo.getEntitlements();
                        EntitlementInfo entitlement = entitlements.get("");
                        Log.e(TAG, "customerInfo: " + customerInfo.toString());
                        if (entitlement != null && entitlement.isActive()) {
                            Log.e(TAG, "entitlement Info: " + entitlement.toString());
                        }
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(@NonNull PurchasesError purchasesError, boolean b) {
                        Log.e(TAG, "Purchase error: " + purchasesError.getMessage());
                        toast("Purchase error: " + purchasesError.getMessage());
                        progressBar.setVisibility(View.GONE);
                    }
                }
        );
    }

    private void retrievePurchaseHistory() {
        if (billingClient != null && billingClient.isReady()) {
            QueryPurchaseHistoryParams.Builder historyParams = QueryPurchaseHistoryParams.newBuilder();
            historyParams.setProductType(BillingClient.ProductType.INAPP);
            PurchaseHistoryResponseListener purchaseHistoryListener = (billingResult, list) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null && !list.isEmpty()) {
                    for (PurchaseHistoryRecord purchase : list) {
                        String purchaseData = purchase.getOriginalJson();
                        String signature = purchase.getSignature();

                        try {
                            JSONObject object = new JSONObject(purchaseData);
                            Log.e(TAG, "retrievePurchaseHistory: " + object);

                            String productId = object.getString("productId");
                            String purchaseToken = purchase.getPurchaseToken(); // Unique transaction ID
                            String purchaseSignature = purchase.getSignature(); // Unique signature
                            JSONArray receiptArray = new JSONArray(purchase.getSkus()); // Assuming skus as subreceipts
                            String jsCode = "var planID = '" + productId + "';" +
                                    "var purchaseToken = '" + purchaseToken + "';" +
                                    "var signature = '" + purchaseSignature + "';" +
                                    "var subreceipts = " + receiptArray + ";";
                            webView.evaluateJavascript(jsCode, s -> Log.e(TAG, "onPurchased: " + productId));
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };
            billingClient.queryPurchaseHistoryAsync(historyParams.build(), purchaseHistoryListener);


            QueryPurchaseHistoryParams.Builder historyParamsSubscription = QueryPurchaseHistoryParams.newBuilder();
            historyParamsSubscription.setProductType(BillingClient.ProductType.SUBS);
            billingClient.queryPurchaseHistoryAsync(historyParamsSubscription.build(), purchaseHistoryListener);
        }
    }

    /**
     * This function will update the cache of playstore, so that we can get the actual purchases after it.
     */
    private void restorePurchases() {
        QueryPurchaseHistoryParams.Builder historyParams = QueryPurchaseHistoryParams.newBuilder();
        historyParams.setProductType(BillingClient.ProductType.INAPP);
        PurchaseHistoryResponseListener purchaseHistoryListener = (billingResult, list) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null && !list.isEmpty()) {
                verifyPreviousPurchases(false);
            } else {
                Log.e(TAG, "restorePurchases failed: " + billingResult.getDebugMessage());
            }
        };
        billingClient.queryPurchaseHistoryAsync(historyParams.build(), purchaseHistoryListener);


        QueryPurchaseHistoryParams.Builder subHistoryParams = QueryPurchaseHistoryParams.newBuilder();
        subHistoryParams.setProductType(BillingClient.ProductType.SUBS);
        PurchaseHistoryResponseListener subscriptionHistoryListener = (billingResult, list) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null && !list.isEmpty()) {
                verifyPreviousPurchases(true);
            } else {
                Log.e(TAG, "restorePurchases failed: " + billingResult.getDebugMessage());
            }
        };
        billingClient.queryPurchaseHistoryAsync(subHistoryParams.build(), subscriptionHistoryListener);
    }

    private void verifyPreviousPurchases(boolean shouldCheckSubscription) {
        QueryPurchasesParams.Builder purchaseParams = QueryPurchasesParams.newBuilder();
        PurchasesResponseListener purchaseListener = (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (Purchase purchase : purchases) {
                            try {
                                JSONObject object = new JSONObject(purchase.getOriginalJson());
                                String productId = object.getString("productId");
                                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                    Toast.makeText(MainActivity.this, "Purchase restored :)", Toast.LENGTH_SHORT).show();
                                    if (disableAdMob) {
                                        AlertManager.purchaseState(getApplicationContext(), true);
                                        mAdView.setVisibility(View.GONE);
                                        mAdView.destroy();
                                        adLayout.removeAllViews();
                                        adLayout.setVisibility(View.GONE);

                                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                                        SharedPreferences.Editor editor = settings.edit();
                                        editor.putString("disableAdMobDone", "removed");
                                        editor.commit();

                                    }
                                }
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });
            }
        };
        if (!shouldCheckSubscription) {
            purchaseParams.setProductType(BillingClient.ProductType.INAPP);
        } else {
            purchaseParams.setProductType(BillingClient.ProductType.SUBS);
        }
        billingClient.queryPurchasesAsync(purchaseParams.build(), purchaseListener);
    }

    private void turnOnScanningMode() {
        if (!scanningModeOn) {
            WindowManager.LayoutParams layout = getWindow().getAttributes();

            // Record previous screen brightness
            previousScreenBrightness = layout.screenBrightness;

            // Turn on scanning mode
            scanningModeOn = true;
            layout.screenBrightness = 1F;
            getWindow().setAttributes(layout);
            if (!PREVENT_SLEEP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    private void turnOffScanningMode() {
        if (scanningModeOn) {
            WindowManager.LayoutParams layout = getWindow().getAttributes();

            // Turn off scanning mode
            scanningModeOn = false;
            layout.screenBrightness = previousScreenBrightness;
            getWindow().setAttributes(layout);
            if (!PREVENT_SLEEP) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    private void handleAppPurchases(String url) {
        String keyPackage = "package=";
        String keySuccessURL = "&successful_url=";
        String keyExpiredURL = "&expired_url=";
        if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "play " + checkPlayServices());
        int packageIndex = -1;
        int successIndex = -1;
        int expireIndex = -1;
        String packagePlan = "";
        if (url.contains(keyPackage)) {
            packageIndex = url.indexOf(keyPackage) + keyPackage.length();
        }
        if (url.contains(keySuccessURL)) {
            successIndex = url.indexOf(keySuccessURL) + keySuccessURL.length();
        }
        if (url.contains(keyExpiredURL)) {
            expireIndex = url.indexOf(keyExpiredURL) + keyExpiredURL.length();
        }
        try {
            if (packageIndex != -1) {
                packagePlan = url.substring(packageIndex, url.indexOf("&"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (expireIndex == -1) {
                successUrl = url.split(keySuccessURL)[1];
                failUrl = "";
            } else {
                successUrl = url.substring(successIndex, expireIndex - keyExpiredURL.length());
                failUrl = url.substring(expireIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!packagePlan.isEmpty()) {
            String productType = "";
            if (url.startsWith("inapppurchase://")) {
                productType = BillingClient.ProductType.INAPP;
            } else if (url.startsWith("inappsubscription://")) {
                productType = BillingClient.ProductType.SUBS;
            }
            Log.e(TAG, "handleAppPurchases: " + packagePlan + " x " + productType);
            List<QueryProductDetailsParams.Product> skuList = new ArrayList<>();
            skuList.add(
                    QueryProductDetailsParams.Product
                            .newBuilder()
                            .setProductType(productType)
                            .setProductId(packagePlan)
                            .build()
            );
//            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            QueryProductDetailsParams.Builder params =
                    QueryProductDetailsParams.newBuilder();
            params.setProductList(skuList);
            checkItemPurchase(params.build());
        } else {
            Toast.makeText(this, "Unable to get any package. Try again!", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendNotification(String url) {
        final int secondsDelayed = Integer.parseInt(url.split("=")[1]);

        final String[] contentDetails = (url.substring((url.indexOf("msg!") + 4), url.length())).split("&!#");
        String message = contentDetails[0].replaceAll("%20", " ");
        String title = contentDetails[1].replaceAll("%20", " ");

        try {
            message = URLDecoder.decode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            title = URLDecoder.decode(title, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String urlToOpen = null;
        // if data has length greater then 2 then there should be URL at index 2
        if (contentDetails.length > 2) {
            urlToOpen = contentDetails[2].replaceAll("%20", " ");
        }

        final Notification.Builder builder = getNotificationBuilder(title, message, urlToOpen);

        final Notification notification = builder.build();
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationHandler = new Handler();
        notificationHandler.postDelayed(() -> {
            notificationManager.notify(0, notification);
            notificationHandler = null;
        }, secondsDelayed * 1000);
    }

    private void sendCartReminderNotification(String url) {


        final int secondsDelayed = Integer.parseInt(url.split("=")[1]);

        final String[] contentDetails = (url.substring((url.indexOf("msg!") + 4), url.length())).split("&!#");

        String message = contentDetails[0].replaceAll("%20", " ");
        String title = contentDetails[1].replaceAll("%20", " ");


        try {
            message = URLDecoder.decode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            title = URLDecoder.decode(title, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        String urlToOpen = null;
        // if data has length greater than 2 then there should be URL at index 2
        if (contentDetails.length > 2) {
            urlToOpen = contentDetails[2].replaceAll("%20", " ");
        }

        final Notification.Builder builder = getNotificationBuilder(title, message, urlToOpen);

        final Notification notification = builder.build();
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        CartRemindernotificationHandler = new Handler();
        CartRemindernotificationHandler.postDelayed(() -> {
            notificationManager.notify(0, notification);
            CartRemindernotificationHandler = null;
        }, secondsDelayed * 1000);
    }

    private void sendCategoryRecommNotification(String url) {


        final int secondsDelayed = Integer.parseInt(url.split("=")[1]);

        final String[] contentDetails = (url.substring((url.indexOf("msg!") + 4), url.length())).split("&!#");


        String message = contentDetails[0].replaceAll("%20", " ");
        String title = contentDetails[1].replaceAll("%20", " ");


        try {
            message = URLDecoder.decode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            title = URLDecoder.decode(title, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        String urlToOpen = null;
        // if data has length greater than 2 then there should be URL at index 2
        if (contentDetails.length > 2) {
            urlToOpen = contentDetails[2].replaceAll("%20", " ");
        }

        final Notification.Builder builder = getNotificationBuilder(title, message, urlToOpen);

        final Notification notification = builder.build();
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        CategoryRecommNotificationHandler = new Handler();
        CategoryRecommNotificationHandler.postDelayed(() -> {
            notificationManager.notify(0, notification);
            CategoryRecommNotificationHandler = null;
        }, secondsDelayed * 1000);
    }

    private void sendProductRecommNotification(String url) {
        final int secondsDelayed = Integer.parseInt(url.split("=")[1]);

        final String[] contentDetails = (url.substring((url.indexOf("msg!") + 4), url.length())).split("&!#");
        String message = contentDetails[0].replaceAll("%20", " ");
        String title = contentDetails[1].replaceAll("%20", " ");


        try {
            message = URLDecoder.decode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            title = URLDecoder.decode(title, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String urlToOpen = null;
        // if data has length greater than 2 then there should be URL at index 2
        if (contentDetails.length > 2) {
            urlToOpen = contentDetails[2].replaceAll("%20", " ");
        }

        final Notification.Builder builder = getNotificationBuilder(title, message, urlToOpen);

        final Notification notification = builder.build();
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        ProductRecommNotificationHandler = new Handler();
        ProductRecommNotificationHandler.postDelayed(() -> {
            notificationManager.notify(0, notification);
            ProductRecommNotificationHandler = null;
        }, secondsDelayed * 1000);
    }

    private void stopNotification() {
        if (notificationHandler != null) {
            notificationHandler.removeCallbacksAndMessages(null);
            notificationHandler = null;
        }
    }

    private void stopCartReminderNotification() {
        if (CartRemindernotificationHandler != null) {
            CartRemindernotificationHandler.removeCallbacksAndMessages(null);
            CartRemindernotificationHandler = null;
        }
    }

    private void stopCategoryRecommNotification() {
        if (CategoryRecommNotificationHandler != null) {
            CategoryRecommNotificationHandler.removeCallbacksAndMessages(null);
            CategoryRecommNotificationHandler = null;
        }
    }

    private void stopProductRecommNotification() {
        if (ProductRecommNotificationHandler != null) {
            ProductRecommNotificationHandler.removeCallbacksAndMessages(null);
            ProductRecommNotificationHandler = null;
        }
    }

    private Notification.Builder getNotificationBuilder(String title, String message, String urlToOpen) {

        createNotificationChannel();
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(MainActivity.this, getString(R.string.local_notification_channel_id));
        } else {
            builder = new Notification.Builder(MainActivity.this);
        }

        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        intent.putExtra("ONESIGNAL_URL", urlToOpen);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
//                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(MainActivity.this, 1, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        }

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(title)
                .setAutoCancel(true)
                .setContentText(message)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        return builder;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.local_notification_channel_name);
            String description = getString(R.string.local_notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getString(R.string.local_notification_channel_id), name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void downloadFile(String url) {
        try {
            boolean isICSFile = false;
            String fileName = getFileNameFromURL(url);
            if (fileName.endsWith(".ics")) {
                isICSFile = true;
            } else {
                Toast.makeText(MainActivity.this, "Downloading file...", Toast.LENGTH_SHORT).show();
            }
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            String cookie = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("Cookie", cookie);
            request.allowScanningByMediaScanner();
            if (isICSFile) {
                request.setVisibleInDownloadsUi(false);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            } else {
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            }
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
        } catch (Exception e) {
            e.printStackTrace();
        }


        BroadcastReceiver onComplete = new BroadcastReceiver() {

            public void onReceive(Context ctxt, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    openDownloadedAttachment(MainActivity.this, downloadId);
                }
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(
                    onComplete,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
            );
        }
    }

    public static boolean isRooted() { //credit to Sanjay Bhalani (https://stackoverflow.com/a/57590343)
        // get from build info
        String buildTags = android.os.Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }
        // check if /system/app/Superuser.apk is present
        try {
            File file = new File("/system/app/Superuser.apk");
            if (file.exists()) {
                return true;
            }
        } catch (Exception e1) {
            // ignore
        }
        // try executing commands
        //return canExecuteCommand("/system/xbin/which su")|| canExecuteCommand("/system/bin/which su") || canExecuteCommand("which su");
        if (!canExecuteCommand("su"))
            if (findBinary("su"))
                return true;
        return false;
    }

    private void showRootedErrorMessage() {

        new AlertDialog.Builder(this)
                .setTitle("Security Error")
                .setMessage("This app cannot run on a rooted device for security reasons.")
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish(); // Close the app
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showNoPermissionMessage(String permission) {
        String message = "";
        switch (permission) {
            case Manifest.permission.RECORD_AUDIO:
                message = "audio";
                break;
            case Manifest.permission.CAMERA:
                message = "camera";
                break;
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                message = "storage";
                break;
            case Manifest.permission.ACCESS_COARSE_LOCATION:
            case Manifest.permission.ACCESS_FINE_LOCATION:
                message = "location";
                break;
        }

        new AlertDialog.Builder(this)
                .setTitle("Permission Error")
                .setMessage(getString(R.string.no_camera_permission, message))
                .setPositiveButton("Go to settings", (dialog, which) -> {
                    Intent intent = new Intent(ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getApplicationContext().getPackageName(), null));
                    intent.addCategory(CATEGORY_DEFAULT);
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(FLAG_ACTIVITY_NO_HISTORY);
                    intent.addFlags(FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

                    startActivity(intent);
                })
                .setCancelable(false)
                .show();
    }

    public static boolean findBinary(String binaryName) {  //credit to Sanjay Bhalani (https://stackoverflow.com/a/57590343)
        boolean found = false;
        if (!found) {
            String[] places = {"/sbin/", "/system/bin/", "/system/xbin/",
                    "/data/local/xbin/", "/data/local/bin/",
                    "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
            for (String where : places) {
                if (new File(where + binaryName).exists()) {
                    found = true;

                    break;
                }
            }
        }
        return found;
    }

    // executes a command on the system
    private static boolean canExecuteCommand(String command) {  //credit to Sanjay Bhalani (https://stackoverflow.com/a/57590343)
        boolean executedSuccesfully;
        try {
            Runtime.getRuntime().exec(command);
            executedSuccesfully = true;
        } catch (Exception e) {
            executedSuccesfully = false;
        }
        return executedSuccesfully;
    }

    public static String getFileNameFromURL(String url) {
        if (url == null) {
            return "";
        }
        try {
            URL resource = new URL(url);
            String host = resource.getHost();
            if (host.length() > 0 && url.endsWith(host)) {
                return "";
            }
        } catch (MalformedURLException e) {
            return "";
        }

        int startIndex = url.lastIndexOf('/') + 1;
        int length = url.length();


        int lastQMPos = url.lastIndexOf('?');
        if (lastQMPos == -1) {
            lastQMPos = length;
        }

        int lastHashPos = url.lastIndexOf('#');
        if (lastHashPos == -1) {
            lastHashPos = length;
        }

        int endIndex = Math.min(lastQMPos, lastHashPos);
        return url.substring(startIndex, endIndex);
    }

    private class CustomeGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (ENABLE_SWIPE_NAVIGATE) {
                if (e1 == null || e2 == null) return false;
                if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
                else {

                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int screenWidth = displayMetrics.widthPixels;
                    int edgeSwipeTolerance = 30;

                    try {
                        // Detect a left swipe
                        if (e1.getX() - e2.getX() > 100 && Math.abs(velocityX) > 800) {
//                            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "LEFT swipe: e1.X = " + e1.getX() + ", e2.X = " + e2.getX());
                            // Detect a "forwards" gesture (left swipe from the right edge)
                            if (e1.getX() > (screenWidth - edgeSwipeTolerance)) {
                                if (BuildConfig.IS_DEBUG_MODE)
                                    Log.d(TAG, "FORWARDS swipe detected");
                                if (webView.canGoForward()) {
                                    webView.goForward();
                                }
                                return true;
                            }
                        }
                        // Detect a right swipe
                        else if (e2.getX() - e1.getX() > 100 && Math.abs(velocityX) > 800) {
//                            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "RIGHT swipe: e1.X = " + e1.getX() + ", e2.X = " + e2.getX());
                            // Detect a "backwards" gesture (right swipe from the left edge)
                            if (e1.getX() < edgeSwipeTolerance) {
                                if (BuildConfig.IS_DEBUG_MODE)
                                    Log.d(TAG, "BACKWARDS swipe detected");
                                if (webView.canGoBack()) {
                                    webView.goBack();
                                }
                                return true;
                            }
                        }
                    } catch (Exception e) { // nothing
                    }
                    return false;
                }
            }
            return false;
        }

        // Register when a single tap is made on the screen
        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
            if (INCREMENT_WITH_TAPS) {
                mCountTaps++; // Increment tap counter
                if (BuildConfig.IS_DEBUG_MODE)
                    Log.d("MYTAG ->ADCOUNTFROMTAPS", String.valueOf(mCountTaps));
            }
            if (mCountTaps >= Config.SHOW_AD_AFTER_X_TAPS) {
                if (Config.SHOW_FULL_SCREEN_AD && !HIDE_ADS_FOR_PURCHASE) {
                    if (Config.USE_FACEBOOK_ADS) { // Using Facebook Ads
                        if (BuildConfig.IS_DEBUG_MODE)
                            Log.d(TAG, "ShowFacebookAdBecauseOfScreenTaps");
                        if (facebookInterstitialAd != null && facebookInterstitialAd.isAdLoaded()) {
                            facebookInterstitialAd.show();
                        }
                        facebookInterstitialAd.loadAd();
                    } else { // Using AdMob
                        if (mInterstitialAd == null) {
                            if (BuildConfig.IS_DEBUG_MODE)
                                Log.d(TAG, "ShowAdMobBecauseOfScreenTaps");
                            if (Config.USE_REWARDED_ADS_WHERE_POSSIBLE) {
                                showRewardedAd();
                            } else {
                                loadAdmobInterstatial();
                            }
                        }

                    }
                }
                mCountTaps = 0;
            }
            return true;
        }
    }

    private class AdvanceWebChromeClient extends MyWebChromeClient {

        private Handler notificationHandler;
        private Handler CartRemindernotificationHandler;
        private Handler CategoryRecommNotificationHandler;
        private Handler ProductRecommNotificationHandler;

        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            requestLocationPermission(origin, callback);
//            callback.invoke(origin, true, false);
        }

        @Override
        public void onCloseWindow(WebView window) {
            super.onCloseWindow(window);
            ClosePopupWindow(mWebviewPop);
            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "onCloseWindow url " + window.getUrl());
            if (BuildConfig.IS_DEBUG_MODE)
                Log.d(TAG, "onCloseWindow url " + window.getOriginalUrl());
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {

            Bundle extras = getIntent().getExtras();
            String URL = null;
            if (extras != null) {
                URL = extras.getString("ONESIGNAL_URL");
            }
            if (URL != null && !URL.equalsIgnoreCase("")) {
                isNotificationURL = true;
                deepLinkingURL = URL;
            } else isNotificationURL = false;
            preferences.edit().putString("proshow", "show").apply();

            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, " LOG24 " + deepLinkingURL);

            WebView.HitTestResult result = view.getHitTestResult();
            String data = result.getExtra();

            // Link with an Image
            if (result.getType() == result.SRC_IMAGE_ANCHOR_TYPE) {
                // Get the source link, not the image link
                Message href = view.getHandler().obtainMessage();
                view.requestFocusNodeHref(href);
                String imageLinkSource = href.getData().getString("url");
                data = imageLinkSource;
            }

            // Check if the URL should always open in an in-app tab
            if ((data != null) && shouldAlwaysOpenInInappTab(data)) {
                openInInappTab(data);
                return true;
            }

            // Open special link in-app
            if (SPECIAL_LINK_HANDLING_OPTIONS == 0) {

                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "if ");

                if ((data == null) || (data != null && data.endsWith("#"))) {
                    if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "else true ");
                    windowContainer.setVisibility(View.VISIBLE);
                    mWebviewPop = new WebView(view.getContext());
                    webViewSetting(mWebviewPop);

                    mWebviewPop.setWebChromeClient(new AdvanceWebChromeClient());
                    mWebviewPop.setWebViewClient(new AdvanceWebViewClient());
                    if (!Config.USER_AGENT.isEmpty()) {
                        mWebviewPop.getSettings().setUserAgentString(Config.USER_AGENT);
                    } else {
                        mWebviewPop.getSettings().setUserAgentString(mWebviewPop.getSettings().getUserAgentString().replace("wv", ""));
                    }
                    mContainer.addView(mWebviewPop);

                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    transport.setWebView(mWebviewPop);
                    resultMsg.sendToTarget();
                    return true;
                } else {

                    WebSettings webSettings = webView.getSettings();
                    webSettings.setJavaScriptEnabled(true);
                    webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                    webSettings.setSupportMultipleWindows(true);

                    if (URLUtil.isValidUrl(data)) {
                        webView.loadUrl(data);
                    }
                }

                // Open special link in a new in-app tab
            } else if (SPECIAL_LINK_HANDLING_OPTIONS == 1) {

                if (data == null) {
                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                    builder.setToolbarColor(getResources().getColor(R.color.colorPrimaryDark));
                    CustomTabsIntent customTabsIntent = builder.build();
                    WebView newWebView = new WebView(view.getContext());
                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    transport.setWebView(newWebView);
                    resultMsg.sendToTarget();
                    newWebView.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            Log.e(TAG, "URL-3: " + url);
                            // Retrieve cookies from WebView & set cookies in the customTabsIntent WebView
                            CookieManager cookieManager = CookieManager.getInstance();
                            String allCookies = cookieManager.getCookie(Uri.parse(url).toString());
                            if (allCookies != null) {
                                String[] cookieList = allCookies.split(";");
                                for (String cookie : cookieList) {
                                    customTabsIntent.intent.putExtra("android.webkit.CookieManager.COOKIE", cookie.trim());
                                }
                            }
                            customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
                            webView.stopLoading();
                            return false;
                        }
                    });
                } else {
                    openInInappTab(data);
                }

                // Open special link in Chrome
            } else if (SPECIAL_LINK_HANDLING_OPTIONS == 2) {

                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(getResources().getColor(R.color.colorPrimaryDark));
                if (BuildConfig.IS_DEBUG_MODE) Log.d("TAG", " data " + data);
                WebView newWebView = new WebView(view.getContext());
                newWebView.setWebChromeClient(new WebChromeClient());
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
            }

            if (BuildConfig.IS_DEBUG_MODE) Log.d("TAG", " running this main activity ");
            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, " onJsalert");
            return super.onJsAlert(view, url, message, result);
        }

        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            mUM = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/pdf", "image/*"};
            i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            startActivityForResult(Intent.createChooser(i, "Upload"), FCR);
        }

        @SuppressLint("InlinedApi")
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

            boolean hasStoragePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                    (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

            boolean hasCameraPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
//            if (mUMA != null) {
//                mUMA.onReceiveValue(null);
//            }
            mUMA = filePathCallback;
            if (hasStoragePermission && hasCameraPermission) {
                openFilePicker(fileChooserParams);
                return true;
            } else {
                MainActivity.this.fileChooserParams = fileChooserParams;
                if (BuildConfig.IS_DEBUG_MODE)
                    Log.d(TAG, "File Chooser permissions not granted - requesting permissions");
                ArrayList<String> permissionList = new ArrayList<>();
                if (!hasCameraPermission && Config.requireCamera) {
                    permissionList.add(Manifest.permission.CAMERA);
                }

                if (!hasStoragePermission && Config.requireStorage) {
                    permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                    permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }

                if (!permissionList.isEmpty()) {
                    requestPermissions(permissionList.toArray(new String[0]), REQUEST_PERMISSION_STORAGE_CAMERA);
                }
                return true;
            }
        }


        protected void openFileChooser(ValueCallback<Uri> uploadMsg) {
            mUploadMessage = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/pdf", "image/*"};
            i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
        }

    }

    WebChromeClient.FileChooserParams fileChooserParams;

    private void openFilePicker(WebChromeClient.FileChooserParams fileChooserParams) {

        if (Arrays.asList(fileChooserParams.getAcceptTypes()).contains("audio/*")) {
            Intent chooserIntent = fileChooserParams.createIntent();
            startActivityForResult(chooserIntent, CODE_AUDIO_CHOOSER);
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                takePictureIntent.putExtra("PhotoPath", mCM);
            } catch (IOException ex) {
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Image file creation failed", ex);
            }
            if (photoFile != null) {
                mCM = "file:" + photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".provider", photoFile));
            } else {
                takePictureIntent = null;
            }
        }
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
            File videoFile = null;
            try {
                videoFile = createVideoFile();
                takeVideoIntent.putExtra("PhotoPath", mVM);
            } catch (IOException ex) {
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Video file creation failed", ex);
            }
            if (videoFile != null) {
                mVM = "file:" + videoFile.getAbsolutePath();
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".provider", videoFile));
            } else {
                takeVideoIntent = null;
            }
        }

        Intent contentSelectionIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        contentSelectionIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/* video/*");

        String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/pdf", "image/*", "video/*", "*/*"};
        contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        Intent[] intentArray;
        if (takePictureIntent != null && takeVideoIntent != null) {
            intentArray = new Intent[]{takePictureIntent, takeVideoIntent};
        } else if (takePictureIntent != null) {
            intentArray = new Intent[]{takePictureIntent};
        } else if (takeVideoIntent != null) {
            intentArray = new Intent[]{takeVideoIntent};
        } else {
            intentArray = new Intent[0];
        }

        int pickerMode = Config.filePickerMode;
        boolean shouldUseCamera = pickerMode == 0 || pickerMode == 1;
        boolean shouldUseGallery = pickerMode == 0 || pickerMode == 2;
        if (shouldUseGallery) {
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Upload");
            if (shouldUseCamera) {
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            }
            startActivityForResult(chooserIntent, FCR);
        } else if (shouldUseCamera) {
            Intent chooserIntent = Intent.createChooser(takePictureIntent, "Capture Image or Video");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takeVideoIntent});
            startActivityForResult(chooserIntent, FCR);
        }
    }

    private class MyWebChromeClient extends WebChromeClient {

        private View mCustomView;
        private CustomViewCallback mCustomViewCallback;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;

        MyWebChromeClient() {
        }


        public Bitmap getDefaultVideoPoster() {
            if (mCustomView == null) {
                return null;
            }
            return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
        }

        public void onHideCustomView() {
            ((FrameLayout) getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(this.mOriginalOrientation);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
            webView.clearFocus();
        }

        public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = getRequestedOrientation();
            this.mCustomViewCallback = paramCustomViewCallback;

            if (Config.LANDSCAPE_FULLSCREEN_VIDEO) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            ((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
            getWindow().getDecorView().setSystemUiVisibility(3846);
        }

        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            requestLocationPermission(origin, callback);
            callback.invoke(origin, true, false);
        }

        boolean progressBarActive = false;

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "progress " + newProgress);

            //Activate progress bar if this is a new redirect
            if (ACTIVATE_PROGRESS_BAR && !progressBarActive) {
                progressBar.setVisibility(View.VISIBLE);
                progressBarActive = true;
            }

            isRedirected = true;
            String name = preferences.getString("proshow", "");

            if (ACTIVATE_PROGRESS_BAR && name.equals("show")) {
                progressBar.setVisibility(View.VISIBLE);
            }

            if (newProgress >= 80 && ACTIVATE_PROGRESS_BAR && progressBarActive) {
                /* remove progress bar when page has been loaded 80%,
                 since the frame will likely have already changed to new page
                 otherwise, the spinner will still be visible
                 while non-critical resources load in background*/
                progressBar.setVisibility(View.GONE);
                progressBarActive = false;
            }

            if (newProgress == 100) {
                isRedirected = false;
                mAdView.setVisibility(View.VISIBLE);
                webView.setVisibility(View.VISIBLE);
            }

            if (!ACTIVATE_PROGRESS_BAR) {
                progressBar.setVisibility(View.GONE);
                progressBarActive = false;
            }
        }

        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            permissionRequest = request;
            for (String permission : request.getResources()) {
                if (Config.requireRecordAudio && permission.equalsIgnoreCase(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    askForPermission(request.getOrigin().toString(), Manifest.permission.RECORD_AUDIO, WEBVIEW_PERMISSION_REQUEST);
                }
                if (Config.requireCamera && permission.equalsIgnoreCase(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    askForPermission(request.getOrigin().toString(), Manifest.permission.CAMERA, WEBVIEW_PERMISSION_REQUEST);
                }
                Log.e(TAG, "onPermissionRequest: " + permission);
            }
        }
    }

    private PermissionRequest permissionRequest = null;

    public void askForPermission(String origin, String permission, int requestCode) {

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {
                if (permission.equalsIgnoreCase(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    showNoPermissionMessage(Manifest.permission.RECORD_AUDIO);
                } else if (permission.equalsIgnoreCase(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    showNoPermissionMessage(Manifest.permission.CAMERA);
                }
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            }
        } else {
            permissionRequest.grant(permissionRequest.getResources());
        }
    }

    private void webViewSetting(WebView intWebView) {

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(intWebView, true);
        }

        WebSettings webSettings = intWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        if (Config.CLEAR_CACHE_ON_STARTUP) {
            //webSettings.setAppCacheEnabled(false);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        } else {
            //webSettings.setAppCacheEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(intWebView, true);
        }
        intWebView.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webSettings.setSupportMultipleWindows(true);
        webSettings.setUseWideViewPort(true);

        if (!Config.USER_AGENT.isEmpty()) {
            webSettings.setUserAgentString(Config.USER_AGENT);
        } else {
            webSettings.setUserAgentString(webSettings.getUserAgentString().replace("wv", ""));
        }


    }

    // nfc

    private void initNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            readFromIntent(getIntent());
            pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            tagDetected.addCategory(CATEGORY_DEFAULT);
            writeTagFilters = new IntentFilter[]{tagDetected};
        }
    }


    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];

                }
            }
            read(msgs);
        }
    }

    private void read(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
//        String tagId = new String(msgs[0].getRecords()[0].getType());
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");

        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);

            webView.loadUrl("javascript: readNFCResult('" + text + "');");


        } catch (UnsupportedEncodingException e) {
            if (BuildConfig.IS_DEBUG_MODE) Log.d("UnsupportedEncoding", e.toString());
        }

        TextView textView = new TextView(this);
        textView.setPadding(16, 16, 16, 16);
        textView.setTextColor(Color.BLUE);
        textView.setText("read : " + text);

    }

    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = {createRecord(text)};
        NdefMessage message = new NdefMessage(records);
        writeData(tag, message);

    }

    public void writeData(Tag tag, NdefMessage message) {
        if (tag != null) {
            try {
                Ndef ndefTag = Ndef.get(tag);
                if (ndefTag == null) {
                    // Let's try to format the Tag in NDEF
                    NdefFormatable nForm = NdefFormatable.get(tag);
                    if (nForm != null) {
                        nForm.connect();
                        nForm.format(message);
                        nForm.close();
                        toast(WRITE_SUCCESS);
                    }
                } else {
                    ndefTag.connect();
                    ndefTag.writeNdefMessage(message);
                    ndefTag.close();
                    toast(WRITE_SUCCESS);
                }
            } catch (Exception e) {
                e.printStackTrace();
                toast("write error : " + e.getMessage());
            }
        }
    }

//    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {

//    }

    private NdefRecord createRecord(String text)
            throws UnsupportedEncodingException {

        if (text.startsWith("VCARD")) {

            String nameVcard = "BEGIN:" +
                    text.replace('_', '\n').replace("%20", " ")
                    + '\n' + "END:VCARD";

            byte[] uriField = nameVcard.getBytes(StandardCharsets.US_ASCII);
            byte[] payload = new byte[uriField.length + 1];              //add 1 for the URI Prefix
            //payload[0] = 0x01;                                      //prefixes http://www. to the URI
            System.arraycopy(uriField, 0, payload, 1, uriField.length);  //appends URI to payload

            NdefRecord nfcRecord = new NdefRecord(
                    NdefRecord.TNF_MIME_MEDIA, "text/vcard".getBytes(), new byte[0], payload);

//        byte[] vCardDataBytes = nameVcard.getBytes(Charset.forName("UTF-8"));
//        byte[] vCardPayload = new byte[vCardDataBytes.length+1];
//        System.arraycopy(vCardDataBytes, 0, vCardPayload, 1, vCardDataBytes.length);
//// vCardDataBytes[0] = (byte)0x00;
//        NdefRecord nfcRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,"text/x-vcard".getBytes(),new byte[] {}, vCardPayload);

            return nfcRecord;
        }

        //Intent intent = getIntent();
        //EditText editTextWeb = (EditText)

        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);

        return recordNFC;
    }

    private String getDeepLinkingURL(Intent intent) {
        if (intent.getData() != null
                && (intent.getData().getScheme().equals("http") || intent.getData().getScheme().equals("https"))) {
            Uri data = intent.getData();

            List<String> pathSegments = data.getPathSegments();
            if (pathSegments.size() > 0) {
                String hostOfLink = data.getHost();

                // Validate the host against expected value (e.g., your app's host)
                if (hostOfLink.contains(Config.HOST)) {
                    String localDeepLinkingURL;

                    // Reconstruct the URL for WebView
                    localDeepLinkingURL = data.getScheme() + "://" + data.getHost() + data.getPath();

                    String normalizeString = data.normalizeScheme().toString();
                    if (normalizeString.length() == localDeepLinkingURL.length()) {
                        localDeepLinkingURL = sanitizeURL(localDeepLinkingURL);
                    } else {
                        localDeepLinkingURL = sanitizeURL(normalizeString);
                    }

                    if (BuildConfig.IS_DEBUG_MODE)
                        Log.d(TAG, "getDeepLinkingURL: " + localDeepLinkingURL);

                    return localDeepLinkingURL;
                }
            }
        }
        return null;
    }

    // Sanitize URL to prevent XSS
    private String sanitizeURL(String url) {
        // Apply a whitelist approach to allow only known safe characters and patterns
        // Remove any characters that are not alphanumeric, forward slash, or colon.
        String localURL = url.replaceAll("[^-%=&|?_.a-zA-Z\\d/:]", "");

        // Apply Content Security Policy (CSP) if applicable
        return applyCSP(localURL);
    }

    // Apply Content Security Policy (CSP) to the URL if necessary
    private String applyCSP(String url) {
        // This method can add CSP headers or modify the URL as per your app's requirements

        // Add a CSP header to restrict inline scripts and unsafe sources
        String cspHeaderValue = "default-src 'self'; script-src 'self' 'unsafe-inline'; object-src 'none'; style-src 'self' 'unsafe-inline'; img-src 'self'; media-src 'self'; frame-src 'none'; font-src 'self'; connect-src 'self';";

        // Append the CSP header to the URL as a query parameter
        return url + "?CSP_HEADER=" + Uri.encode(cspHeaderValue);
    }


    @Override
    protected void onNewIntent(Intent intent) {

        if (intent != null) {
            // Check for URL in the extras bundle
            Bundle extras = intent.getExtras();
            String URL = null;
            if (extras != null) {
                URL = extras.getString("ONESIGNAL_URL");
                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "applinkURL [1]: From extras = " + URL);
            }
            // Check for URL in the intent's data
            if (intent.getDataString() != null) {
                URL = intent.getDataString();
                if (BuildConfig.IS_DEBUG_MODE)
                    Log.d(TAG, "applinkURL [1]: From intent data = " + URL);
            }
            // Handle the URL if it's found
            if (URL != null) {
                handleURL(URL);
            } else {
                if (BuildConfig.IS_DEBUG_MODE)
                    Log.d(TAG, "applinkURL [3]: No App Links URL found in intent");
            }
        }
        if (!readModeNFC && !writeModeNFC) {
            return;
        }
        super.onNewIntent(intent);
        setIntent(intent);
        if (readModeNFC) {
            readFromIntent(intent);
        }
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            toast("tag detected : " + myTag.toString());


            try {
                if (writeModeNFC) {
                    write(textToWriteNFC, myTag);
                }
            } catch (IOException | FormatException e) {
                e.printStackTrace();
                Toast.makeText(this, WRITE_ERROR, Toast.LENGTH_LONG).show();
            }
        }
        handleSharedIntent(intent);
    }

    public void ClosePopupWindow(View view) {

        progressBar.setVisibility(View.GONE);
        preferences = getPreferences(MODE_PRIVATE);
        preferences.edit().putString("proshow", "noshow").apply();
        mContainer.removeAllViews();
        windowContainer.setVisibility(View.GONE);
        mWebviewPop.destroy();

    }

    private void WriteModeOn() {
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void ExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(getResources().getString(R.string.exit_app_dialog))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.finish();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
            }
        });
        alert.show();
    }


    private void setOfflineScreenBackgroundColor() {
        offlineLayout.getBackground().setColorFilter(Color.parseColor(Config.OFFLINE_SCREEN_BACKGROUND_COLOR), PorterDuff.Mode.DARKEN);
    }

    boolean shouldAlwaysOpenInInappTab(String URL) {
        for (int i = 0; i < Config.ALWAYS_OPEN_IN_INAPP_TAB.length; i++) {
            if ((Config.ALWAYS_OPEN_IN_INAPP_TAB[i] != "") && (URL.startsWith(Config.ALWAYS_OPEN_IN_INAPP_TAB[i]))) {
                return true;
            }
        }
        return false;
    }

    void openInInappTab(String URL) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(getResources().getColor(R.color.colorPrimaryDark));
        CustomTabsIntent customTabsIntent = builder.build();
        // Retrieve cookies from WebView & set cookies in the customTabsIntent WebView
        CookieManager cookieManager = CookieManager.getInstance();
        String allCookies = cookieManager.getCookie(URL);
        if (allCookies != null) {
            String[] cookieList = allCookies.split(";");
            for (String cookie : cookieList) {
                customTabsIntent.intent.putExtra("android.webkit.CookieManager.COOKIE", cookie.trim());
            }
        }
        customTabsIntent.launchUrl(MainActivity.this, Uri.parse(URL));
        webView.stopLoading();
    }

    void openInNewBrowser(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(i);
    }
}
