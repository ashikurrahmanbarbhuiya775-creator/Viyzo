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
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout mainLayout;
    private Uri selectedVideo = null;
    private VideoView videoPlayer;

    private long likeCount = 0;

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
                Toast.makeText(
                        this,
                        "Email और Password भरें",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            login.setEnabled(false);

            auth.signInWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> {
                        login.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Login Successful",
                                Toast.LENGTH_SHORT
                        ).show();

                        showHome();
                    })
                    .addOnFailureListener(error -> {
                        login.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Login failed: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });
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
                Toast.makeText(
                        this,
                        "Email और Password भरें",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (p.length() < 6) {
                Toast.makeText(
                        this,
                        "Password कम से कम 6 अक्षर का होना चाहिए",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            create.setEnabled(false);

            auth.createUserWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> {

                        FirebaseUser user = auth.getCurrentUser();

                        if (user == null) {
                            create.setEnabled(true);
                            return;
                        }

                        Map<String, Object> profile = new HashMap<>();

                        profile.put("email", e);
                        profile.put("followers", 0L);
                        profile.put("following", 0L);
                        profile.put("videos", 0L);
                        profile.put("views", 0L);
                        profile.put("likes", 0L);
                        profile.put("earnings", 0L);

                        db.collection("users")
                                .document(user.getUid())
                                .set(profile)
                                .addOnSuccessListener(unused -> {

                                    create.setEnabled(true);

                                    Toast.makeText(
                                            this,
                                            "Account Created",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    showHome();
                                })
                                .addOnFailureListener(error -> {

                                    create.setEnabled(true);

                                    Toast.makeText(
                                            this,
                                            "Profile save failed: " +
                                                    error.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });
                    })
                    .addOnFailureListener(error -> {

                        create.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Signup failed: " +
                                        error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });
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

            videoStatus.setText(
                    "Video selected: " +
                            selectedVideo.getLastPathSegment()
            );

            videoPlayer.setVisibility(View.VISIBLE);
            videoPlayer.setVideoURI(selectedVideo);

            MediaController controller =
                    new MediaController(this);

            controller.setAnchorView(videoPlayer);
            videoPlayer.setMediaController(controller);

            videoPlayer.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                videoPlayer.start();
            });
        }

        upload.setOnClickListener(v -> {

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

            startActivityForResult(
                    intent,
                    VIDEO_PICKER
            );
        });

        Button like =
                button("❤️ LIKE " + likeCount);

        mainLayout.addView(like);

        loadLikeCount(like);

        like.setOnClickListener(v -> {

            FirebaseUser user =
                    auth.getCurrentUser();

            if (user == null) {
                Toast.makeText(
                        this,
                        "पहले Login करें",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String videoId = getCurrentVideoId();

            like.setEnabled(false);

            db.collection("videos")
                    .document(videoId)
                    .update(
                            "likes",
                            FieldValue.increment(1)
                    )
                    .addOnSuccessListener(unused -> {

                        likeCount++;

                        like.setText(
                                "❤️ LIKE " + likeCount
                        );

                        like.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Like saved",
                                Toast.LENGTH_SHORT
                        ).show();
                    })
                    .addOnFailureListener(error -> {

                        Map<String, Object> video =
                                new HashMap<>();

                        video.put(
                                "ownerId",
                                user.getUid()
                        );

                        video.put(
                                "likes",
                                1L
                        );

                        video.put(
                                "createdAt",
                                FieldValue.serverTimestamp()
                        );

                        db.collection("videos")
                                .document(videoId)
                                .set(video)
                                .addOnSuccessListener(unused2 -> {

                                    likeCount = 1;

                                    like.setText(
                                            "❤️ LIKE 1"
                                    );

                                    like.setEnabled(true);
                                })
                                .addOnFailureListener(error2 -> {

                                    like.setEnabled(true);

                                    Toast.makeText(
                                            this,
                                            "Like save failed: " +
                                                    error2.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });
                    });
        });

        Button comment =
                button("💬 COMMENT");

        mainLayout.addView(comment);

        comment.setOnClickListener(
                v -> showCommentBox()
        );

        Button viewComments =
                button("📖 VIEW COMMENTS");

        mainLayout.addView(viewComments);

        viewComments.setOnClickListener(
                v -> showComments()
        );

        Button share =
                button("🔄 SHARE");

        mainLayout.addView(share);

        share.setOnClickListener(v -> {

            Intent send =
                    new Intent(Intent.ACTION_SEND);

            send.setType("text/plain");

            send.putExtra(
                    Intent.EXTRA_TEXT,
                    "Watch this video on VIYZO!"
            );

            startActivity(
                    Intent.createChooser(
                            send,
                            "Share VIYZO video"
                    )
            );
        });

        Button delete =
                button("🗑️ DELETE VIDEO");

        mainLayout.addView(delete);

        delete.setOnClickListener(v -> {

            selectedVideo = null;
            likeCount = 0;

            if (videoPlayer != null) {

                videoPlayer.stopPlayback();
                videoPlayer.setVisibility(
                        View.GONE
                );
            }

            Toast.makeText(
                    this,
                    "Video removed",
                    Toast.LENGTH_SHORT
            ).show();

            showHome();
        });

        Button profile =
                button("👤 PROFILE");

        mainLayout.addView(profile);

        profile.setOnClickListener(
                v -> showProfile()
        );

        Button search =
                button("🔍 SEARCH");

        mainLayout.addView(search);

        search.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Search अगले step में Firebase से जुड़ेगा",
                        Toast.LENGTH_SHORT
                ).show()
        );

        Button followers =
                button("👥 FOLLOWERS / FOLLOWING");

        mainLayout.addView(followers);

        followers.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Followers अगले step में Firebase से जुड़ेगा",
                        Toast.LENGTH_SHORT
                ).show()
        );

        Button message =
                button("✉️ MESSAGE");

        mainLayout.addView(message);

        message.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Message अगले step में बनाया जाएगा",
                        Toast.LENGTH_SHORT
                ).show()
        );

        Button report =
                button("🚫 REPORT / DISPUTE");

        mainLayout.addView(report);

        report.setOnClickListener(
                v -> showReportBox()
        );

        Button logout =
                button("LOGOUT");

        mainLayout.addView(logout);

        logout.setOnClickListener(v -> {

            auth.signOut();

            selectedVideo = null;
            likeCount = 0;

            showLogin();
        });
    }

    private String getCurrentVideoId() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return "unknown";
        }

        return user.getUid();
    }

    private void loadLikeCount(Button likeButton) {

        String videoId =
                getCurrentVideoId();

        db.collection("videos")
                .document(videoId)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        Long likes =
                                document.getLong("likes");

                        if (likes != null) {
                            likeCount = likes;
                        } else {
                            likeCount = 0;
                        }

                        likeButton.setText(
                                "❤️ LIKE " + likeCount
                        );
                    }
                });
    }

    private void showCommentBox() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "पहले Login करें",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        prepareLayout();

        mainLayout.addView(
                title("COMMENTS")
        );

        EditText commentText =
                new EditText(this);

        commentText.setHint(
                "अपना Comment लिखें..."
        );

        commentText.setMinLines(3);

        commentText.setGravity(
                Gravity.TOP
        );

        mainLayout.addView(commentText);

        Button send =
                button("SEND COMMENT");

        mainLayout.addView(send);

        Button back =
                button("BACK TO HOME");

        mainLayout.addView(back);

        send.setOnClickListener(v -> {

            String text =
                    commentText
                            .getText()
                            .toString()
                            .trim();

            if (text.isEmpty()) {

                Toast.makeText(
                        this,
                        "Comment लिखें",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            send.setEnabled(false);

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "userId",
                    user.getUid()
            );

            data.put(
                    "email",
                    user.getEmail()
            );

            data.put(
                    "text",
                    text
            );

            data.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            db.collection("videos")
                    .document(getCurrentVideoId())
                    .collection("comments")
                    .add(data)
                    .addOnSuccessListener(documentReference -> {

                        send.setEnabled(true);

                        commentText.setText("");

                        Toast.makeText(
                                this,
                                "Comment saved successfully",
                                Toast.LENGTH_SHORT
                        ).show();

                        showComments();
                    })
                    .addOnFailureListener(error -> {

                        send.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Comment save failed: " +
                                        error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });
        });

        back.setOnClickListener(
                v -> showHome()
        );
    }

    private void showComments() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            showLogin();
            return;
        }

        prepareLayout();

        mainLayout.addView(
                title("COMMENTS")
        );

        TextView loading =
                new TextView(this);

        loading.setText(
                "Comments loading..."
        );

        loading.setTextSize(17);

        loading.setPadding(
                5, 15, 5, 15
        );

        mainLayout.addView(loading);

        db.collection("videos")
                .document(getCurrentVideoId())
                .collection("comments")
                .get()
                .addOnSuccessListener(snapshot -> {

                    mainLayout.removeView(
                            loading
                    );

                    if (snapshot.isEmpty()) {

                        TextView empty =
                                new TextView(this);

                        empty.setText(
                                "अभी कोई Comment नहीं है।"
                        );

                        empty.setTextSize(17);

                        empty.setPadding(
                                5, 20, 5, 20
                        );

                        mainLayout.addView(
                                empty
                        );

                    } else {

                        for (
                                DocumentSnapshot document :
                                snapshot.getDocuments()
                        ) {

                            String email =
                                    document.getString(
                                            "email"
                                    );

                            String text =
                                    document.getString(
                                            "text"
                                    );

                            TextView item =
                                    new TextView(this);

                            item.setText(
                                    "👤 " +
                                            (email == null
                                                    ? "User"
                                                    : email)
                                            +
                                            "\n"
                                            +
                                            (text == null
                                                    ? ""
                                                    : text)
                            );

                            item.setTextSize(16);

                            item.setPadding(
                                    10, 15, 10, 15
                            );

                            mainLayout.addView(
                                    item
                            );
                        }
                    }
                })
                .addOnFailureListener(error -> {

                    loading.setText(
                            "Comments load failed: " +
                                    error.getMessage()
                    );
                });

        Button addComment =
                button("💬 ADD COMMENT");

        mainLayout.addView(addComment);

        addComment.setOnClickListener(
                v -> showCommentBox()
        );

        Button home =
                button("HOME");

        mainLayout.addView(home);

        home.setOnClickListener(
                v -> showHome()
        );
    }

    private void showReportBox() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            showLogin();
            return;
        }

        prepareLayout();

        mainLayout.addView(
                title("REPORT / DISPUTE")
        );

        EditText reportText =
                new EditText(this);

        reportText.setHint(
                "Report का कारण लिखें..."
        );

        reportText.setMinLines(4);

        reportText.setGravity(
                Gravity.TOP
        );

        mainLayout.addView(reportText);

        Button submit =
                button("SUBMIT REPORT");

        mainLayout.addView(submit);

        Button back =
                button("BACK TO HOME");

        mainLayout.addView(back);

        submit.setOnClickListener(v -> {

            String reason =
                    reportText
                            .getText()
                            .toString()
                            .trim();

            if (reason.isEmpty()) {

                Toast.makeText(
                        this,
                        "Report का कारण लिखें",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            submit.setEnabled(false);

            Map<String, Object> report =
                    new HashMap<>();

            report.put(
                    "userId",
                    user.getUid()
            );

            report.put(
                    "email",
                    user.getEmail()
            );

            report.put(
                    "reason",
                    reason
            );

            report.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            db.collection("reports")
                    .add(report)
                    .addOnSuccessListener(documentReference -> {

                        submit.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Report submitted successfully",
                                Toast.LENGTH_LONG
                        ).show();

                        showHome();
                    })
                    .addOnFailureListener(error -> {

                        submit.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Report failed: " +
                                        error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });
        });

        back.setOnClickListener(
                v -> showHome()
        );
    }

    private void showProfile() {

        prepareLayout();

        mainLayout.addView(
                title("PROFILE")
        );

        FirebaseUser user =
                auth.getCurrentUser();

        if (user != null) {

            TextView email =
                    new TextView(this);

            email.setText(
                    "Email: " +
                            user.getEmail()
            );

            email.setTextSize(18);

            email.setPadding(
                    5, 15, 5, 20
            );

            mainLayout.addView(email);

            TextView loading =
                    new TextView(this);

            loading.setText(
                    "Loading profile..."
            );

            loading.setTextSize(16);

            mainLayout.addView(
                    loading
            );

            db.collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {

                        if (document.exists()) {

                            Long followers =
                                    document.getLong(
                                            "followers"
                                    );

                            Long following =
                                    document.getLong(
                                            "following"
                                    );

                            Long videos =
                                    document.getLong(
                                            "videos"
                                    );

                            Long views =
                                    document.getLong(
                                            "views"
                                    );

                            Long likes =
                                    document.getLong(
                                            "likes"
                                    );

                            Long earnings =
                                    document.getLong(
                                            "earnings"
                                    );

                            String data =
                                    "Followers: " +
                                            safeNumber(followers)
                                            +
                                            "\n\nFollowing: " +
                                            safeNumber(following)
                                            +
                                            "\n\nVideos: " +
                                            safeNumber(videos)
                                            +
                                            "\n\nViews: " +
                                            safeNumber(views)
                                            +
                                            "\n\nLikes: " +
                                            safeNumber(likes)
                                            +
                                            "\n\nEarnings: ₹" +
                                            safeNumber(earnings);

                            loading.setText(data);

                        } else {

                            loading.setText(
                                    "Profile data not found."
                            );
                        }
                    })
                    .addOnFailureListener(error ->
                            loading.setText(
                                    "Profile error: " +
                                            error.getMessage()
                            )
                    );
        }

        Button home =
                button("HOME");

        mainLayout.addView(home);

        home.setOnClickListener(
                v -> showHome()
        );

        Button logout =
                button("LOGOUT");

        mainLayout.addView(logout);

        logout.setOnClickListener(v -> {

            auth.signOut();

            selectedVideo = null;
            likeCount = 0;

            showLogin();
        });
    }

    private String safeNumber(Long value) {

        if (value == null) {
            return "0";
        }

        return String.valueOf(value);
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
                requestCode == VIDEO_PICKER &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getData() != null
        ) {

            selectedVideo =
                    data.getData();

            try {

                getContentResolver()
                        .takePersistableUriPermission(
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
