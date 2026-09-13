package com.viyzo.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.MediaController;

public class MainActivity extends Activity {

    private static final int PICK_VIDEO = 101;

    private VideoView videoView;
    private Uri selectedVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showHome();
    }

    private void showHome() {

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(Color.BLACK);
        main.setPadding(12, 12, 12, 12);

        TextView title = new TextView(this);
        title.setText("VIYZO");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 15);

        main.addView(title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

        TextView homeText = new TextView(this);
        homeText.setText("HOME");
        homeText.setTextColor(Color.WHITE);
        homeText.setTextSize(20);
        homeText.setGravity(Gravity.CENTER);
        homeText.setPadding(0, 5, 0, 10);

        main.addView(homeText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

        videoView = new VideoView(this);
        videoView.setBackgroundColor(Color.BLACK);

        LinearLayout.LayoutParams videoParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                );

        main.addView(videoView, videoParams);

        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoView);
        videoView.setMediaController(controller);

        Button uploadButton = new Button(this);
        uploadButton.setText("UPLOAD VIDEO");
        uploadButton.setTextSize(17);

        uploadButton.setOnClickListener(v -> openVideoPicker());

        main.addView(uploadButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

        setContentView(main);

        if (selectedVideo != null) {
            playSelectedVideo();
        }
    }

    private void openVideoPicker() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        startActivityForResult(intent, PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getData() != null) {

            selectedVideo = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(
                        selectedVideo,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (Exception ignored) {
            }

            showHome();
        }
    }

    private void playSelectedVideo() {

        if (selectedVideo == null || videoView == null) {
            return;
        }

        videoView.setVideoURI(selectedVideo);

        videoView.setOnPreparedListener(mp -> {

            mp.setLooping(true);

            videoView.start();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {

            return false;
        });

        videoView.requestFocus();
    }

    @Override
    protected void onPause() {

        super.onPause();

        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (videoView != null &&
                selectedVideo != null) {

            videoView.start();
        }
    }

    @Override
    protected void onDestroy() {

        if (videoView != null) {
            videoView.stopPlayback();
        }

        super.onDestroy();
    }
}
