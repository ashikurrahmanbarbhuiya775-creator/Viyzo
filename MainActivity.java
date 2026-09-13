package com.viyzo.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private SurfaceView videoSurface;
    private SurfaceHolder surfaceHolder;
    private android.media.MediaPlayer mediaPlayer;
    private Surface currentSurface;

    private Uri selectedVideoUri;

    private SharedPreferences prefs;

    private static final int PICK_VIDEO = 5001;

    private LinearLayout root;
    private TextView statusText;

    private String currentVideoId = "";

    private int page = 0;
    private static final int PAGE_HOME = 1;
    private static final int PAGE_VIDEO = 2;

    private int WHITE = Color.WHITE;
    private int BLACK = Color.BLACK;
    private int DARK = Color.rgb(18, 18, 22);
    private int CARD = Color.rgb(30, 30, 36);
    private int GRAY = Color.rgb(180, 180, 185);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        prefs = getSharedPreferences("viyzo_settings", MODE_PRIVATE);

        String savedVideo = prefs.getString("selected_video", "");

        if (!savedVideo.isEmpty()) {
            try {
                selectedVideoUri = Uri.parse(savedVideo);
            } catch (Exception ignored) {
            }
        }

        if (auth.getCurrentUser() == null) {
            showLogin();
        } else {
            showHome();
        }
    }

    // =========================================================
    // BASIC UI HELPERS
    // =========================================================

    private TextView text(String value, float size) {

        TextView t = new TextView(this);

        t.setText(value);
        t.setTextColor(WHITE);
        t.setTextSize(size);
        t.setPadding(16, 12, 16, 12);

        return t;
    }

    private Button button(String value) {

        Button b = new Button(this);

        b.setText(value);
        b.setTextSize(15);
        b.setAllCaps(false);

        return b;
    }

    private void baseRoot() {

        root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(DARK);

        root.setPadding(12, 12, 12, 12);

        ScrollView scroll = new ScrollView(this);

        scroll.setFillViewport(true);

        scroll.addView(root);

        setContentView(scroll);
    }

    private void title(String value) {

        TextView t = text(value, 28);

        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);

        root.addView(
                t,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
    }

    private void message(String value) {

        TextView t = text(value, 14);

        t.setTextColor(GRAY);

        root.addView(
                t,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
    }

    // =========================================================
    // LOGIN
    // =========================================================

    private void showLogin() {

        stopVideo();

        baseRoot();

        title("VIYZO");

        TextView heading = text("LOGIN", 22);
        heading.setGravity(Gravity.CENTER);

        root.addView(heading);

        final EditText email = new EditText(this);
        email.setHint("Email");
        email.setTextColor(WHITE);
        email.setHintTextColor(GRAY);

        root.addView(email);

        final EditText password = new EditText(this);
        password.setHint("Password");
        password.setTextColor(WHITE);
        password.setHintTextColor(GRAY);
        password.setInputType(0x81);

        root.addView(password);

        Button login = button("LOGIN");

        root.addView(login);

        Button signup = button("CREATE NEW ACCOUNT");

        root.addView(signup);

        TextView result = text("", 14);
        result.setTextColor(GRAY);

        root.addView(result);

        login.setOnClickListener(v -> {

            String e = email.getText().toString().trim();
            String p = password.getText().toString();

            if (e.isEmpty() || p.isEmpty()) {
                result.setText("Email और password भरें।");
                return;
            }

            result.setText("Login हो रहा है...");

            auth.signInWithEmailAndPassword(e, p)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            showHome();

                        } else {

                            String msg = "Login failed";

                            if (task.getException() != null) {
                                msg = task.getException().getMessage();
                            }

                            result.setText(msg);
                        }
                    });
        });

        signup.setOnClickListener(v -> showSignup());
    }

    // =========================================================
    // SIGNUP
    // =========================================================

    private void showSignup() {

        stopVideo();

        baseRoot();

        title("VIYZO");

        TextView heading = text("SIGN UP", 22);
        heading.setGravity(Gravity.CENTER);

        root.addView(heading);

        final EditText name = new EditText(this);
        name.setHint("Your Name");
        name.setTextColor(WHITE);
        name.setHintTextColor(GRAY);

        root.addView(name);

        final EditText email = new EditText(this);
        email.setHint("Email");
        email.setTextColor(WHITE);
        email.setHintTextColor(GRAY);

        root.addView(email);

        final EditText password = new EditText(this);
        password.setHint("Password");
        password.setTextColor(WHITE);
        password.setHintTextColor(GRAY);
        password.setInputType(0x81);

        root.addView(password);

        Button create = button("CREATE ACCOUNT");

        root.addView(create);

        Button back = button("BACK TO LOGIN");

        root.addView(back);

        TextView result = text("", 14);

        result.setTextColor(GRAY);

        root.addView(result);

        create.setOnClickListener(v -> {

            String n = name.getText().toString().trim();
            String e = email.getText().toString().trim();
            String p = password.getText().toString();

            if (n.isEmpty() || e.isEmpty() || p.isEmpty()) {

                result.setText("सारी जानकारी भरें।");
                return;
            }

            if (p.length() < 6) {

                result.setText("Password कम से कम 6 characters का रखें।");
                return;
            }

            result.setText("Account बनाया जा रहा है...");

            auth.createUserWithEmailAndPassword(e, p)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            FirebaseUser user = auth.getCurrentUser();

                            if (user != null) {

                                Map<String, Object> profile = new HashMap<>();

                                profile.put("uid", user.getUid());
                                profile.put("name", n);
                                profile.put("email", e);
                                profile.put("followers", 0);
                                profile.put("following", 0);
                                profile.put("createdAt",
                                        System.currentTimeMillis());

                                db.collection("users")
                                        .document(user.getUid())
                                        .set(profile);
                            }

                            showHome();

                        } else {

                            String msg = "Signup failed";

                            if (task.getException() != null) {
                                msg = task.getException().getMessage();
                            }

                            result.setText(msg);
                        }
                    });
        });

        back.setOnClickListener(v -> showLogin());
    }

    // =========================================================
    // HOME
    // =========================================================

    private void showHome() {

        page = PAGE_HOME;

        stopVideo();

        baseRoot();

        title("VIYZO");

        TextView home = text("HOME", 21);
        home.setGravity(Gravity.CENTER);

        root.addView(home);

        // ---------------- VIDEO AREA ----------------

        FrameLayout videoFrame = new FrameLayout(this);

        videoFrame.setBackgroundColor(Color.BLACK);

        LinearLayout.LayoutParams videoFrameParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(300)
                );

        root.addView(videoFrame, videoFrameParams);

        videoSurface = new SurfaceView(this);

        videoSurface.setBackgroundColor(Color.BLACK);

        videoFrame.addView(
                videoSurface,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        TextView videoHint = text("", 14);

        videoHint.setGravity(Gravity.CENTER);
        videoHint.setTextColor(WHITE);

        videoFrame.addView(
                videoHint,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        surfaceHolder = videoSurface.getHolder();

        surfaceHolder.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                currentSurface = holder.getSurface();

                if (selectedVideoUri != null) {

                    videoHint.setText("वीडियो लोड हो रहा है...");

                    playVideoOnSurface(
                            currentSurface,
                            videoHint
                    );
                } else {

                    videoHint.setText("कोई वीडियो चुना नहीं गया");
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

                if (currentSurface == holder.getSurface()) {
                    currentSurface = null;
                }

                stopVideo();
            }
        });

        // ---------------- UPLOAD ----------------

        Button upload = button("UPLOAD / SELECT VIDEO");

        root.addView(upload);

        upload.setOnClickListener(v -> openVideoPicker());

        // ---------------- VIDEO SCREEN ----------------

        Button viewVideo = button("OPEN VIDEO SCREEN");

        root.addView(viewVideo);

        viewVideo.setOnClickListener(v -> {

            if (selectedVideoUri == null) {

                Toast.makeText(
                        this,
                        "पहले वीडियो चुनें।",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            showVideoScreen();
        });

        // ---------------- ACTIONS ----------------

        Button like = button("❤️ LIKE VIDEO");

        root.addView(like);

        like.setOnClickListener(v -> likeVideo());

        Button comment = button("💬 COMMENT");

        root.addView(comment);

        comment.setOnClickListener(v -> addComment());

        Button share = button("📤 SHARE VIDEO");

        root.addView(share);

        share.setOnClickListener(v -> shareVideo());

        // ---------------- OTHER SCREENS ----------------

        Button messenger = button("💬 MESSENGER");

        root.addView(messenger);

        messenger.setOnClickListener(v -> showMessenger());

        Button notifications = button("🔔 NOTIFICATIONS");

        root.addView(notifications);

        notifications.setOnClickListener(v -> showNotifications());

        Button settings = button("⚙ SETTINGS");

        root.addView(settings);

        settings.setOnClickListener(v -> showSettings());

        Button userDashboard = button("👤 USER DASHBOARD");

        root.addView(userDashboard);

        userDashboard.setOnClickListener(v -> showUserDashboard());

        Button myDashboard = button("📊 MY DASHBOARD");

        root.addView(myDashboard);

        myDashboard.setOnClickListener(v -> showMyDashboard());

        Button logout = button("LOGOUT");

        root.addView(logout);

        logout.setOnClickListener(v -> {

            auth.signOut();

            stopVideo();

            showLogin();
        });

        message(
                "Viyzo local video player + Firebase account system"
        );
    }

    // =========================================================
    // VIDEO SCREEN
    // =========================================================

    private void showVideoScreen() {

        page = PAGE_VIDEO;

        stopVideo();

        baseRoot();

        title("VIYZO VIDEO");

        FrameLayout frame = new FrameLayout(this);

        frame.setBackgroundColor(Color.BLACK);

        root.addView(
                frame,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(500)
                )
        );

        SurfaceView surface = new SurfaceView(this);

        surface.setBackgroundColor(Color.BLACK);

        frame.addView(
                surface,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        TextView hint = text("वीडियो लोड हो रहा है...", 15);

        hint.setGravity(Gravity.CENTER);

        frame.addView(
                hint,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        SurfaceHolder holder = surface.getHolder();

        holder.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder h) {

                currentSurface = h.getSurface();

                if (selectedVideoUri != null) {

                    playVideoOnSurface(
                            h.getSurface(),
                            hint
                    );
                }
            }

            @Override
            public void surfaceChanged(
                    SurfaceHolder h,
                    int format,
                    int width,
                    int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder h) {

                if (currentSurface == h.getSurface()) {
                    currentSurface = null;
                }

                stopVideo();
            }
        });

        Button back = button("← BACK TO HOME");

        root.addView(back);

        back.setOnClickListener(v -> showHome());

        Button like = button("❤️ LIKE");

        root.addView(like);

        like.setOnClickListener(v -> likeVideo());

        Button comment = button("💬 COMMENT");

        root.addView(comment);

        comment.setOnClickListener(v -> addComment());

        Button share = button("📤 SHARE");

        root.addView(share);

        share.setOnClickListener(v -> shareVideo());
    }

    // =========================================================
    // VIDEO PICKER
    // =========================================================

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

            prefs.edit()
                    .putString(
                            "selected_video",
                            selectedVideoUri.toString()
                    )
                    .apply();

            saveVideoToFirestore();

            Toast.makeText(
                    this,
                    "वीडियो चुना गया।",
                    Toast.LENGTH_SHORT
            ).show();

            showHome();
        }
    }

    // =========================================================
    // STRONG VIDEO PLAYER
    // =========================================================

    private void playVideoOnSurface(
            Surface surface,
            TextView hint) {

        if (selectedVideoUri == null ||
                surface == null) {

            return;
        }

        stopVideo();

        try {

            mediaPlayer =
                    new android.media.MediaPlayer();

            mediaPlayer.setAudioStreamType(
                    android.media.AudioManager.STREAM_MUSIC
            );

            mediaPlayer.setDataSource(
                    this,
                    selectedVideoUri
            );

            mediaPlayer.setSurface(surface);

            mediaPlayer.setLooping(true);

            mediaPlayer.setOnPreparedListener(mp -> {

                try {

                    mp.setLooping(true);

                    mp.setVideoScalingMode(
                            android.media.MediaPlayer
                                    .VIDEO_SCALING_MODE_SCALE_TO_FIT
                    );

                    hint.setVisibility(View.GONE);

                    mp.start();

                } catch (Exception e) {

                    hint.setVisibility(View.VISIBLE);

                    hint.setText(
                            "वीडियो शुरू नहीं हो पाया"
                    );
                }
            });

            mediaPlayer.setOnCompletionListener(mp -> {

                try {
                    mp.start();
                } catch (Exception ignored) {
                }
            });

            mediaPlayer.setOnErrorListener(
                    (mp, what, extra) -> {

                        hint.setVisibility(View.VISIBLE);

                        hint.setText(
                                "इस वीडियो का format फोन में play नहीं हो रहा।"
                        );

                        return true;
                    }
            );

            mediaPlayer.prepareAsync();

        } catch (Exception e) {

            hint.setVisibility(View.VISIBLE);

            hint.setText(
                    "वीडियो खोलने में समस्या हुई।"
            );
        }
    }

    private void stopVideo() {

        if (mediaPlayer != null) {

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

    // =========================================================
    // FIRESTORE VIDEO
    // =========================================================

    private void saveVideoToFirestore() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null ||
                selectedVideoUri == null) {

            return;
        }

        Map<String, Object> video =
                new HashMap<>();

        video.put(
                "uid",
                user.getUid()
        );

        video.put(
                "email",
                user.getEmail()
        );

        video.put(
                "localUri",
                selectedVideoUri.toString()
        );

        video.put(
                "likes",
                0
        );

        video.put(
                "views",
                0
        );

        video.put(
                "createdAt",
                System.currentTimeMillis()
        );

        db.collection("videos")
                .add(video)
                .addOnSuccessListener(
                        documentReference ->
                                currentVideoId =
                                        documentReference.getId()
                );
    }

    // =========================================================
    // LIKE
    // =========================================================

    private void likeVideo() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        if (currentVideoId.isEmpty()) {

            Toast.makeText(
                    this,
                    "पहले वीडियो select करें।",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        db.collection("videos")
                .document(currentVideoId)
                .update(
                        "likes",
                        com.google.firebase.firestore.FieldValue
                                .increment(1)
                )
                .addOnSuccessListener(v -> {

                    Toast.makeText(
                            this,
                            "Liked ❤️",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =========================================================
    // COMMENT
    // =========================================================

    private void addComment() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        final EditText input =
                new EditText(this);

        input.setHint("अपना comment लिखें");

        input.setTextColor(WHITE);

        input.setHintTextColor(GRAY);

        new android.app.AlertDialog.Builder(this)
                .setTitle("COMMENT")
                .setView(input)
                .setPositiveButton(
                        "POST",
                        (dialog, which) -> {

                            String comment =
                                    input.getText()
                                            .toString()
                                            .trim();

                            if (comment.isEmpty()) {
                                return;
                            }

                            Map<String, Object> data =
                                    new HashMap<>();

                            data.put(
                                    "uid",
                                    user.getUid()
                            );

                            data.put(
                                    "email",
                                    user.getEmail()
                            );

                            data.put(
                                    "comment",
                                    comment
                            );

                            data.put(
                                    "videoId",
                                    currentVideoId
                            );

                            data.put(
                                    "createdAt",
                                    System.currentTimeMillis()
                            );

                            db.collection("comments")
                                    .add(data);

                            Toast.makeText(
                                    this,
                                    "Comment posted",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .show();
    }

    // =========================================================
    // SHARE
    // =========================================================

    private void shareVideo() {

        if (selectedVideoUri == null) {

            Toast.makeText(
                    this,
                    "पहले वीडियो चुनें।",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent share =
                new Intent(Intent.ACTION_SEND);

        share.setType("video/*");

        share.putExtra(
                Intent.EXTRA_STREAM,
                selectedVideoUri
        );

        share.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        try {

            startActivity(
                    Intent.createChooser(
                            share,
                            "Share Viyzo Video"
                    )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Share नहीं हो पाया।",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // MESSENGER
    // =========================================================

    private void showMessenger() {

        stopVideo();

        baseRoot();

        title("MESSENGER");

        message(
                "यहाँ Viyzo users को message भेज सकते हैं।"
        );

        EditText receiver =
                new EditText(this);

        receiver.setHint("Receiver UID / Email");

        receiver.setTextColor(WHITE);

        receiver.setHintTextColor(GRAY);

        root.addView(receiver);

        EditText msg =
                new EditText(this);

        msg.setHint("Message");

        msg.setTextColor(WHITE);

        msg.setHintTextColor(GRAY);

        root.addView(msg);

        Button send =
                button("SEND MESSAGE");

        root.addView(send);

        send.setOnClickListener(v -> {

            FirebaseUser user =
                    auth.getCurrentUser();

            if (user == null) {
                return;
            }

            String to =
                    receiver.getText()
                            .toString()
                            .trim();

            String text =
                    msg.getText()
                            .toString()
                            .trim();

            if (to.isEmpty() ||
                    text.isEmpty()) {

                Toast.makeText(
                        this,
                        "Receiver और message भरें।",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "fromUid",
                    user.getUid()
            );

            data.put(
                    "fromEmail",
                    user.getEmail()
            );

            data.put(
                    "to",
                    to
            );

            data.put(
                    "message",
                    text
            );

            data.put(
                    "createdAt",
                    System.currentTimeMillis()
            );

            db.collection("messages")
                    .add(data)
                    .addOnSuccessListener(x -> {

                        msg.setText("");

                        Toast.makeText(
                                this,
                                "Message sent",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
        });

        Button back =
                button("← BACK");

        root.addView(back);

        back.setOnClickListener(
                v -> showHome()
        );
    }

    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    private void showNotifications() {

        stopVideo();

        baseRoot();

        title("NOTIFICATIONS");

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        TextView list =
                text("Loading...", 15);

        root.addView(list);

        db.collection("notifications")
                .whereEqualTo(
                        "uid",
                        user.getUid()
                )
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .limit(50)
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            StringBuilder s =
                                    new StringBuilder();

                            for (com.google.firebase.firestore
                                    .DocumentSnapshot d :
                                    snapshots) {

                                String msg =
                                        d.getString(
                                                "message"
                                        );

                                if (msg != null) {

                                    s.append("• ")
                                            .append(msg)
                                            .append("\n\n");
                                }
                            }

                            if (s.length() == 0) {

                                list.setText(
                                        "कोई notification नहीं है।"
                                );

                            } else {

                                list.setText(
                                        s.toString()
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        e ->
                                list.setText(
                                        "Notifications load नहीं हो सकीं।"
                                )
                );

        Button back =
                button("← BACK");

        root.addView(back);

        back.setOnClickListener(
                v -> showHome()
        );
    }

    // =========================================================
    // SETTINGS
    // =========================================================

    private void showSettings() {

        stopVideo();

        baseRoot();

        title("SETTINGS");

        TextView account =
                text("ACCOUNT SETTINGS", 20);

        root.addView(account);

        Button logout =
                button("LOGOUT");

        root.addView(logout);

        logout.setOnClickListener(v -> {

            auth.signOut();

            showLogin();
        });

        Button back =
                button("← BACK TO HOME");

        root.addView(back);

        back.setOnClickListener(
                v -> showHome()
        );
    }

    // =========================================================
    // USER DASHBOARD
    // =========================================================

    private void showUserDashboard() {

        stopVideo();

        baseRoot();

        title("USER DASHBOARD");

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        TextView info =
                text(
                        "Email: " +
                        user.getEmail(),
                        17
                );

        root.addView(info);

        TextView stats =
                text(
                        "Loading profile...",
                        16
                );

        root.addView(stats);

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(
                        doc -> {

                            if (doc.exists()) {

                                String name =
                                        doc.getString(
                                                "name"
                                        );

                                Long followers =
                                        doc.getLong(
                                                "followers"
                                        );

                                Long following =
                                        doc.getLong(
                                                "following"
                                        );

                                if (followers == null) {
                                    followers = 0L;
                                }

                                if (following == null) {
                                    following = 0L;
                                }

                                stats.setText(
                                        "Name: " +
                                        name +
                                        "\n\nFollowers: " +
                                        followers +
                                        "\nFollowing: " +
                                        following
                                );

                            } else {

                                stats.setText(
                                        "Profile नहीं मिला।"
                                );
                            }
                        }
                );

        Button back =
                button("← BACK TO HOME");

        root.addView(back);

        back.setOnClickListener(
                v -> showHome()
        );
    }

    // =========================================================
    // MY DASHBOARD
    // =========================================================

    private void showMyDashboard() {

        stopVideo();

        baseRoot();

        title("MY DASHBOARD");

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        TextView stats =
                text(
                        "Loading dashboard...",
                        16
                );

        root.addView(stats);

        db.collection("videos")
                .whereEqualTo(
                        "uid",
                        user.getUid()
                )
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            int videos =
                                    snapshots.size();

                            long likes = 0;

                            long views = 0;

                            for (
                                    com.google.firebase.firestore
                                            .DocumentSnapshot d :
                                    snapshots
                            ) {

                                Long l =
                                        d.getLong("likes");

                                Long v =
                                        d.getLong("views");

                                if (l != null) {
                                    likes += l;
                                }

                                if (v != null) {
                                    views += v;
                                }
                            }

                            stats.setText(
                                    "My Videos: " +
                                    videos +
                                    "\n\nLikes: " +
                                    likes +
                                    "\nViews: " +
                                    views
                            );
                        }
                )
                .addOnFailureListener(
                        e ->
                                stats.setText(
                                        "Dashboard load नहीं हुआ।"
                                )
                );

        Button back =
                button("← BACK TO HOME");

        root.addView(back);

        back.setOnClickListener(
                v -> showHome()
        );
    }

    // =========================================================
    // BACK BUTTON
    // =========================================================

    @Override
    public void onBackPressed() {

        if (page == PAGE_VIDEO) {

            showHome();

            return;
        }

        if (auth.getCurrentUser() != null) {

            showHome();

            return;
        }

        super.onBackPressed();
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================

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
    protected void onResume() {

        super.onResume();

        if (mediaPlayer != null) {

            try {

                mediaPlayer.start();

            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onDestroy() {

        stopVideo();

        super.onDestroy();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private int dp(int value) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return (int) (value * density + 0.5f);
    }
}
