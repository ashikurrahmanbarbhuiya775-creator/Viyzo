package com.viyzo.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout mainLayout;
    private Uri selectedVideo = null;
    private VideoView videoPlayer;

    private int likeCount = 0;

    private final int VIDEO_PICKER = 1001;

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

    private TextView title(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(25);
        t.setGravity(Gravity.CENTER);
        t.setPadding(10, 25, 10, 25);
        return t;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setAllCaps(false);
        return b;
    }

    private void prepareLayout() {
        ScrollView scroll = new ScrollView(this);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(25, 20, 25, 30);

        scroll.addView(mainLayout);
        setContentView(scroll);
    }

    private void showLogin() {
        prepareLayout();

        mainLayout.addView(title("VIYZO"));

        TextView info = new TextView(this);
        info.setText("Login to your account");
        info.setTextSize(18);
        info.setGravity(Gravity.CENTER);
        mainLayout.addView(info);

        EditText email = new EditText(this);
        email.setHint("Email");
        email.setInputType(33);
        mainLayout.addView(email);

        EditText password = new EditText(this);
        password.setHint("Password");
        password.setInputType(129);
        mainLayout.addView(password);

        Button login = button("LOGIN");
        mainLayout.addView(login);

        Button signup = button("CREATE NEW ACCOUNT");
        mainLayout.addView(signup);

        login.setOnClickListener(v -> {

            String e = email.getText().toString().trim();
            String p = password.getText().toString().trim();

            if (e.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Email और Password भरें", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> {
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                        showHome();
                    })
                    .addOnFailureListener(error ->
                            Toast.makeText(
                                    this,
                                    "Login failed: " + error.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show()
                    );
        });

        signup.setOnClickListener(v -> showSignup());
    }

    private void showSignup() {
        prepareLayout();

        mainLayout.addView(title("VIYZO"));

        TextView info = new TextView(this);
        info.setText("Create your account");
        info.setTextSize(18);
        info.setGravity(Gravity.CENTER);
        mainLayout.addView(info);

        EditText email = new EditText(this);
        email.setHint("Email");
        email.setInputType(33);
        mainLayout.addView(email);

        EditText password = new EditText(this);
        password.setHint("Password (6+ characters)");
        password.setInputType(129);
        mainLayout.addView(password);

        Button create = button("SIGN UP");
        mainLayout.addView(create);

        Button back = button("BACK TO LOGIN");
        mainLayout.addView(back);

        create.setOnClickListener(v -> {

            String e = email.getText().toString().trim();
            String p = password.getText().toString().trim();

            if (e.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Email और Password भरें", Toast.LENGTH_SHORT).show();
                return;
            }

            if (p.length() < 6) {
                Toast.makeText(this, "Password कम से कम 6 अक्षर का होना चाहिए", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> {

                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {

                            Map<String, Object> profile = new HashMap<>();
                            profile.put("email", e);
                            profile.put("followers", 0);
                            profile.put("following", 0);
                            profile.put("videos", 0);
                            profile.put("views", 0);
                            profile.put("likes", 0);
                            profile.put("earnings", 0);

                            db.collection("users")
                                    .document(user.getUid())
                                    .set(profile)
                                    .addOnCompleteListener(task -> {
                                        Toast.makeText(
                                                this,
                                                "Account Created",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        showHome();
                                    });
                        }
                    })
                    .addOnFailureListener(error ->
                            Toast.makeText(
                                    this,
                                    "Signup failed: " + error.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show()
                    );
        });

        back.setOnClickListener(v -> showLogin());
    }

    private void showHome() {
        prepareLayout();

        mainLayout.addView(title("Welcome to VIYZO"));

        TextView success = new TextView(this);
        success.setText("You are successfully logged in.");
        success.setTextSize(17);
        success.setGravity(Gravity.CENTER);
        success.setPadding(5, 5, 5, 20);
        mainLayout.addView(success);

        Button upload = button("🎥 UPLOAD VIDEO");
        mainLayout.addView(upload);

        TextView videoStatus = new TextView(this);
        videoStatus.setText("No video selected.");
        videoStatus.setTextSize(16);
        videoStatus.setGravity(Gravity.CENTER);
        videoStatus.setPadding(5, 15, 5, 15);
        mainLayout.addView(videoStatus);

        videoPlayer = new VideoView(this);
        videoPlayer.setVisibility(View.GONE);

        LinearLayout.LayoutParams videoParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        600
                );

        videoParams.setMargins(0, 10, 0, 15);
        mainLayout.addView(videoPlayer, videoParams);

        if (selectedVideo != null) {
            videoStatus.setText("Video selected: " +
                    selectedVideo.getLastPathSegment());

            videoPlayer.setVisibility(View.VISIBLE);
            videoPlayer.setVideoURI(selectedVideo);

            MediaController controller = new MediaController(this);
            controller.setAnchorView(videoPlayer);
            videoPlayer.setMediaController(controller);

            videoPlayer.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                videoPlayer.start();
            });
        }

        upload.setOnClickListener(v -> {

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

            startActivityForResult(intent, VIDEO_PICKER);
        });

        Button like = button("❤️ LIKE " + likeCount);
        mainLayout.addView(like);

        like.setOnClickListener(v -> {
            likeCount++;
            like.setText("❤️ LIKE " + likeCount);
        });

        Button comment = button("💬 COMMENT");
        mainLayout.addView(comment);

        comment.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Comment system जल्द Firebase से जुड़ेगा",
                        Toast.LENGTH_SHORT
                ).show()
        );

        Button share = button("🔄 SHARE");
        mainLayout.addView(share);

        share.setOnClickListener(v -> {

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(
                    Intent.EXTRA_TEXT,
                    "Watch this video on VIYZO!"
            );

            startActivity(
                    Intent.createChooser(send, "Share VIYZO video")
            );
        });

        Button delete = button("🗑️ DELETE VIDEO");
        mainLayout.addView(delete);

        delete.setOnClickListener(v -> {

            selectedVideo = null;

            if (videoPlayer != null) {
                videoPlayer.stopPlayback();
                videoPlayer.setVisibility(View.GONE);
            }

            Toast.makeText(
                    this,
                    "Video removed",
                    Toast.LENGTH_SHORT
            ).show();

            showHome();
        });

        Button profile = button("👤 PROFILE");
        mainLayout.addView(profile);

        profile.setOnClickListener(v -> showProfile());

        Button search = button("🔍 SEARCH");
        mainLayout.addView(search);

        search.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Search system जल्द Firebase से जुड़ेगा",
                        Toast.LENGTH_SHORT
                ).show()
        );

        Button followers = button("👥 FOLLOWERS / FOLLOWING");
        mainLayout.addView(followers);

        followers.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Followers system जल्द Firebase से जुड़ेगा",
                        Toast.LENGTH_SHORT
                ).show()
        );

        Button message = button("✉️ MESSAGE");
        mainLayout.addView(message);

        message.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Chat system जल्द बनाया जाएगा",
                        Toast.LENGTH_SHORT
                ).show()
        );

        Button report = button("🚫 REPORT / DISPUTE");
        mainLayout.addView(report);

        report.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Report system जल्द Firebase से जुड़ेगा",
                        Toast.LENGTH_SHORT
                ).show()
        );

        Button logout = button("LOGOUT");
        mainLayout.addView(logout);

        logout.setOnClickListener(v -> {
            auth.signOut();
            selectedVideo = null;
            likeCount = 0;
            showLogin();
        });
    }

    private void showProfile() {
        prepareLayout();

        mainLayout.addView(title("PROFILE"));

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {

            TextView email = new TextView(this);
            email.setText("Email: " + user.getEmail());
            email.setTextSize(18);
            email.setPadding(5, 15, 5, 20);
            mainLayout.addView(email);

            TextView loading = new TextView(this);
            loading.setText("Loading profile...");
            loading.setTextSize(16);
            mainLayout.addView(loading);

            db.collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {

                        if (document.exists()) {

                            String data =
                                    "Followers: " +
                                    document.getLong("followers") +
                                    "\n\nFollowing: " +
                                    document.getLong("following") +
                                    "\n\nVideos: " +
                                    document.getLong("videos") +
                                    "\n\nViews: " +
                                    document.getLong("views") +
                                    "\n\nLikes: " +
                                    document.getLong("likes") +
                                    "\n\nEarnings: ₹" +
                                    document.getLong("earnings");

                            loading.setText(data);

                        } else {
                            loading.setText("Profile data not found.");
                        }
                    })
                    .addOnFailureListener(error ->
                            loading.setText(
                                    "Profile error: " + error.getMessage()
                            )
                    );
        }

        Button home = button("HOME");
        mainLayout.addView(home);

        home.setOnClickListener(v -> showHome());

        Button logout = button("LOGOUT");
        mainLayout.addView(logout);

        logout.setOnClickListener(v -> {
            auth.signOut();
            selectedVideo = null;
            likeCount = 0;
            showLogin();
        });
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VIDEO_PICKER &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getData() != null) {

            selectedVideo = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(
                        selectedVideo,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (SecurityException ignored) {
            }

            Toast.makeText(
                    this,
                    "Video selected successfully",
                    Toast.LENGTH_SHORT
            ).show();

            showHome();
        }
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
