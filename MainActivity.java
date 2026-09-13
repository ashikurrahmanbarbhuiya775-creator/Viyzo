package com.viyzo.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.media.MediaPlayer;

public class MainActivity extends Activity {

    private static final int PICK_VIDEO = 100;

    private LinearLayout mainLayout;
    private SurfaceView videoView;
    private MediaPlayer player;
    private Uri videoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private void showHome() {

        releasePlayer();

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.BLACK);
        mainLayout.setPadding(12, 12, 12, 12);

        TextView title = new TextView(this);
        title.setText("VIYZO");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 10);

        mainLayout.addView(title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

        videoView = new SurfaceView(this);
        videoView.setBackgroundColor(Color.BLACK);

        mainLayout.addView(videoView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                ));

        Button uploadButton = new Button(this);
        uploadButton.setText("UPLOAD VIDEO");
        uploadButton.setTextSize(17);

        uploadButton.setOnClickListener(v -> openVideoPicker());

        mainLayout.addView(uploadButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

        setContentView(mainLayout);

        videoView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                if (videoUri != null) {
                    playVideo(holder);
                }
            }

            @Override
            public void surfaceChanged(
                    SurfaceHolder holder,
                    int format,
                    int width,
                    int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

                releasePlayer();
            }
        });
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

            videoUri = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(
                        videoUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (Exception ignored) {
            }

            showHome();
        }
    }

    private void playVideo(SurfaceHolder holder) {

        if (videoUri == null) {
            return;
        }

        releasePlayer();

        try {

            player = new MediaPlayer();

            player.setDataSource(
                    this,
                    videoUri
            );

            // वीडियो की तस्वीर SurfaceView पर दिखेगी
            player.setDisplay(holder);

            player.setAudioStreamType(
                    android.media.AudioManager.STREAM_MUSIC
            );

            player.setVolume(1.0f, 1.0f);

            player.setLooping(true);

            player.setVideoScalingMode(
                    MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
            );

            player.setOnPreparedListener(mp -> {
                mp.start();
            });

            player.setOnErrorListener((mp, what, extra) -> {
                return false;
            });

            player.prepareAsync();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releasePlayer() {

        if (player != null) {

            try {
                player.stop();
            } catch (Exception ignored) {
            }

            try {
                player.reset();
            } catch (Exception ignored) {
            }

            try {
                player.release();
            } catch (Exception ignored) {
            }

            player = null;
        }
    }

    @Override
    protected void onDestroy() {

        releasePlayer();

        super.onDestroy();
    }
}
