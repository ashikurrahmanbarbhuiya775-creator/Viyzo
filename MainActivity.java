package com.viyzo.app;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout mainLayout;
    private VideoView videoPlayer;

    private Uri selectedVideo = null;
    private long likeCount = 0;

    private static final int VIDEO_PICKER = 1001;

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

    private TextView title(String value) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(25);
        t.setGravity(Gravity.CENTER);
        t.setPadding(10, 25, 10, 25);
        return t;
    }

    private TextView text(String value) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(17);
        t.setPadding(10, 12, 10, 12);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(16);
        b.setAllCaps(false);
        return b;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setPadding(12, 12, 12, 12);
        return e;
    }

    private void prepareLayout() {
        ScrollView scroll = new ScrollView(this);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(20, 15, 20, 30);

        scroll.addView(mainLayout);
        setContentView(scroll);
    }

    private void showLogin() {

        prepareLayout();

        mainLayout.addView(title("VIYZO"));
        mainLayout.addView(text("Login to your account"));

        EditText email = input("Email");
        email.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );
        mainLayout.addView(email);

        EditText password = input("Password");
        password.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        mainLayout.addView(password);

        Button login = button("LOGIN");
        mainLayout.addView(login);

        Button signup = button("CREATE NEW ACCOUNT");
        mainLayout.addView(signup);

        login.setOnClickListener(v -> {

            String e = email.getText().toString().trim();
            String p = password.getText().toString();

            if (e.isEmpty() || p.isEmpty()) {
                toast("Email और Password भरें");
                return;
            }

            login.setEnabled(false);

            auth.signInWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> {

                        login.setEnabled(true);

                        toast("Login Successful");

                        showHome();
                    })
                    .addOnFailureListener(error -> {

                        login.setEnabled(true);

                        toast(
                                "Login failed: " +
                                        error.getMessage()
                        );
                    });
        });

        signup.setOnClickListener(v -> showSignup());
    }

    private void showSignup() {

        prepareLayout();

        mainLayout.addView(title("VIYZO"));
        mainLayout.addView(text("Create your account"));

        EditText email = input("Email");

        email.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        mainLayout.addView(email);

        EditText password =
                input("Password (6+ characters)");

        password.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        mainLayout.addView(password);

        Button create = button("SIGN UP");
        mainLayout.addView(create);

        Button back = button("BACK TO LOGIN");
        mainLayout.addView(back);

        create.setOnClickListener(v -> {

            String e =
                    email.getText()
                            .toString()
                            .trim();

            String p =
                    password.getText()
                            .toString();

            if (e.isEmpty() || p.isEmpty()) {
                toast("Email और Password भरें");
                return;
            }

            if (p.length() < 6) {
                toast(
                        "Password कम से कम 6 characters का होना चाहिए"
                );
                return;
            }

            create.setEnabled(false);

            auth.createUserWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> {

                        FirebaseUser user =
                                auth.getCurrentUser();

                        if (user == null) {
                            create.setEnabled(true);
                            return;
                        }

                        Map<String, Object> profile =
                                new HashMap<>();

                        profile.put("email", e);
                        profile.put("followers", 0L);
                        profile.put("following", 0L);
                        profile.put("videos", 0L);
                        profile.put("views", 0L);
                        profile.put("likes", 0L);
                        profile.put("earnings", 0L);
                        profile.put(
                                "createdAt",
                                FieldValue.serverTimestamp()
                        );

                        db.collection("users")
                                .document(user.getUid())
                                .set(profile)
                                .addOnSuccessListener(x -> {

                                    create.setEnabled(true);

                                    toast("Account Created");

                                    showHome();
                                })
                                .addOnFailureListener(error -> {

                                    create.setEnabled(true);

                                    toast(
                                            "Profile save failed: " +
                                                    error.getMessage()
                                    );
                                });
                    })
                    .addOnFailureListener(error -> {

                        create.setEnabled(true);

                        toast(
                                "Signup failed: " +
                                        error.getMessage()
                        );
                    });
        });

        back.setOnClickListener(v -> showLogin());
    }

    private void showHome() {

        prepareLayout();

        mainLayout.addView(
                title("Welcome to VIYZO")
        );

        mainLayout.addView(
                text("You are successfully logged in.")
        );

        Button upload =
                button("🎥 SELECT VIDEO");

        mainLayout.addView(upload);

        TextView videoStatus =
                text(
                        selectedVideo == null
                                ? "No video selected."
                                : "Video selected: " +
                                selectedVideo.getLastPathSegment()
                );

        mainLayout.addView(videoStatus);

        videoPlayer = new VideoView(this);

        videoPlayer.setVisibility(View.GONE);

        LinearLayout.LayoutParams vp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        600
                );

        vp.setMargins(0, 10, 0, 15);

        mainLayout.addView(
                videoPlayer,
                vp
        );

        if (selectedVideo != null) {
            playSelectedVideo();
        }

        upload.setOnClickListener(
                v -> pickVideo()
        );

        Button like =
                button("❤️ LIKE");

        mainLayout.addView(like);

        loadLikeCount(like);

        like.setOnClickListener(
                v -> saveLike(like)
        );

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
                button("🔄 SHARE VIDEO");

        mainLayout.addView(share);

        share.setOnClickListener(
                v -> shareVideo()
        );

        Button delete =
                button("🗑️ DELETE SELECTED VIDEO");

        mainLayout.addView(delete);

        delete.setOnClickListener(v -> {

            selectedVideo = null;
            likeCount = 0;

            if (videoPlayer != null) {
                videoPlayer.stopPlayback();
            }

            showHome();
        });

        Button profile =
                button("👤 PROFILE");

        mainLayout.addView(profile);

        profile.setOnClickListener(
                v -> showProfile()
        );

        Button search =
                button("🔍 SEARCH USERS");

        mainLayout.addView(search);

        search.setOnClickListener(
                v -> showSearch()
        );

        Button follow =
                button("👥 FOLLOWERS / FOLLOWING");

        mainLayout.addView(follow);

        follow.setOnClickListener(
                v -> showFollowCenter()
        );

        Button message =
                button("✉️ MESSAGES");

        mainLayout.addView(message);

        message.setOnClickListener(
                v -> showMessageCenter()
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

        logout.setOnClickListener(
                v -> logout()
        );
    }

    private void pickVideo() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

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
    }

    private void playSelectedVideo() {

        if (selectedVideo == null ||
                videoPlayer == null) {
            return;
        }

        videoPlayer.setVisibility(
                View.VISIBLE
        );

        videoPlayer.setVideoURI(
                selectedVideo
        );

        MediaController controller =
                new MediaController(this);

        controller.setAnchorView(
                videoPlayer
        );

        videoPlayer.setMediaController(
                controller
        );

        videoPlayer.setOnPreparedListener(
                mp -> {

                    mp.setLooping(true);

                    videoPlayer.start();
                }
        );
    }

    private String currentUid() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return null;
        }

        return user.getUid();
    }

    private String currentVideoId() {

        String uid = currentUid();

        if (uid == null) {
            return "unknown";
        }

        return uid;
    }

    private void ensureVideoDocument() {

        String uid = currentUid();

        if (uid == null ||
                selectedVideo == null) {
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "ownerId",
                uid
        );

        data.put(
                "videoName",
                selectedVideo.getLastPathSegment()
        );

        data.put(
                "likes",
                likeCount
        );

        data.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        data.put(
                "localOnly",
                true
        );

        db.collection("videos")
                .document(uid)
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    private void loadLikeCount(
            Button likeButton
    ) {

        String uid = currentUid();

        if (uid == null) {
            return;
        }

        db.collection("videos")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    Long likes =
                            doc.getLong("likes");

                    likeCount =
                            likes == null
                                    ? 0
                                    : likes;

                    likeButton.setText(
                            "❤️ LIKE " +
                                    likeCount
                    );
                });
    }

    private void saveLike(
            Button likeButton
    ) {

        String uid = currentUid();

        if (uid == null) {
            toast("पहले Login करें");
            return;
        }

        if (selectedVideo == null) {
            toast("पहले Video select करें");
            return;
        }

        likeButton.setEnabled(false);

        ensureVideoDocument();

        db.collection("videos")
                .document(uid)
                .update(
                        "likes",
                        FieldValue.increment(1)
                )
                .addOnSuccessListener(x -> {

                    likeCount++;

                    likeButton.setText(
                            "❤️ LIKE " +
                                    likeCount
                    );

                    updateMyLikes(
                            likeCount
                    );

                    likeButton.setEnabled(true);
                })
                .addOnFailureListener(error -> {

                    Map<String, Object> data =
                            new HashMap<>();

                    data.put(
                            "ownerId",
                            uid
                    );

                    data.put(
                            "videoName",
                            selectedVideo.getLastPathSegment()
                    );

                    data.put(
                            "likes",
                            1L
                    );

                    data.put(
                            "createdAt",
                            FieldValue.serverTimestamp()
                    );

                    data.put(
                            "localOnly",
                            true
                    );

                    db.collection("videos")
                            .document(uid)
                            .set(
                                    data,
                                    SetOptions.merge()
                            )
                            .addOnSuccessListener(y -> {

                                likeCount = 1;

                                likeButton.setText(
                                        "❤️ LIKE 1"
                                );

                                updateMyLikes(1);

                                likeButton.setEnabled(
                                        true
                                );
                            })
                            .addOnFailureListener(e -> {

                                likeButton.setEnabled(
                                        true
                                );

                                toast(
                                        "Like save failed: " +
                                                e.getMessage()
                                );
                            });
                });
    }

    private void updateMyLikes(
            long value
    ) {

        String uid = currentUid();

        if (uid == null) {
            return;
        }

        db.collection("users")
                .document(uid)
                .set(
                        mapOf(
                                "likes",
                                value
                        ),
                        SetOptions.merge()
                );
    }

    private Map<String, Object> mapOf(
            String key,
            Object value
    ) {

        Map<String, Object> map =
                new HashMap<>();

        map.put(
                key,
                value
        );

        return map;
    }

    private void showCommentBox() {

        if (currentUid() == null) {
            showLogin();
            return;
        }

        if (selectedVideo == null) {
            toast("पहले Video select करें");
            return;
        }

        prepareLayout();

        mainLayout.addView(
                title("COMMENTS")
        );

        EditText commentText =
                input("अपना Comment लिखें...");

        commentText.setMinLines(3);

        commentText.setGravity(
                Gravity.TOP
        );

        mainLayout.addView(
                commentText
        );

        Button send =
                button("SEND COMMENT");

        mainLayout.addView(send);

        Button back =
                button("BACK TO HOME");

        mainLayout.addView(back);

        send.setOnClickListener(v -> {

            String value =
                    commentText
                            .getText()
                            .toString()
                            .trim();

            if (value.isEmpty()) {
                toast("Comment लिखें");
                return;
            }

            FirebaseUser user =
                    auth.getCurrentUser();

            if (user == null) {
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
                    value
            );

            data.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            db.collection("videos")
                    .document(currentVideoId())
                    .collection("comments")
                    .add(data)
                    .addOnSuccessListener(x -> {

                        send.setEnabled(true);

                        commentText.setText("");

                        toast(
                                "Comment saved"
                        );

                        showComments();
                    })
                    .addOnFailureListener(error -> {

                        send.setEnabled(true);

                        toast(
                                "Comment failed: " +
                                        error.getMessage()
                        );
                    });
        });

        back.setOnClickListener(
                v -> showHome()
        );
    }

    private void showComments() {

        if (currentUid() == null) {
            showLogin();
            return;
        }

        if (selectedVideo == null) {
            toast("पहले Video select करें");
            return;
        }

        prepareLayout();

        mainLayout.addView(
                title("COMMENTS")
        );

        TextView loading =
                text("Comments loading...");

        mainLayout.addView(
                loading
        );

        db.collection("videos")
                .document(currentVideoId())
                .collection("comments")
                .orderBy(
                        "createdAt",
                        Query.Direction.ASCENDING
                )
                .get()
                .addOnSuccessListener(snapshot -> {

                    mainLayout.removeView(
                            loading
                    );

                    if (snapshot.isEmpty()) {

                        mainLayout.addView(
                                text(
                                        "अभी कोई Comment नहीं है।"
                                )
                        );

                    } else {

                        for (
                                DocumentSnapshot doc :
                                snapshot.getDocuments()
                        ) {

                            String email =
                                    doc.getString(
                                            "email"
                                    );

                            String value =
                                    doc.getString(
                                            "text"
                                    );

                            mainLayout.addView(
                                    text(
                                            "👤 " +
                                                    (
                                                            email == null
                                                                    ? "User"
                                                                    : email
                                                    )
                                                    +
                                                    "\n" +
                                                    (
                                                            value == null
                                                                    ? ""
                                                                    : value
                                                    )
                                    )
                            );
                        }
                    }
                })
                .addOnFailureListener(error ->
                        loading.setText(
                                "Comments load failed: " +
                                        error.getMessage()
                        )
                );

        Button add =
                button("💬 ADD COMMENT");

        mainLayout.addView(add);

        add.setOnClickListener(
                v -> showCommentBox()
        );

        Button home =
                button("HOME");

        mainLayout.addView(home);

        home.setOnClickListener(
                v -> showHome()
        );
    }

    private void shareVideo() {

        if (selectedVideo == null) {
            toast("पहले Video select करें");
            return;
        }

        Intent send =
                new Intent(
                        Intent.ACTION_SEND
                );

        send.setType("video/*");

        send.putExtra(
                Intent.EXTRA_STREAM,
                selectedVideo
        );

        send.putExtra(
                Intent.EXTRA_TEXT,
                "Watch this video on VIYZO!"
        );

        send.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        send.setClipData(
                ClipData.newRawUri(
                        "VIYZO Video",
                        selectedVideo
                )
        );

        try {

            startActivity(
                    Intent.createChooser(
                            send,
                            "Share VIYZO video"
                    )
            );

        } catch (Exception e) {

            toast(
                    "Share नहीं हो पाया"
            );
        }
    }

    private void showSearch() {

        if (currentUid() == null) {
            showLogin();
            return;
        }

        prepareLayout();

        mainLayout.addView(
                title("SEARCH USERS")
        );

        EditText search =
                input(
                        "Email का कुछ हिस्सा लिखें"
                );

        search.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        mainLayout.addView(search);

        Button find =
                button("SEARCH");

        mainLayout.addView(find);

        LinearLayout results =
                new LinearLayout(this);

        results.setOrientation(
                LinearLayout.VERTICAL
        );

        mainLayout.addView(
                results
        );

        Button home =
                button("HOME");

        mainLayout.addView(home);

        home.setOnClickListener(
                v -> showHome()
        );

        find.setOnClickListener(v -> {

            String q =
                    search.getText()
                            .toString()
                            .trim()
                            .toLowerCase();

            if (q.isEmpty()) {
                toast("Search text लिखें");
                return;
            }

            results.removeAllViews();

            TextView loading =
                    text("Searching...");

            results.addView(
                    loading
            );

            db.collection("users")
                    .orderBy("email")
                    .startAt(q)
                    .endAt(q + "\uf8ff")
                    .limit(20)
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        results.removeAllViews();

                        if (snapshot.isEmpty()) {

                            results.addView(
                                    text(
                                            "User नहीं मिला।"
                                    )
                            );

                            return;
                        }

                        for (
                                DocumentSnapshot doc :
                                snapshot.getDocuments()
                        ) {

                            String uid =
                                    doc.getId();

                            String email =
                                    doc.getString(
                                            "email"
                                    );

                            if (uid.equals(
                                    currentUid()
                            )) {
                                continue;
                            }

                            Button userButton =
                                    button(
                                            "👤 " +
                                                    (
                                                            email == null
                                                                    ? "User"
                                                                    : email
                                                    )
                                    );

                            results.addView(
                                    userButton
                            );

                            userButton.setOnClickListener(
                                    x ->
                                            showUserProfile(
                                                    uid,
                                                    email
                                            )
                            );
                        }
                    })
                    .addOnFailureListener(
                            error ->
                                    loading.setText(
                                            "Search failed: " +
                                                    error.getMessage()
                                    )
                    );
        });
    }

    private void showUserProfile(
            String uid,
            String email
    ) {

        prepareLayout();

        mainLayout.addView(
                title("USER PROFILE")
        );

        mainLayout.addView(
                text(
                        "Email: " +
                                (
                                        email == null
                                                ? "User"
                                                : email
                                )
                )
        );

        TextView stats =
                text("Loading...");

        mainLayout.addView(
                stats
        );

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    Long followers =
                            doc.getLong(
                                    "followers"
                            );

                    Long following =
                            doc.getLong(
                                    "following"
                            );

                    Long videos =
                            doc.getLong(
                                    "videos"
                            );

                    Long likes =
                            doc.getLong(
                                    "likes"
                            );

                    stats.setText(
                            "Followers: " +
                                    number(followers)
                                    +
                                    "\nFollowing: " +
                                    number(following)
                                    +
                                    "\nVideos: " +
                                    number(videos)
                                    +
                                    "\nLikes: " +
                                    number(likes)
                    );
                });

        Button follow =
                button(
                        "FOLLOW / UNFOLLOW"
                );

        mainLayout.addView(follow);

        checkFollowing(
                uid,
                follow
        );

        follow.setOnClickListener(
                v ->
                        toggleFollow(
                                uid,
                                follow
                        )
        );

        Button message =
                button("✉️ MESSAGE");

        mainLayout.addView(
                message
        );

        message.setOnClickListener(
                v ->
                        showChat(
                                uid,
                                email
                        )
        );

        Button back =
                button("BACK TO SEARCH");

        mainLayout.addView(
                back
        );

        back.setOnClickListener(
                v -> showSearch()
        );
    }

    private String number(
            Long value
    ) {

        return value == null
                ? "0"
                : String.valueOf(value);
    }

    private void checkFollowing(
            String targetUid,
            Button button
    ) {

        String me = currentUid();

        if (me == null) {
            return;
        }

        db.collection("users")
                .document(me)
                .collection("following")
                .document(targetUid)
                .get()
                .addOnSuccessListener(doc ->
                        button.setText(
                                doc.exists()
                                        ? "UNFOLLOW"
                                        : "FOLLOW"
                        )
                );
    }

    private void toggleFollow(
            String targetUid,
            Button button
    ) {

        String me = currentUid();

        if (me == null ||
                me.equals(targetUid)) {
            return;
        }

        button.setEnabled(false);

        db.collection("users")
                .document(me)
                .collection("following")
                .document(targetUid)
                .get()
                .addOnSuccessListener(existing -> {

                    if (existing.exists()) {

                        db.collection("users")
                                .document(me)
                                .collection("following")
                                .document(targetUid)
                                .delete()
                                .addOnSuccessListener(x -> {

                                    updateCounter(
                                            me,
                                            "following",
                                            -1
                                    );

                                    updateCounter(
                                            targetUid,
                                            "followers",
                                            -1
                                    );

                                    button.setText(
                                            "FOLLOW"
                                    );

                                    button.setEnabled(
                                            true
                                    );
                                })
                                .addOnFailureListener(error -> {

                                    button.setEnabled(
                                            true
                                    );

                                    toast(
                                            "Unfollow failed: " +
                                                    error.getMessage()
                                    );
                                });

                    } else {

                        Map<String, Object> data =
                                new HashMap<>();

                        data.put(
                                "userId",
                                targetUid
                        );

                        data.put(
                                "createdAt",
                                FieldValue.serverTimestamp()
                        );

                        db.collection("users")
                                .document(me)
                                .collection("following")
                                .document(targetUid)
                                .set(data)
                                .addOnSuccessListener(x -> {

                                    db.collection("users")
                                            .document(targetUid)
                                            .collection("followers")
                                            .document(me)
                                            .set(
                                                    mapOf(
                                                            "userId",
                                                            me
                                                    )
                                            );

                                    updateCounter(
                                            me,
                                            "following",
                                            1
                                    );

                                    updateCounter(
                                            targetUid,
                                            "followers",
                                            1
                                    );

                                    button.setText(
                                            "UNFOLLOW"
                                    );

                                    button.setEnabled(
                                            true
                                    );
                                })
                                .addOnFailureListener(error -> {

                                    button.setEnabled(
                                            true
                                    );

                                    toast(
                                            "Follow failed: " +
                                                    error.getMessage()
                                    );
                                });
                    }
                });
    }

    private void updateCounter(
            String uid,
            String field,
            long amount
    ) {

        db.collection("users")
                .document(uid)
                .set(
                        mapOf(
                                field,
                                FieldValue.increment(
                                        amount
                                )
                        ),
                        SetOptions.merge()
                );
    }

    private void showFollowCenter() {

        if (currentUid() == null) {
            showLogin();
            return;
        }

        prepareLayout();

        mainLayout.addView(
                title(
                        "FOLLOWERS / FOLLOWING"
                )
        );

        Button followers =
                button(
                        "👥 MY FOLLOWERS"
                );

        Button following =
                button(
                        "👤 MY FOLLOWING"
                );

        mainLayout.addView(
                followers
        );

        mainLayout.addView(
                following
        );

        LinearLayout list =
                new LinearLayout(this);

        list.setOrientation(
                LinearLayout.VERTICAL
        );

        mainLayout.addView(
                list
        );

        followers.setOnClickListener(
                v ->
                        loadFollowList(
                                "followers",
                                list
                        )
        );

        following.setOnClickListener(
                v ->
                        loadFollowList(
                                "following",
                                list
                        )
        );

        Button home =
                button("HOME");

        mainLayout.addView(home);

        home.setOnClickListener(
                v -> showHome()
        );
    }

    private void loadFollowList(
            String type,
            LinearLayout list
    ) {

        list.removeAllViews();

        list.addView(
                text("Loading...")
        );

        String me = currentUid();

        db.collection("users")
                .document(me)
                .collection(type)
                .get()
                .addOnSuccessListener(snapshot -> {

                    list.removeAllViews();

                    if (snapshot.isEmpty()) {

                        list.addView(
                                text(
                                        "अभी कोई user नहीं है।"
                                )
                        );

                        return;
                    }

                    for (
                            DocumentSnapshot doc :
                            snapshot.getDocuments()
                    ) {

                        String uid =
                                doc.getId();

                        db.collection("users")
                                .document(uid)
                                .get()
                                .addOnSuccessListener(
                                        userDoc -> {

                                            String email =
                                                    userDoc.getString(
                                                            "email"
                                                    );

                                            Button b =
                                                    button(
                                                            "👤 " +
                                                                    (
                                                                            email == null
                                                                                    ? uid
                                                                                    : email
                                                                    )
                                                    );

                                            list.addView(
                                                    b
                                            );

                                            b.setOnClickListener(
                                                    v ->
                                                            showUserProfile(
                                                                    uid,
                                                                    email
                                                            )
                                            );
                                        }
                                );
                    }
                })
                .addOnFailureListener(
                        error ->
                                list.addView(
                                        text(
                                                "Load failed: " +
                                                        error.getMessage()
                                        )
                                )
                );
    }

    private void showMessageCenter() {

        if (currentUid() == null) {
            showLogin();
            return;
        }

        prepareLayout();

        mainLayout.addView(
                title("MESSAGES")
        );

        EditText search =
                input(
                        "जिस user को message करना है उसका email"
                );

        search.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        mainLayout.addView(
                search
        );

        Button find =
                button("FIND USER");

        mainLayout.addView(
                find
        );

        LinearLayout results =
                new LinearLayout(this);

        results.setOrientation(
                LinearLayout.VERTICAL
        );

        mainLayout.addView(
                results
        );

        Button home =
                button("HOME");

        mainLayout.addView(
                home
        );

        home.setOnClickListener(
                v -> showHome()
        );

        find.setOnClickListener(v -> {

            String email =
                    search.getText()
                            .toString()
                            .trim()
                            .toLowerCase();

            if (email.isEmpty()) {
                toast("Email लिखें");
                return;
            }

            results.removeAllViews();

            db.collection("users")
                    .whereEqualTo(
                            "email",
                            email
                    )
                    .limit(1)
                    .get()
                    .addOnSuccessListener(
                            snapshot -> {

                                results.removeAllViews();

                                if (snapshot.isEmpty()) {

                                    results.addView(
                                            text(
                                                    "User नहीं मिला।"
                                            )
                                    );

                                    return;
                                }

                                DocumentSnapshot doc =
                                        snapshot
                                                .getDocuments()
                                                .get(0);

                                String uid =
                                        doc.getId();

                                Button b =
                                        button(
                                                "✉️ " +
                                                        email
                                        );

                                results.addView(
                                        b
                                );

                                b.setOnClickListener(
                                        x ->
                                                showChat(
                                                        uid,
                                                        email
                                                )
                                );
                            }
                    )
                    .addOnFailureListener(
                            error ->
                                    results.addView(
                                            text(
                                                    "Search failed: " +
                                                            error.getMessage()
                                            )
                                    )
                    );
        });
    }

    private String chatId(
            String a,
            String b
    ) {

        if (a.compareTo(b) < 0) {
            return a + "_" + b;
        }

        return b + "_" + a;
    }

    private void showChat(
            String otherUid,
            String otherEmail
    ) {

        String me = currentUid();

        if (me == null) {
            showLogin();
            return;
        }

        String id =
                chatId(
                        me,
                        otherUid
                );

        prepareLayout();

        mainLayout.addView(
                title("CHAT")
        );

        mainLayout.addView(
                text(
                        "With: " +
                                (
                                        otherEmail == null
                                                ? otherUid
                                                : otherEmail
                                )
                )
        );

        LinearLayout messages =
                new LinearLayout(this);

        messages.setOrientation(
                LinearLayout.VERTICAL
        );

        mainLayout.addView(
                messages
        );

        EditText messageText =
                input("Message लिखें...");

        mainLayout.addView(
                messageText
        );

        Button send =
                button("SEND");

        mainLayout.addView(
                send
        );

        Button back =
                button("BACK");

        mainLayout.addView(
                back
        );

        db.collection("chats")
                .document(id)
                .collection("messages")
                .orderBy(
                        "createdAt",
                        Query.Direction.ASCENDING
                )
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            if (snapshot.isEmpty()) {

                                messages.addView(
                                        text(
                                                "अभी कोई message नहीं है।"
                                        )
                                );

                            } else {

                                for (
                                        DocumentSnapshot doc :
                                        snapshot.getDocuments()
                                ) {

                                    String from =
                                            doc.getString(
                                                    "fromEmail"
                                            );

                                    String value =
                                            doc.getString(
                                                    "text"
                                            );

                                    messages.addView(
                                            text(
                                                    (
                                                            from == null
                                                                    ? "User"
                                                                    : from
                                                    )
                                                            +
                                                            ":\n" +
                                                            (
                                                                    value == null
                                                                            ? ""
                                                                            : value
                                                            )
                                            )
                                    );
                                }
                            }
                        }
                );

        send.setOnClickListener(v -> {

            String value =
                    messageText
                            .getText()
                            .toString()
                            .trim();

            if (value.isEmpty()) {
                toast("Message लिखें");
                return;
            }

            FirebaseUser user =
                    auth.getCurrentUser();

            if (user == null) {
                return;
            }

            send.setEnabled(false);

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "fromUid",
                    me
            );

            data.put(
                    "fromEmail",
                    user.getEmail()
            );

            data.put(
                    "toUid",
                    otherUid
            );

            data.put(
                    "text",
                    value
            );

            data.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            db.collection("chats")
                    .document(id)
                    .collection("messages")
                    .add(data)
                    .addOnSuccessListener(x -> {

                        send.setEnabled(true);

                        messageText.setText("");

                        messages.addView(
                                text(
                                        "You:\n" +
                                                value
                                )
                        );
                    })
                    .addOnFailureListener(error -> {

                        send.setEnabled(true);

                        toast(
                                "Message failed: " +
                                        error.getMessage()
                        );
                    });
        });

        back.setOnClickListener(
                v -> showMessageCenter()
        );
    }

    private void showReportBox() {

        if (currentUid() == null) {
            showLogin();
            return;
        }

        prepareLayout();

        mainLayout.addView(
                title("REPORT / DISPUTE")
        );

        EditText reason =
                input(
                        "Report का कारण लिखें..."
                );

        reason.setMinLines(4);

        reason.setGravity(
                Gravity.TOP
        );

        mainLayout.addView(
                reason
        );

        Button submit =
                button("SUBMIT REPORT");

        mainLayout.addView(
                submit
        );

        Button back =
                button("BACK TO HOME");

        mainLayout.addView(
                back
        );

        submit.setOnClickListener(v -> {

            String value =
                    reason.getText()
                            .toString()
                            .trim();

            if (value.isEmpty()) {
                toast(
                        "Report का कारण लिखें"
                );
                return;
            }

            FirebaseUser user =
                    auth.getCurrentUser();

            if (user == null) {
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
                    value
            );

            report.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            db.collection("reports")
                    .add(report)
                    .addOnSuccessListener(x -> {

                        submit.setEnabled(true);

                        toast(
                                "Report submitted successfully"
                        );

                        showHome();
                    })
                    .addOnFailureListener(error -> {

                        submit.setEnabled(true);

                        toast(
                                "Report failed: " +
                                        error.getMessage()
                        );
                    });
        });

        back.setOnClickListener(
                v -> showHome()
        );
    }

    private void showProfile() {

        if (currentUid() == null) {
            showLogin();
            return;
        }

        prepareLayout();

        mainLayout.addView(
                title("PROFILE")
        );

        FirebaseUser user =
                auth.getCurrentUser();

        mainLayout.addView(
                text(
                        "Email: " +
                                user.getEmail()
                )
        );

        TextView stats =
                text("Loading profile...");

        mainLayout.addView(
                stats
        );

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    stats.setText(
                            "Followers: " +
                                    number(
                                            doc.getLong(
                                                    "followers"
                                            )
                                    )
                                    +
                                    "\nFollowing: " +
                                    number(
                                            doc.getLong(
                                                    "following"
                                            )
                                    )
                                    +
                                    "\nVideos: " +
                                    number(
                                            doc.getLong(
                                                    "videos"
                                            )
                                    )
                                    +
                                    "\nViews: " +
                                    number(
                                            doc.getLong(
                                                    "views"
                                            )
                                    )
                                    +
                                    "\nLikes: " +
                                    number(
                                            doc.getLong(
                                                    "likes"
                                            )
                                    )
                                    +
                                    "\nEarnings: ₹" +
                                    number(
                                            doc.getLong(
                                                    "earnings"
                                            )
                                    )
                    );
                })
                .addOnFailureListener(
                        error ->
                                stats.setText(
                                        "Profile error: " +
                                                error.getMessage()
                                )
                );

        Button home =
                button("HOME");

        mainLayout.addView(home);

        home.setOnClickListener(
                v -> showHome()
        );

        Button logout =
                button("LOGOUT");

        mainLayout.addView(
                logout
        );

        logout.setOnClickListener(
                v -> logout()
        );
    }

    private void logout() {

        auth.signOut();

        selectedVideo = null;

        likeCount = 0;

        showLogin();
    }

    private void toast(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
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
                requestCode == VIDEO_PICKER
                        &&
                        resultCode == RESULT_OK
                        &&
                        data != null
                        &&
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

            ensureVideoDocument();

            toast(
                    "Video selected successfully"
            );

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
