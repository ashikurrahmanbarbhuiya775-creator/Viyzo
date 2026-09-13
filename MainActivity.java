package com.viyzo.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.content.Intent;
import android.net.Uri;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends Activity {

    private FirebaseAuth auth;
    private LinearLayout root;

    private SurfaceView videoSurface;
    private MediaPlayer mediaPlayer;
    private Uri selectedVideoUri;

    private boolean surfaceReady = false;
    private boolean videoPlayingScreen = false;

    private static final int PICK_VIDEO = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            showHome();
        } else {
            showLogin();
        }
    }

    // =========================
    // COMMON UI
    // =========================

    private LinearLayout baseRoot() {
        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        layout.setPadding(35, 45, 35, 30);
        layout.setBackgroundColor(Color.rgb(18, 17, 22));

        return layout;
    }

    private TextView title(String text) {
        TextView t = new TextView(this);

        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(32);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 0, 0, 25);

        return t;
    }

    private TextView message(String text) {
        TextView t = new TextView(this);

        t.setText(text);
        t.setTextColor(Color.LTGRAY);
        t.setTextSize(17);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 0, 0, 20);

        return t;
    }

    private EditText input(String hint, boolean password) {
        EditText e = new EditText(this);

        e.setHint(hint);
        e.setHintTextColor(Color.GRAY);
        e.setTextColor(Color.WHITE);
        e.setTextSize(17);
        e.setSingleLine(true);
        e.setPadding(20, 12, 20, 12);

        if (password) {
            e.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            );
        } else {
            e.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        }

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        lp.setMargins(0, 6, 0, 6);
        e.setLayoutParams(lp);

        return e;
    }

    private Button button(String text) {
        Button b = new Button(this);

        b.setText(text);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(Color.BLACK);
        b.setBackgroundColor(Color.rgb(225, 225, 225));

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        lp.setMargins(0, 8, 0, 8);
        b.setLayoutParams(lp);

        return b;
    }

    // =========================
    // LOGIN
    // =========================

    private void showLogin() {

        stopVideo();

        root = baseRoot();

        root.addView(title("VIYZO"));
        root.addView(message("Login to continue"));

        EditText email = input("Email", false);
        EditText password = input("Password", true);

        root.addView(email);
        root.addView(password);

        Button login = button("LOGIN");
        Button signup = button("CREATE NEW ACCOUNT");

        root.addView(login);
        root.addView(signup);

        login.setOnClickListener(v -> {

            String emailText = email.getText().toString().trim();
            String passwordText = password.getText().toString();

            if (emailText.isEmpty() || passwordText.isEmpty()) {
                toast("Email and password required");
                return;
            }

            login.setEnabled(false);

            auth.signInWithEmailAndPassword(
                    emailText,
                    passwordText
            )
            .addOnSuccessListener(result -> {

                login.setEnabled(true);

                showHome();
            })
            .addOnFailureListener(error -> {

                login.setEnabled(true);

                toast("Login failed: " + error.getMessage());
            });
        });

        signup.setOnClickListener(v -> showSignup());

        setContentView(root);
    }

    // =========================
    // SIGNUP
    // =========================

    private void showSignup() {

        stopVideo();

        root = baseRoot();

        root.addView(title("VIYZO"));
        root.addView(message("Create your account"));

        EditText name = input("Name", false);
        EditText email = input("Email", false);
        EditText password =
                input("Password (minimum 6 characters)", true);

        root.addView(name);
        root.addView(email);
        root.addView(password);

        Button create = button("SIGN UP");
        Button back = button("BACK TO LOGIN");

        root.addView(create);
        root.addView(back);

        create.setOnClickListener(v -> {

            String nameText = name.getText().toString().trim();
            String emailText = email.getText().toString().trim();
            String passwordText = password.getText().toString();

            if (nameText.isEmpty() ||
                    emailText.isEmpty() ||
                    passwordText.isEmpty()) {

                toast("Please fill all fields");
                return;
            }

            if (passwordText.length() < 6) {

                toast("Password must be at least 6 characters");
                return;
            }

            create.setEnabled(false);

            auth.createUserWithEmailAndPassword(
                    emailText,
                    passwordText
            )
            .addOnSuccessListener(result -> {

                create.setEnabled(true);

                showHome();
            })
            .addOnFailureListener(error -> {

                create.setEnabled(true);

                toast("Signup failed: " + error.getMessage());
            });
        });

        back.setOnClickListener(v -> showLogin());

        setContentView(root);
    }

    // =========================
    // HOME SCREEN
    // =========================

    private void showHome() {

        stopVideo();

        root = baseRoot();

        root.addView(title("VIYZO"));

        TextView status;

        if (selectedVideoUri == null) {
            status = message(
                    "Login successful\n\n" +
                    "Select a video to show it on the Home Screen."
            );
        } else {
            status = message(
                    "Video selected\n\n" +
                    "The video is ready to play."
            );
        }

        root.addView(status);

        // --------------------------------
        // HOME VIDEO PLAYER
        // --------------------------------

        if (selectedVideoUri != null) {

            videoSurface = new SurfaceView(this);

            videoSurface.setBackgroundColor(Color.BLACK);

            LinearLayout.LayoutParams videoParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            0
                    );

            videoParams.weight = 1;
            videoParams.setMargins(0, 5, 0, 10);

            videoSurface.setLayoutParams(videoParams);

            root.addView(videoSurface);

            videoSurface.getHolder().addCallback(
                    new SurfaceHolder.Callback() {

                @Override
                public void surfaceCreated(
                        SurfaceHolder holder) {

                    surfaceReady = true;

                    startVideo(holder, status, true);
                }

                @Override
                public void surfaceChanged(
                        SurfaceHolder holder,
                        int format,
                        int width,
                        int height) {

                    if (mediaPlayer != null) {

                        try {
                            mediaPlayer.setDisplay(holder);
                        } catch (Exception ignored) {
                        }
                    }
                }

                @Override
                public void surfaceDestroyed(
                        SurfaceHolder holder) {

                    surfaceReady = false;

                    if (mediaPlayer != null) {

                        try {
                            mediaPlayer.setDisplay(null);
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        }

        // --------------------------------
        // BUTTONS
        // --------------------------------

        Button select =
                button("UPLOAD / SELECT VIDEO");

        Button view =
                button("OPEN VIDEO SCREEN");

        Button logout =
                button("LOGOUT");

        root.addView(select);
        root.addView(view);
        root.addView(logout);

        select.setOnClickListener(v -> selectVideo());

        view.setOnClickListener(v -> {

            if (selectedVideoUri == null) {

                toast("First select a video");

                return;
            }

            showVideoScreen();
        });

        logout.setOnClickListener(v -> {

            stopVideo();

            selectedVideoUri = null;

            auth.signOut();

            showLogin();
        });

        setContentView(root);
    }

    // =========================
    // SELECT VIDEO
    // =========================

    private void selectVideo() {

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType("video/*");

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        intent.addFlags(
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        try {

            startActivityForResult(
                    intent,
                    PICK_VIDEO
            );

        } catch (Exception e) {

            Intent fallback =
                    new Intent(Intent.ACTION_GET_CONTENT);

            fallback.addCategory(
                    Intent.CATEGORY_OPENABLE
            );

            fallback.setType("video/*");

            fallback.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivityForResult(
                    fallback,
                    PICK_VIDEO
            );
        }
    }

    // =========================
    // VIDEO SELECT RESULT
    // =========================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == PICK_VIDEO &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getData() != null) {

            selectedVideoUri = data.getData();

            try {

                getContentResolver()
                        .takePersistableUriPermission(
                                selectedVideoUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );

            } catch (Exception ignored) {
            }

            toast("Video selected successfully");

            // वापस Home पर जाएँ और वहीं वीडियो चलाएँ।
            showHome();
        }
    }

    // =========================
    // SEPARATE VIDEO SCREEN
    // =========================

    private void showVideoScreen() {

        stopVideo();

        videoPlayingScreen = true;
        surfaceReady = false;

        root = baseRoot();

        root.setPadding(0, 20, 0, 20);

        TextView heading =
                title("VIYZO VIDEO");

        heading.setPadding(0, 5, 0, 10);

        root.addView(heading);

        if (selectedVideoUri == null) {

            root.addView(
                    message("No video selected.")
            );

            Button back =
                    button("BACK TO HOME");

            root.addView(back);

            back.setOnClickListener(
                    v -> showHome()
            );

            setContentView(root);

            return;
        }

        videoSurface =
                new SurfaceView(this);

        videoSurface.setBackgroundColor(
                Color.BLACK
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0
                );

        params.weight = 1;

        params.setMargins(
                0,
                5,
                0,
                10
        );

        videoSurface.setLayoutParams(params);

        root.addView(videoSurface);

        TextView status =
                message("Preparing video...");

        status.setTextSize(14);

        status.setPadding(
                5,
                0,
                5,
                5
        );

        root.addView(status);

        Button back =
                button("BACK TO HOME");

        root.addView(back);

        videoSurface.getHolder().addCallback(
                new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(
                    SurfaceHolder holder) {

                surfaceReady = true;

                startVideo(
                        holder,
                        status,
                        false
                );
            }

            @Override
            public void surfaceChanged(
                    SurfaceHolder holder,
                    int format,
                    int width,
                    int height) {

                if (mediaPlayer != null) {

                    try {
                        mediaPlayer.setDisplay(holder);
                    } catch (Exception ignored) {
                    }
                }
            }

            @Override
            public void surfaceDestroyed(
                    SurfaceHolder holder) {

                surfaceReady = false;

                if (mediaPlayer != null) {

                    try {
                        mediaPlayer.setDisplay(null);
                    } catch (Exception ignored) {
                    }
                }
            }
        });

        back.setOnClickListener(
                v -> showHome()
        );

        setContentView(root);
    }

    // =========================
    // START VIDEO
    // =========================

    private void startVideo(
            SurfaceHolder holder,
            TextView status,
            boolean homeVideo) {

        if (!surfaceReady ||
                selectedVideoUri == null) {

            return;
        }

        releaseMediaPlayer();

        mediaPlayer =
                new MediaPlayer();

        try {

            mediaPlayer.setAudioStreamType(
                    AudioManager.STREAM_MUSIC
            );

            // सबसे जरूरी लाइन:
            // Video decoder को Surface से जोड़ना।
            mediaPlayer.setDisplay(holder);

            // Selected local video खोलना।
            mediaPlayer.setDataSource(
                    this,
                    selectedVideoUri
            );

            // Video बार-बार शुरू नहीं होगा।
            mediaPlayer.setLooping(false);

            try {

                mediaPlayer.setVideoScalingMode(
                        MediaPlayer
                                .VIDEO_SCALING_MODE_SCALE_TO_FIT
                );

            } catch (Exception ignored) {
            }

            mediaPlayer.setOnPreparedListener(
                    mp -> {

                try {

                    if (homeVideo) {

                        status.setText(
                                "Video playing on Home Screen"
                        );

                    } else {

                        status.setText(
                                "Video playing"
                        );
                    }

                    // Surface फिर से attach करें।
                    mp.setDisplay(holder);

                    // आवाज़ ON।
                    mp.setVolume(1.0f, 1.0f);

                    mp.start();

                } catch (Exception e) {

                    status.setText(
                            "Video start failed"
                    );

                    toast(
                            "Video start error"
                    );
                }
            });

            mediaPlayer.setOnVideoSizeChangedListener(
                    (mp, width, height) -> {

                try {

                    mp.setDisplay(holder);

                } catch (Exception ignored) {
                }
            });

            mediaPlayer.setOnCompletionListener(
                    mp -> {

                status.setText(
                        "Video finished"
                );
            });

            mediaPlayer.setOnErrorListener(
                    (mp, what, extra) -> {

                status.setText(
                        "Video cannot be played"
                );

                toast(
                        "This video format is not supported"
                );

                return true;
            });

            // Async preparation से UI freeze नहीं होगा।
            mediaPlayer.prepareAsync();

        } catch (Exception e) {

            status.setText(
                    "Unable to open video"
            );

            toast(
                    "Video error: " +
                    e.getMessage()
            );

            releaseMediaPlayer();
        }
    }

    // =========================
    // RELEASE PLAYER
    // =========================

    private void releaseMediaPlayer() {

        if (mediaPlayer != null) {

            try {
                mediaPlayer.setDisplay(null);
            } catch (Exception ignored) {
            }

            try {

                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }

            } catch (Exception ignored) {
            }

            try {
                mediaPlayer.reset();
            } catch (Exception ignored) {
            }

            try {
                mediaPlayer.release();
            } catch (Exception ignored) {
            }

            mediaPlayer = null;
        }
    }

    private void stopVideo() {

        surfaceReady = false;
        videoPlayingScreen = false;

        releaseMediaPlayer();

        videoSurface = null;
    }

    // =========================
    // BACK / LIFECYCLE
    // =========================

    @Override
    protected void onPause() {

        super.onPause();

        if (mediaPlayer != null) {

            try {

                if (mediaPlayer.isPlaying()) {

                    mediaPlayer.pause();
                }

            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onDestroy() {

        stopVideo();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if (videoPlayingScreen) {

            showHome();

        } else {

            super.onBackPressed();
        }
    }

    // =========================
    // TOAST
    // =========================

    private void toast(String text) {

        Toast.makeText(
                this,
                text,
                Toast.LENGTH_SHORT
        ).show();
    }
}
