package com.webviewgold.myappname;

import android.app.Activity;
import android.view.View;

public class SystemBarUtils {

    public interface InsetsCallback {
        void onInsetsAvailable(Insets insets);
    }

    public static void getSafeInsetsAsync(Activity activity, InsetsCallback callback) {
        Insets insets = new Insets();

        View rootView = activity.getWindow().getDecorView();
        rootView.setOnApplyWindowInsetsListener((v, windowInsets) -> {
            insets.top = windowInsets.getSystemWindowInsetTop();
            insets.bottom = windowInsets.getSystemWindowInsetBottom();
            insets.left = windowInsets.getSystemWindowInsetLeft();
            insets.right = windowInsets.getSystemWindowInsetRight();
            callback.onInsetsAvailable(insets);
            return windowInsets;
        });
        rootView.requestApplyInsets(); // Trigger the insets delivery
    }

    public static class Insets {
        public int top, bottom, left, right;
    }
}

