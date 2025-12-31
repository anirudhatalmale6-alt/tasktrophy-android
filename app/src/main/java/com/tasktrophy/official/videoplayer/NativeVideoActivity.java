package com.tasktrophy.official.videoplayer;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.tasktrophy.official.R;

public class NativeVideoActivity extends AppCompatActivity {
    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_native_video);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        String videoUrl = getIntent().getStringExtra("video_url");
        videoView = findViewById(R.id.video_view);
        videoView.setOnCompletionListener(mp -> finish());
        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);

        ConstraintLayout actionBar = findViewById(R.id.topBar);
        ImageView btnBack = findViewById(R.id.ivBackButton);
        ImageView btnEnterPiP = findViewById(R.id.ivEnterPIP);
        btnBack.setOnClickListener(v -> finish());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnEnterPiP.setVisibility(View.VISIBLE);
            btnEnterPiP.setOnClickListener(v -> enterPiPMode());
        } else {
            btnEnterPiP.setVisibility(View.GONE);
        }

        addOnPictureInPictureModeChangedListener(pictureInPictureModeChangedInfo -> {
            boolean isPiPModeActivate = pictureInPictureModeChangedInfo.isInPictureInPictureMode();
            if (isPiPModeActivate){
                actionBar.setVisibility(View.GONE);
            } else {
                actionBar.setVisibility(View.VISIBLE);
            }
        });

        loadViewPlayer(videoUrl);
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String videoUrl = getIntent().getStringExtra("video_url");
        loadViewPlayer(videoUrl);
    }

    private void loadViewPlayer(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()){
            finish();
            return;
        }
        videoView.setVideoURI(Uri.parse(videoUrl));
        videoView.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        enterPiPMode(); // triggers on Home button
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void enterPiPMode() {
        PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder();
        enterPictureInPictureMode(pipBuilder.build());
    }
}