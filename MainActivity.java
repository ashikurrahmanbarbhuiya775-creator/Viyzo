package com.viyzo.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.SurfaceTexture;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.media.MediaPlayer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout root;

    private Uri selectedVideoUri;

    private MediaPlayer mediaPlayer;
    private TextureView currentTexture;
    private TextView videoMessage;

    private static final int PICK_VIDEO = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() != null) {
            showHome();
        } else {
            showLogin();
        }
    }

    // =========================
    // BASIC UI
    // =========================

    private LinearLayout baseRoot() {

        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        layout.setPadding(
                dp(16),
                dp(25),
                dp(16),
                dp(30)
        );

        layout.setBackgroundColor(
                Color.rgb(18, 18, 22)
        );

        return layout;
    }

    private int dp(int value) {
        return (int) (
                value *
                getResources()
                        .getDisplayMetrics()
                        .density
                + 0.5f
        );
    }

    private void setPage(LinearLayout page) {

        ScrollView scroll = new ScrollView(this);

        scroll.setFillViewport(true);

        scroll.addView(page);

        setContentView(scroll);
    }

    private void addTitle(String text) {

        TextView title = new TextView(this);

        title.setText(text);
        title.setTextColor(Color.WHITE);
        title.setTextSize(29);
        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );
        title.setGravity(Gravity.CENTER);

        title.setPadding(
                0,
                dp(10),
                0,
                dp(20)
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );
    }

    private EditText input(String hint) {

        EditText edit = new EditText(this);

        edit.setHint(hint);
        edit.setHintTextColor(Color.LTGRAY);
        edit.setTextColor(Color.WHITE);
        edit.setTextSize(16);
        edit.setSingleLine(true);

        edit.setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8)
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.setMargins(
                0,
                dp(6),
                0,
                dp(6)
        );

        root.addView(edit, params);

        return edit;
    }

    private Button button(String text) {

        Button b = new Button(this);

        b.setText(text);
        b.setTextSize(16);
        b.setAllCaps(false);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.setMargins(
                0,
                dp(6),
                0,
                dp(4)
        );

        root.addView(b, params);

        return b;
    }

    private TextView text(String value, float size) {

        TextView t = new TextView(this);

        t.setText(value);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);

        t.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        root.addView(
                t,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        return t;
    }

    private void toast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // =========================
    // LOGIN
    // =========================

    private void showLogin() {

        stopVideo();

        root = baseRoot();

        addTitle("VIYZO");

        TextView heading =
                text("LOGIN", 22);

        heading.setGravity(Gravity.CENTER);

        EditText email =
                input("Email");

        EditText password =
                input("Password");

        password.setInputType(0x81);

        Button login =
                button("LOGIN");

        login.setOnClickListener(v -> {

            String e =
                    email.getText()
                            .toString()
                            .trim();

            String p =
                    password.getText()
                            .toString();

            if (e.isEmpty() || p.isEmpty()) {

                toast(
                        "Email और password डालें"
                );

                return;
            }

            login.setEnabled(false);

            auth.signInWithEmailAndPassword(
                    e,
                    p
            )
            .addOnSuccessListener(result -> {

                login.setEnabled(true);

                ensureUserProfile();

                showHome();

            })
            .addOnFailureListener(error -> {

                login.setEnabled(true);

                toast(
                        "Login failed: "
                                + error.getMessage()
                );
            });
        });

        Button signup =
                button("CREATE NEW ACCOUNT");

        signup.setOnClickListener(
                v -> showSignup()
        );

        setPage(root);
    }

    // =========================
    // SIGNUP
    // =========================

    private void showSignup() {

        stopVideo();

        root = baseRoot();

        addTitle("CREATE VIYZO ACCOUNT");

        EditText name =
                input("Your name");

        EditText email =
                input("Email");

        EditText password =
                input("Password");

        password.setInputType(0x81);

        Button create =
                button("SIGN UP");

        create.setOnClickListener(v -> {

            String n =
                    name.getText()
                            .toString()
                            .trim();

            String e =
                    email.getText()
                            .toString()
                            .trim();

            String p =
                    password.getText()
                            .toString();

            if (n.isEmpty()) {

                toast("Name डालें");

                return;
            }

            if (e.isEmpty()) {

                toast("Email डालें");

                return;
            }

            if (p.length() < 6) {

                toast(
                        "Password कम से कम 6 characters का रखें"
                );

                return;
            }

            create.setEnabled(false);

            auth.createUserWithEmailAndPassword(
                    e,
                    p
            )
            .addOnSuccessListener(result -> {

                String uid =
                        auth.getCurrentUser()
                                .getUid();

                Map<String, Object> user =
                        new HashMap<>();

                user.put(
                        "uid",
                        uid
                );

                user.put(
                        "name",
                        n
                );

                user.put(
                        "email",
                        e
                );

                user.put(
                        "followers",
                        0L
                );

                user.put(
                        "following",
                        0L
                );

                user.put(
                        "createdAt",
                        FieldValue.serverTimestamp()
                );

                db.collection("users")
                        .document(uid)
                        .set(user);

                toast(
                        "Account created successfully"
                );

                showHome();

            })
            .addOnFailureListener(error -> {

                create.setEnabled(true);

                toast(
                        "Signup failed: "
                                + error.getMessage()
                );
            });
        });

        Button back =
                button("BACK TO LOGIN");

        back.setOnClickListener(
                v -> showLogin()
        );

        setPage(root);
    }

    // =========================
    // HOME
    // =========================

    private void showHome() {

        stopVideo();

        root = baseRoot();

        addTitle("VIYZO");

        TextView homeTitle =
                text(
                        "HOME • VIDEO FEED",
                        21
                );

        homeTitle.setGravity(
                Gravity.CENTER
        );

        // VIDEO AREA

        FrameLayout videoBox =
                new FrameLayout(this);

        videoBox.setBackgroundColor(
                Color.BLACK
        );

        LinearLayout.LayoutParams boxParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(320)
                );

        boxParams.setMargins(
                0,
                dp(8),
                0,
                dp(10)
        );

        root.addView(
                videoBox,
                boxParams
        );

        currentTexture =
                new TextureView(this);

        FrameLayout.LayoutParams textureParams =
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                );

        textureParams.gravity =
                Gravity.CENTER;

        videoBox.addView(
                currentTexture,
                textureParams
        );

        videoMessage =
                new TextView(this);

        videoMessage.setText(
                selectedVideoUri == null
                        ? "HOME VIDEO\n\nUPLOAD A VIDEO"
                        : "Loading video..."
        );

        videoMessage.setTextColor(
                Color.WHITE
        );

        videoMessage.setTextSize(17);

        videoMessage.setGravity(
                Gravity.CENTER
        );

        FrameLayout.LayoutParams messageParams =
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                );

        messageParams.gravity =
                Gravity.CENTER;

        videoBox.addView(
                videoMessage,
                messageParams
        );

        if (selectedVideoUri != null) {

            currentTexture.setSurfaceTextureListener(
                    new HomeTextureListener()
            );
        }

        // UPLOAD

        Button upload =
                button("UPLOAD VIDEO");

        upload.setOnClickListener(
                v -> selectVideo()
        );

        // VIDEO VIEW

        Button view =
                button("VIDEO VIEW");

        view.setOnClickListener(v -> {

            if (selectedVideoUri == null) {

                toast(
                        "पहले video upload/select करें"
                );

                return;
            }

            showVideoView();
        });

        // LIKE

        Button like =
                button("♥ LIKE");

        like.setOnClickListener(
                v -> likeVideo()
        );

        // COMMENT

        Button comment =
                button("💬 COMMENT");

        comment.setOnClickListener(
                v -> showComments()
        );

        // SHARE

        Button share =
                button("↗ SHARE");

        share.setOnClickListener(
                v -> shareVideo()
        );

        // MESSENGER

        Button messenger =
                button("MESSENGER");

        messenger.setOnClickListener(
                v -> showMessenger()
        );

        // NOTIFICATION

        Button notifications =
                button("NOTIFICATIONS");

        notifications.setOnClickListener(
                v -> showNotifications()
        );

        // SETTINGS

        Button settings =
                button("SETTINGS");

        settings.setOnClickListener(
                v -> showSettings()
        );

        // USER DASHBOARD

        Button userDashboard =
                button("USER DASHBOARD");

        userDashboard.setOnClickListener(
                v -> showUserDashboard()
        );

        // MY DASHBOARD

        Button myDashboard =
                button("MY DASHBOARD");

        myDashboard.setOnClickListener(
                v -> showMyDashboard()
        );

        // LOGOUT

        Button logout =
                button("LOGOUT");

        logout.setOnClickListener(v -> {

            stopVideo();

            auth.signOut();

            selectedVideoUri = null;

            showLogin();
        });

        setPage(root);
    }

    // =========================
    // VIDEO PICKER
    // =========================

    private void selectVideo() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType(
                "video/*"
        );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        intent.addFlags(
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                PICK_VIDEO
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (
                requestCode == PICK_VIDEO
                        &&
                resultCode == RESULT_OK
                        &&
                data != null
                        &&
                data.getData() != null
        ) {

            selectedVideoUri =
                    data.getData();

            try {

                int takeFlags =
                        data.getFlags()
                                &
                                Intent.FLAG_GRANT_READ_URI_PERMISSION;

                getContentResolver()
                        .takePersistableUriPermission(
                                selectedVideoUri,
                                takeFlags
                        );

            } catch (Exception ignored) {
            }

            toast(
                    "Video selected successfully"
            );

            saveVideoToFirestore();

            showHome();
        }
    }

    // =========================
    // HOME VIDEO PLAYER
    // =========================

    private class HomeTextureListener
            implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(
                SurfaceTexture surface,
                int width,
                int height
        ) {

            Surface s =
                    new Surface(surface);

            playVideo(
                    selectedVideoUri,
                    s,
                    videoMessage
            );
        }

        @Override
        public void onSurfaceTextureSizeChanged(
                SurfaceTexture surface,
                int width,
                int height
        ) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(
                SurfaceTexture surface
        ) {

            stopVideo();

            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(
                SurfaceTexture surface
        ) {
        }
    }

    // =========================
    // ACTUAL VIDEO PLAYER
    // =========================

    private void playVideo(
            Uri uri,
            Surface surface,
            TextView message
    ) {

        if (uri == null) {

            message.setText(
                    "No video selected"
            );

            return;
        }

        stopVideo();

        try {

            mediaPlayer =
                    new MediaPlayer();

            mediaPlayer.setDataSource(
                    this,
                    uri
            );

            /*
             * IMPORTANT:
             * Video output is sent directly
             * to TextureView Surface.
             */
            mediaPlayer.setSurface(
                    surface
            );

            mediaPlayer.setLooping(
                    true
            );

            mediaPlayer.setOnPreparedListener(
                    mp -> {

                        message.setText("");

                        mp.start();

                        saveView();
                    }
            );

            mediaPlayer.setOnErrorListener(
                    (mp, what, extra) -> {

                        message.setText(
                                "VIDEO PLAYBACK ERROR"
                        );

                        toast(
                                "Video play नहीं हो पाया"
                        );

                        return true;
                    }
            );

            mediaPlayer.prepareAsync();

        } catch (Exception e) {

            message.setText(
                    "VIDEO ERROR"
            );

            toast(
                    "Video error: "
                            + e.getMessage()
            );
        }
    }

    // =========================
    // VIDEO VIEW SCREEN
    // =========================

    private void showVideoView() {

        stopVideo();

        root = baseRoot();

        addTitle("VIDEO VIEW");

        FrameLayout videoBox =
                new FrameLayout(this);

        videoBox.setBackgroundColor(
                Color.BLACK
        );

        LinearLayout.LayoutParams boxParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(500)
                );

        root.addView(
                videoBox,
                boxParams
        );

        TextureView texture =
                new TextureView(this);

        FrameLayout.LayoutParams textureParams =
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                );

        textureParams.gravity =
                Gravity.CENTER;

        videoBox.addView(
                texture,
                textureParams
        );

        TextView message =
                new TextView(this);

        message.setText(
                "Loading video..."
        );

        message.setTextColor(
                Color.WHITE
        );

        message.setTextSize(17);

        message.setGravity(
                Gravity.CENTER
        );

        FrameLayout.LayoutParams msgParams =
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                );

        videoBox.addView(
                message,
                msgParams
        );

        texture.setSurfaceTextureListener(
                new TextureView.SurfaceTextureListener() {

                    @Override
                    public void onSurfaceTextureAvailable(
                            SurfaceTexture surface,
                            int width,
                            int height
                    ) {

                        Surface s =
                                new Surface(
                                        surface
                                );

                        playVideo(
                                selectedVideoUri,
                                s,
                                message
                        );
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(
                            SurfaceTexture surface,
                            int width,
                            int height
                    ) {
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(
                            SurfaceTexture surface
                    ) {

                        stopVideo();

                        return true;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(
                            SurfaceTexture surface
                    ) {
                    }
                }
        );

        Button like =
                button("♥ LIKE");

        like.setOnClickListener(
                v -> likeVideo()
        );

        Button comment =
                button("💬 COMMENT");

        comment.setOnClickListener(
                v -> showComments()
        );

        Button share =
                button("↗ SHARE");

        share.setOnClickListener(
                v -> shareVideo()
        );

        Button back =
                button("← BACK HOME");

        back.setOnClickListener(
                v -> showHome()
        );

        setPage(root);
    }

    // =========================
    // STOP VIDEO
    // =========================

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

    // =========================
    // SAVE VIDEO
    // =========================

    private void saveVideoToFirestore() {

        if (
                auth.getCurrentUser() == null
                        ||
                selectedVideoUri == null
        ) {
            return;
        }

        Map<String, Object> video =
                new HashMap<>();

        video.put(
                "ownerUid",
                auth.getCurrentUser()
                        .getUid()
        );

        video.put(
                "videoUri",
                selectedVideoUri.toString()
        );

        video.put(
                "likes",
                0L
        );

        video.put(
                "views",
                0L
        );

        video.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        db.collection("videos")
                .add(video);
    }

    // =========================
    // VIEW
    // =========================

    private void saveView() {

        if (
                auth.getCurrentUser() == null
                        ||
                selectedVideoUri == null
        ) {
            return;
        }

        Map<String, Object> view =
                new HashMap<>();

        view.put(
                "uid",
                auth.getCurrentUser()
                        .getUid()
        );

        view.put(
                "videoUri",
                selectedVideoUri.toString()
        );

        view.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        db.collection("views")
                .add(view);
    }

    // =========================
    // LIKE
    // =========================

    private void likeVideo() {

        if (
                auth.getCurrentUser() == null
        ) {
            return;
        }

        Map<String, Object> like =
                new HashMap<>();

        like.put(
                "uid",
                auth.getCurrentUser()
                        .getUid()
        );

        like.put(
                "videoUri",
                selectedVideoUri == null
                        ? ""
                        : selectedVideoUri.toString()
        );

        like.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        db.collection("likes")
                .add(like);

        addNotification(
                "Video liked"
        );

        toast(
                "♥ Video liked"
        );
    }

    // =========================
    // COMMENTS
    // =========================

    private void showComments() {

        root = baseRoot();

        addTitle("COMMENTS");

        EditText comment =
                input("Write a comment");

        Button post =
                button("POST COMMENT");

        post.setOnClickListener(v -> {

            String value =
                    comment.getText()
                            .toString()
                            .trim();

            if (value.isEmpty()) {

                toast(
                        "Comment लिखें"
                );

                return;
            }

            if (
                    auth.getCurrentUser()
                            == null
            ) {
                return;
            }

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "uid",
                    auth.getCurrentUser()
                            .getUid()
            );

            data.put(
                    "text",
                    value
            );

            data.put(
                    "videoUri",
                    selectedVideoUri == null
                            ? ""
                            : selectedVideoUri.toString()
            );

            data.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            db.collection("comments")
                    .add(data);

            addNotification(
                    "New comment posted"
            );

            comment.setText("");

            toast(
                    "Comment posted"
            );
        });

        TextView comments =
                text(
                        "Loading comments...",
                        16
                );

        db.collection("comments")
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .limit(50)
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            StringBuilder result =
                                    new StringBuilder();

                            for (
                                    DocumentSnapshot d
                                    : snapshot.getDocuments()
                            ) {

                                String c =
                                        d.getString(
                                                "text"
                                        );

                                if (c != null) {

                                    result.append(
                                            "• "
                                    )
                                    .append(c)
                                    .append(
                                            "\n\n"
                                    );
                                }
                            }

                            if (
                                    result.length()
                                            == 0
                            ) {

                                comments.setText(
                                        "No comments yet."
                                );

                            } else {

                                comments.setText(
                                        result.toString()
                                );
                            }
                        }
                );

        Button back =
                button("← BACK HOME");

        back.setOnClickListener(
                v -> showHome()
        );

        setPage(root);
    }

    // =========================
    // SHARE
    // =========================

    private void shareVideo() {

        Intent share =
                new Intent(
                        Intent.ACTION_SEND
                );

        share.setType(
                "text/plain"
        );

        String video =
                selectedVideoUri == null
                        ? ""
                        : selectedVideoUri.toString();

        share.putExtra(
                Intent.EXTRA_TEXT,
                "Watch this video on VIYZO\n"
                        + video
        );

        startActivity(
                Intent.createChooser(
                        share,
                        "Share Viyzo video"
                )
        );
    }

    // =========================
    // MESSENGER
    // =========================

    private void showMessenger() {

        root = baseRoot();

        addTitle("MESSENGER");

        EditText receiver =
                input("Recipient email");

        EditText message =
                input("Write message");

        Button send =
                button("SEND MESSAGE");

        send.setOnClickListener(v -> {

            String to =
                    receiver.getText()
                            .toString()
                            .trim();

            String msg =
                    message.getText()
                            .toString()
                            .trim();

            if (
                    to.isEmpty()
                            ||
                    msg.isEmpty()
            ) {

                toast(
                        "Recipient और message डालें"
                );

                return;
            }

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "fromUid",
                    auth.getCurrentUser()
                            .getUid()
            );

            data.put(
                    "toEmail",
                    to
            );

            data.put(
                    "message",
                    msg
            );

            data.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            db.collection("messages")
                    .add(data);

            addNotification(
                    "Message sent"
            );

            message.setText("");

            toast(
                    "Message sent"
            );
        });

        Button back =
                button("← BACK HOME");

        back.setOnClickListener(
                v -> showHome()
        );

        setPage(root);
    }

    // =========================
    // NOTIFICATIONS
    // =========================

    private void showNotifications() {

        root = baseRoot();

        addTitle("NOTIFICATIONS");

        TextView list =
                text(
                        "Loading...",
                        16
                );

        db.collection("notifications")
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .limit(50)
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            StringBuilder result =
                                    new StringBuilder();

                            for (
                                    DocumentSnapshot d
                                    : snapshot.getDocuments()
                            ) {

                                String n =
                                        d.getString(
                                                "message"
                                        );

                                if (n != null) {

                                    result.append(
                                            "• "
                                    )
                                    .append(n)
                                    .append(
                                            "\n\n"
                                    );
                                }
                            }

                            if (
                                    result.length()
                                            == 0
                            ) {

                                list.setText(
                                        "No notifications."
                                );

                            } else {

                                list.setText(
                                        result.toString()
                                );
                            }
                        }
                );

        Button back =
                button("← BACK HOME");

        back.setOnClickListener(
                v -> showHome()
        );

        setPage(root);
    }

    private void addNotification(
            String message
    ) {

        if (
                auth.getCurrentUser() == null
        ) {
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "uid",
                auth.getCurrentUser()
                        .getUid()
        );

        data.put(
                "message",
                message
        );

        data.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        db.collection("notifications")
                .add(data);
    }

    // =========================
    // SETTINGS
    // =========================

    private void showSettings() {

        root = baseRoot();

        addTitle("SETTINGS");

        String email =
                auth.getCurrentUser() == null
                        ? ""
                        : auth.getCurrentUser()
                                .getEmail();

        text(
                "Account Email:\n"
                        + email,
                17
        );

        Button logout =
                button("LOGOUT");

        logout.setOnClickListener(v -> {

            stopVideo();

            auth.signOut();

            selectedVideoUri = null;

            showLogin();
        });

        Button back =
                button("← BACK HOME");

        back.setOnClickListener(
                v -> showHome()
        );

        setPage(root);
    }

    // =========================
    // USER DASHBOARD
    // =========================

    private void showUserDashboard() {

        root = baseRoot();

        addTitle("USER DASHBOARD");

        TextView stats =
                text(
                        "Loading your statistics...",
                        17
                );

        String uid =
                auth.getCurrentUser()
                        .getUid();

        db.collection("views")
                .whereEqualTo(
                        "uid",
                        uid
                )
                .get()
                .addOnSuccessListener(
                        views -> {

                            db.collection("likes")
                                    .whereEqualTo(
                                            "uid",
                                            uid
                                    )
                                    .get()
                                    .addOnSuccessListener(
                                            likes -> {

                                                db.collection(
                                                        "comments"
                                                )
                                                .whereEqualTo(
                                                        "uid",
                                                        uid
                                                )
                                                .get()
                                                .addOnSuccessListener(
                                                        comments -> {

                                                            stats.setText(
                                                                    "USER DASHBOARD\n\n"
                                                                    +
                                                                    "Views: "
                                                                    +
                                                                    views.size()
                                                                    +
                                                                    "\n"
                                                                    +
                                                                    "Likes: "
                                                                    +
                                                                    likes.size()
                                                                    +
                                                                    "\n"
                                                                    +
                                                                    "Comments: "
                                                                    +
                                                                    comments.size()
                                                            );
                                                        }
                                                );
                                            }
                                    );
                        }
                );

        Button back =
                button("← BACK HOME");

        back.setOnClickListener(
                v -> showHome()
        );

        setPage(root);
    }

    // =========================
    // MY / OWNER DASHBOARD
    // =========================

    private void showMyDashboard() {

        root = baseRoot();

        addTitle("MY DASHBOARD");

        TextView stats =
                text(
                        "Loading Viyzo statistics...",
                        17
                );

        db.collection("users")
                .get()
                .addOnSuccessListener(
                        users -> {

                            db.collection("videos")
                                    .get()
                                    .addOnSuccessListener(
                                            videos -> {

                                                db.collection(
                                                        "views"
                                                )
                                                .get()
                                                .addOnSuccessListener(
                                                        views -> {

                                                            db.collection(
                                                                    "likes"
                                                            )
                                                            .get()
                                                            .addOnSuccessListener(
                                                                    likes -> {

                                                                        db.collection(
                                                                                "comments"
                                                                        )
                                                                        .get()
                                                                        .addOnSuccessListener(
                                                                                comments -> {

                                                                                    stats.setText(
                                                                                            "VIYZO OWNER DASHBOARD\n\n"
                                                                                            +
                                                                                            "Users: "
                                                                                            +
                                                                                            users.size()
                                                                                            +
                                                                                            "\n\n"
                                                                                            +
                                                                                            "Videos: "
                                                                                            +
                                                                                            videos.size()
                                                                                            +
                                                                                            "\n\n"
                                                                                            +
                                                                                            "Views: "
                                                                                            +
                                                                                            views.size()
                                                                                            +
                                                                                            "\n\n"
                                                                                            +
                                                                                            "Likes: "
                                                                                            +
                                                                                            likes.size()
                                                                                            +
                                                                                            "\n\n"
                                                                                            +
                                                                                            "Comments: "
                                                                                            +
                                                                                            comments.size()
                                                                                    );
                                                                                }
                                                                        );
                                                                    }
                                                            );
                                                        }
                                                );
                                            }
                                    );
                        }
                );

        Button refresh =
                button("REFRESH");

        refresh.setOnClickListener(
                v -> showMyDashboard()
        );

        Button back =
                button("← BACK HOME");

        back.setOnClickListener(
                v -> showHome()
        );

        setPage(root);
    }

    // =========================
    // USER PROFILE
    // =========================

    private void ensureUserProfile() {

        if (
                auth.getCurrentUser() == null
        ) {
            return;
        }

        String uid =
                auth.getCurrentUser()
                        .getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(
                        document -> {

                            if (!document.exists()) {

                                Map<String, Object>
                                        user =
                                        new HashMap<>();

                                user.put(
                                        "uid",
                                        uid
                                );

                                user.put(
                                        "name",
                                        auth.getCurrentUser()
                                                .getEmail()
                                );

                                user.put(
                                        "email",
                                        auth.getCurrentUser()
                                                .getEmail()
                                );

                                user.put(
                                        "followers",
                                        0L
                                );

                                user.put(
                                        "following",
                                        0L
                                );

                                user.put(
                                        "createdAt",
                                        FieldValue.serverTimestamp()
                                );

                                db.collection("users")
                                        .document(uid)
                                        .set(user);
                            }
                        }
                );
    }

    // =========================
    // LIFECYCLE
    // =========================

    @Override
    protected void onPause() {

        super.onPause();

        if (
                mediaPlayer != null
                        &&
                mediaPlayer.isPlaying()
        ) {

            try {
                mediaPlayer.pause();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (mediaPlayer != null) {

            try {

                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
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

        if (auth.getCurrentUser() != null) {

            showHome();

        } else {

            super.onBackPressed();
        }
    }
}
