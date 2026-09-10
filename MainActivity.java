package com.viyzo.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.MediaController;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout root;
    private VideoView videoView;

    private Uri selectedVideo;
    private String currentUid;

    /*
     * CLOUDINARY
     * Cloud name: vnxamljh
     * Upload preset: viyzo_upload
     */
    private static final String CLOUD_NAME = "vnxamljh";
    private static final String UPLOAD_PRESET = "viyzo_upload";

    private final ActivityResultLauncher<String> videoPicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri == null) return;

                        selectedVideo = uri;

                        playLocalVideo(uri);

                        uploadVideoToCloudinary(uri);
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        createNotificationChannel();

        if (auth.getCurrentUser() != null) {
            currentUid = auth.getCurrentUser().getUid();
            showHome();
        } else {
            showLogin();
        }
    }

    /* =========================================================
       LOGIN
       ========================================================= */

    private void showLogin() {

        root = baseRoot();

        addTitle("WELCOME TO VIYZO");
        addLabel("Login");

        EditText email = input("Email");
        EditText password = input("Password");

        password.setInputType(0x00000081);

        root.addView(email);
        root.addView(password);

        Button login = button("LOGIN");

        login.setOnClickListener(v -> {

            String e = email.getText().toString().trim();
            String p = password.getText().toString();

            if (e.isEmpty() || p.isEmpty()) {
                toast("Email and password required");
                return;
            }

            auth.signInWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> {

                        currentUid = result.getUser().getUid();

                        toast("Login successful");

                        showHome();
                    })
                    .addOnFailureListener(
                            e1 -> toast("Login failed: " + e1.getMessage()));
        });

        root.addView(login);

        Button signup = button("CREATE NEW ACCOUNT");

        signup.setOnClickListener(v -> showSignup());

        root.addView(signup);

        setContentView(wrap());
    }

    /* =========================================================
       SIGN UP
       ========================================================= */

    private void showSignup() {

        root = baseRoot();

        addTitle("CREATE VIYZO ACCOUNT");

        EditText name = input("Your name");
        EditText email = input("Email");
        EditText password = input("Password");

        password.setInputType(0x00000081);

        root.addView(name);
        root.addView(email);
        root.addView(password);

        Button create = button("SIGN UP");

        create.setOnClickListener(v -> {

            String n = name.getText().toString().trim();
            String e = email.getText().toString().trim();
            String p = password.getText().toString();

            if (n.isEmpty()) {
                toast("Enter your name");
                return;
            }

            if (e.isEmpty()) {
                toast("Enter email");
                return;
            }

            if (p.length() < 6) {
                toast("Password must be 6 characters or more");
                return;
            }

            auth.createUserWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> {

                        currentUid = result.getUser().getUid();

                        Map<String, Object> user =
                                new HashMap<>();

                        user.put("uid", currentUid);
                        user.put("name", n);
                        user.put("email", e);
                        user.put("followersCount", 0L);
                        user.put("followingCount", 0L);
                        user.put("likesReceived", 0L);
                        user.put("createdAt",
                                FieldValue.serverTimestamp());

                        db.collection("users")
                                .document(currentUid)
                                .set(user, SetOptions.merge())
                                .addOnSuccessListener(x -> {

                                    toast("Account created");

                                    showHome();
                                })
                                .addOnFailureListener(
                                        x -> toast(
                                                "Profile save failed: "
                                                        + x.getMessage()));
                    })
                    .addOnFailureListener(
                            e1 -> toast(
                                    "Sign up failed: "
                                            + e1.getMessage()));
        });

        root.addView(create);

        Button back = button("BACK TO LOGIN");

        back.setOnClickListener(v -> showLogin());

        root.addView(back);

        setContentView(wrap());
    }

    /* =========================================================
       HOME
       ========================================================= */

    private void showHome() {

        root = baseRoot();

        addTitle("VIYZO");

        addLabel("India Short Video");

        videoView = new VideoView(this);

        videoView.setBackgroundColor(Color.BLACK);

        LinearLayout.LayoutParams vp =
                new LinearLayout.LayoutParams(
                        -1,
                        650);

        vp.setMargins(0, 15, 0, 15);

        root.addView(videoView, vp);

        Button select =
                button("SELECT & UPLOAD VIDEO");

        select.setOnClickListener(
                v -> videoPicker.launch("video/*"));

        root.addView(select);

        Button feed =
                button("ONLINE VIDEO FEED");

        feed.setOnClickListener(
                v -> showOnlineFeed());

        root.addView(feed);

        Button like =
                button("LIKE");

        like.setOnClickListener(
                v -> likeSelectedVideo());

        root.addView(like);

        Button comment =
                button("COMMENT");

        comment.setOnClickListener(
                v -> showCommentBox());

        root.addView(comment);

        Button comments =
                button("VIEW COMMENTS");

        comments.setOnClickListener(
                v -> showComments());

        root.addView(comments);

        Button share =
                button("SHARE VIDEO");

        share.setOnClickListener(
                v -> shareSelectedVideo());

        root.addView(share);

        Button profile =
                button("PROFILE");

        profile.setOnClickListener(
                v -> showProfile());

        root.addView(profile);

        Button search =
                button("SEARCH USERS");

        search.setOnClickListener(
                v -> searchUsers());

        root.addView(search);

        Button follow =
                button("FOLLOWERS / FOLLOWING");

        follow.setOnClickListener(
                v -> showFollowLists());

        root.addView(follow);

        Button messages =
                button("MESSAGES");

        messages.setOnClickListener(
                v -> showMessages());

        root.addView(messages);

        Button notifications =
                button("NOTIFICATIONS");

        notifications.setOnClickListener(
                v -> showNotifications());

        root.addView(notifications);

        Button report =
                button("REPORT / DISPUTE");

        report.setOnClickListener(
                v -> showReport());

        root.addView(report);

        Button delete =
                button("DELETE SELECTED VIDEO");

        delete.setOnClickListener(v -> {

            selectedVideo = null;

            if (videoView != null) {
                videoView.stopPlayback();
                videoView.setVideoURI(null);
            }

            toast("Video removed from screen");
        });

        root.addView(delete);

        Button logout =
                button("LOGOUT");

        logout.setOnClickListener(v -> {

            auth.signOut();

            currentUid = null;
            selectedVideo = null;

            showLogin();
        });

        root.addView(logout);

        setContentView(wrap());
    }

    /* =========================================================
       CLOUDINARY VIDEO UPLOAD
       ========================================================= */

    private void uploadVideoToCloudinary(Uri uri) {

        toast("Video upload started...");

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                String boundary =
                        "----VIYZO" + UUID.randomUUID();

                URL url =
                        new URL(
                                "https://api.cloudinary.com/v1_1/"
                                        + CLOUD_NAME
                                        + "/video/upload");

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary="
                                + boundary);

                connection.setConnectTimeout(30000);
                connection.setReadTimeout(120000);

                OutputStream raw =
                        connection.getOutputStream();

                BufferedOutputStream output =
                        new BufferedOutputStream(raw);

                writeTextField(
                        output,
                        boundary,
                        "upload_preset",
                        UPLOAD_PRESET);

                writeTextField(
                        output,
                        boundary,
                        "folder",
                        "viyzo");

                String fileName =
                        "viyzo_"
                                + System.currentTimeMillis()
                                + ".mp4";

                output.write(
                        ("--"
                                + boundary
                                + "\r\n")
                                .getBytes());

                output.write(
                        ("Content-Disposition: form-data; name=\"file\"; filename=\""
                                + fileName
                                + "\"\r\n")
                                .getBytes());

                output.write(
                        "Content-Type: video/mp4\r\n\r\n"
                                .getBytes());

                InputStream input =
                        getContentResolver()
                                .openInputStream(uri);

                if (input == null) {
                    throw new Exception(
                            "Unable to read selected video");
                }

                BufferedInputStream buffered =
                        new BufferedInputStream(input);

                byte[] buffer =
                        new byte[8192];

                int count;

                while ((count =
                        buffered.read(buffer)) != -1) {

                    output.write(
                            buffer,
                            0,
                            count);
                }

                buffered.close();

                output.write(
                        ("\r\n--"
                                + boundary
                                + "--\r\n")
                                .getBytes());

                output.flush();
                output.close();

                int responseCode =
                        connection.getResponseCode();

                InputStream responseStream;

                if (responseCode >= 200
                        && responseCode < 300) {

                    responseStream =
                            connection.getInputStream();

                } else {

                    responseStream =
                            connection.getErrorStream();
                }

                String response =
                        readStream(responseStream);

                if (responseCode >= 200
                        && responseCode < 300) {

                    JSONObject json =
                            new JSONObject(response);

                    String secureUrl =
                            json.optString(
                                    "secure_url",
                                    "");

                    String publicId =
                            json.optString(
                                    "public_id",
                                    "");

                    String originalName =
                            json.optString(
                                    "original_filename",
                                    fileName);

                    saveOnlineVideo(
                            secureUrl,
                            publicId,
                            originalName);

                    runOnUiThread(() ->
                            toast(
                                    "Video uploaded successfully"));

                } else {

                    runOnUiThread(() ->
                            toast(
                                    "Cloudinary upload failed"));
                }

            } catch (Exception e) {

                runOnUiThread(() ->
                        toast(
                                "Upload error: "
                                        + e.getMessage()));

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private void writeTextField(
            OutputStream output,
            String boundary,
            String name,
            String value)
            throws Exception {

        output.write(
                ("--"
                        + boundary
                        + "\r\n")
                        .getBytes());

        output.write(
                ("Content-Disposition: form-data; name=\""
                        + name
                        + "\"\r\n\r\n")
                        .getBytes());

        output.write(
                (value + "\r\n")
                        .getBytes());
    }

    private String readStream(
            InputStream stream)
            throws Exception {

        if (stream == null) {
            return "";
        }

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        byte[] buffer =
                new byte[4096];

        int count;

        while ((count =
                stream.read(buffer)) != -1) {

            output.write(
                    buffer,
                    0,
                    count);
        }

        stream.close();

        return output.toString("UTF-8");
    }

    /* =========================================================
       SAVE ONLINE VIDEO
       ========================================================= */

    private void saveOnlineVideo(
            String url,
            String publicId,
            String name) {

        if (currentUid == null
                || url == null
                || url.isEmpty()) {

            return;
        }

        Map<String, Object> video =
                new HashMap<>();

        video.put("uid", currentUid);
        video.put("videoUrl", url);
        video.put("cloudinaryUrl", url);
        video.put("publicId", publicId);
        video.put("name", name);
        video.put("likes", 0L);
        video.put("commentsCount", 0L);
        video.put("createdAt",
                FieldValue.serverTimestamp());

        db.collection("videos")
                .add(video)
                .addOnSuccessListener(
                        documentReference -> {

                            runOnUiThread(() ->
                                    toast(
                                            "Video saved online"));

                            playVideoUrl(url);
                        })
                .addOnFailureListener(
                        e -> runOnUiThread(() ->
                                toast(
                                        "Video database save failed: "
                                                + e.getMessage())));
    }

    /* =========================================================
       ONLINE FEED
       ========================================================= */

    private void showOnlineFeed() {

        root = baseRoot();

        addTitle("VIYZO ONLINE FEED");

        db.collection("videos")
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .addOnSuccessListener(snapshot -> {

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        String url =
                                doc.getString("videoUrl");

                        String uid =
                                doc.getString("uid");

                        String name =
                                doc.getString("name");

                        if (url == null
                                || url.isEmpty()) {
                            continue;
                        }

                        TextView title =
                                new TextView(this);

                        title.setText(
                                "VIDEO\n"
                                        + (name == null
                                        ? ""
                                        : name));

                        title.setTextColor(Color.WHITE);

                        title.setTextSize(17);

                        title.setPadding(
                                10,
                                20,
                                10,
                                10);

                        root.addView(title);

                        VideoView vv =
                                new VideoView(this);

                        vv.setBackgroundColor(
                                Color.BLACK);

                        LinearLayout.LayoutParams lp =
                                new LinearLayout.LayoutParams(
                                        -1,
                                        600);

                        root.addView(vv, lp);

                        vv.setVideoURI(
                                Uri.parse(url));

                        vv.setMediaController(
                                new MediaController(this));

                        vv.setOnPreparedListener(
                                mp -> {

                                    mp.setLooping(true);

                                    vv.start();
                                });

                        Button l =
                                button("LIKE");

                        l.setOnClickListener(
                                v -> likeVideoDocument(
                                        doc.getId(),
                                        uid));

                        root.addView(l);

                        Button c =
                                button("COMMENTS");

                        c.setOnClickListener(
                                v -> showVideoComments(
                                        doc.getId()));

                        root.addView(c);

                        Button s =
                                button("SHARE");

                        s.setOnClickListener(
                                v -> {

                                    Intent intent =
                                            new Intent(
                                                    Intent.ACTION_SEND);

                                    intent.setType(
                                            "text/plain");

                                    intent.putExtra(
                                            Intent.EXTRA_TEXT,
                                            url);

                                    startActivity(
                                            Intent.createChooser(
                                                    intent,
                                                    "Share Viyzo video"));
                                });

                        root.addView(s);
                    }

                    Button back =
                            button("BACK");

                    back.setOnClickListener(
                            v -> showHome());

                    root.addView(back);

                    setContentView(wrap());

                })
                .addOnFailureListener(
                        e -> {

                            addLabel(
                                    "Feed error: "
                                            + e.getMessage());

                            Button back =
                                    button("BACK");

                            back.setOnClickListener(
                                    v -> showHome());

                            root.addView(back);

                            setContentView(wrap());
                        });
    }

    /* =========================================================
       VIDEO PLAYER
       ========================================================= */

    private void playLocalVideo(Uri uri) {

        if (videoView == null) return;

        videoView.setVideoURI(uri);

        videoView.setMediaController(
                new MediaController(this));

        videoView.setOnPreparedListener(
                mp -> videoView.start());
    }

    private void playVideoUrl(String url) {

        if (videoView == null
                || url == null
                || url.isEmpty()) {
            return;
        }

        runOnUiThread(() -> {

            videoView.setVideoURI(
                    Uri.parse(url));

            videoView.setMediaController(
                    new MediaController(this));

            videoView.setOnPreparedListener(
                    mp -> videoView.start());
        });
    }

    /* =========================================================
       LIKE
       ========================================================= */

    private void likeSelectedVideo() {

        toast("Use ONLINE VIDEO FEED to like online videos");
    }

    private void likeVideoDocument(
            String videoId,
            String ownerUid) {

        if (videoId == null) return;

        db.collection("videos")
                .document(videoId)
                .set(
                        makeLikeData(),
                        SetOptions.merge())
                .addOnSuccessListener(
                        x -> {

                            toast("Liked");

                            if (ownerUid != null
                                    && !ownerUid.equals(currentUid)) {

                                addNotificationToUser(
                                        ownerUid,
                                        "like",
                                        "Someone liked your Viyzo video");
                            }
                        })
                .addOnFailureListener(
                        e -> toast(
                                "Like failed: "
                                        + e.getMessage()));
    }

    private Map<String, Object> makeLikeData() {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "likes",
                FieldValue.increment(1));

        return data;
    }

    /* =========================================================
       COMMENTS
       ========================================================= */

    private void showCommentBox() {

        if (selectedVideo == null) {

            toast(
                    "Select/upload a video first");

            return;
        }

        final EditText text =
                input("Write your comment");

        root.addView(text);

        Button send =
                button("SEND COMMENT");

        send.setOnClickListener(v -> {

            String message =
                    text.getText()
                            .toString()
                            .trim();

            if (message.isEmpty()) {
                toast("Write a comment");
                return;
            }

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "uid",
                    currentUid);

            data.put(
                    "text",
                    message);

            data.put(
                    "createdAt",
                    FieldValue.serverTimestamp());

            db.collection("comments")
                    .add(data)
                    .addOnSuccessListener(
                            x -> toast(
                                    "Comment saved"))
                    .addOnFailureListener(
                            e -> toast(
                                    "Comment failed: "
                                            + e.getMessage()));
        });

        root.addView(send);
    }

    private void showComments() {

        root = baseRoot();

        addTitle("COMMENTS");

        db.collection("comments")
                .whereEqualTo(
                        "uid",
                        currentUid)
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            for (DocumentSnapshot doc :
                                    snapshot.getDocuments()) {

                                String text =
                                        doc.getString("text");

                                addLabel(
                                        text == null
                                                ? ""
                                                : text);
                            }

                            Button back =
                                    button("BACK");

                            back.setOnClickListener(
                                    v -> showHome());

                            root.addView(back);

                            setContentView(
                                    wrap());
                        });
    }

    private void showVideoComments(
            String videoId) {

        root = baseRoot();

        addTitle("VIDEO COMMENTS");

        db.collection("videos")
                .document(videoId)
                .collection("comments")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            for (DocumentSnapshot doc :
                                    snapshot.getDocuments()) {

                                addLabel(
                                        doc.getString(
                                                "text"));
                            }

                            Button back =
                                    button("BACK");

                            back.setOnClickListener(
                                    v -> showOnlineFeed());

                            root.addView(back);

                            setContentView(
                                    wrap());
                        });
    }

    /* =========================================================
       SHARE
       ========================================================= */

    private void shareSelectedVideo() {

        if (selectedVideo == null) {

            toast("Select a video first");

            return;
        }

        Intent intent =
                new Intent(Intent.ACTION_SEND);

        intent.setType("text/plain");

        intent.putExtra(
                Intent.EXTRA_TEXT,
                selectedVideo.toString());

        startActivity(
                Intent.createChooser(
                        intent,
                        "Share Viyzo video"));
    }

    /* =========================================================
       PROFILE
       ========================================================= */

    private void showProfile() {

        root = baseRoot();

        addTitle("MY PROFILE");

        db.collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(
                        doc -> {

                            addLabel(
                                    "Name: "
                                            + safe(
                                            doc.getString("name")));

                            addLabel(
                                    "Email: "
                                            + safe(
                                            doc.getString("email")));

                            addLabel(
                                    "Followers: "
                                            + value(
                                            doc.get("followersCount")));

                            addLabel(
                                    "Following: "
                                            + value(
                                            doc.get("followingCount")));

                            addLabel(
                                    "Likes: "
                                            + value(
                                            doc.get("likesReceived")));

                            Button back =
                                    button("BACK");

                            back.setOnClickListener(
                                    v -> showHome());

                            root.addView(back);

                            setContentView(
                                    wrap());
                        });
    }

    /* =========================================================
       SEARCH USERS
       ========================================================= */

    private void searchUsers() {

        root = baseRoot();

        addTitle("SEARCH USERS");

        EditText search =
                input("Enter exact user name");

        root.addView(search);

        Button find =
                button("SEARCH");

        find.setOnClickListener(v -> {

            String q =
                    search.getText()
                            .toString()
                            .trim();

            if (q.isEmpty()) {
                toast("Enter a name");
                return;
            }

            db.collection("users")
                    .whereEqualTo(
                            "name",
                            q)
                    .get()
                    .addOnSuccessListener(
                            snapshot -> {

                                for (DocumentSnapshot doc :
                                        snapshot.getDocuments()) {

                                    String uid =
                                            doc.getId();

                                    String name =
                                            doc.getString(
                                                    "name");

                                    Button user =
                                            button(
                                                    name == null
                                                            ? "USER"
                                                            : name);

                                    user.setOnClickListener(
                                            v -> showOtherProfile(
                                                    uid));

                                    root.addView(user);
                                }
                            });
        });

        root.addView(find);

        Button back =
                button("BACK");

        back.setOnClickListener(
                v -> showHome());

        root.addView(back);

        setContentView(wrap());
    }

    /* =========================================================
       OTHER PROFILE
       ========================================================= */

    private void showOtherProfile(
            String uid) {

        root = baseRoot();

        addTitle("USER PROFILE");

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(
                        doc -> {

                            addLabel(
                                    "Name: "
                                            + safe(
                                            doc.getString(
                                                    "name")));

                            addLabel(
                                    "Followers: "
                                            + value(
                                            doc.get(
                                                    "followersCount")));

                            addLabel(
                                    "Following: "
                                            + value(
                                            doc.get(
                                                    "followingCount")));

                            Button follow =
                                    button("FOLLOW");

                            follow.setOnClickListener(
                                    v -> toggleFollow(
                                            uid));

                            root.addView(follow);

                            Button back =
                                    button("BACK");

                            back.setOnClickListener(
                                    v -> searchUsers());

                            root.addView(back);

                            setContentView(
                                    wrap());
                        });
    }

    /* =========================================================
       FOLLOW
       ========================================================= */

    private void toggleFollow(
            String targetUid) {

        if (targetUid == null
                || targetUid.equals(currentUid)) {

            toast("Cannot follow yourself");

            return;
        }

        Map<String, Object> follow =
                new HashMap<>();

        follow.put(
                "fromUid",
                currentUid);

        follow.put(
                "toUid",
                targetUid);

        follow.put(
                "createdAt",
                FieldValue.serverTimestamp());

        db.collection("users")
                .document(targetUid)
                .collection("followers")
                .document(currentUid)
                .set(follow)
                .addOnSuccessListener(
                        x -> {

                            db.collection("users")
                                    .document(targetUid)
                                    .set(
                                            incrementFollowers(),
                                            SetOptions.merge());

                            db.collection("users")
                                    .document(currentUid)
                                    .collection("following")
                                    .document(targetUid)
                                    .set(follow);

                            addNotificationToUser(
                                    targetUid,
                                    "follow",
                                    "Someone followed you");

                            toast("Followed");
                        })
                .addOnFailureListener(
                        e -> toast(
                                "Follow failed: "
                                        + e.getMessage()));
    }

    private Map<String, Object>
    incrementFollowers() {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "followersCount",
                FieldValue.increment(1));

        return data;
    }

    /* =========================================================
       FOLLOW LISTS
       ========================================================= */

    private void showFollowLists() {

        root = baseRoot();

        addTitle("FOLLOWERS / FOLLOWING");

        Button followers =
                button("MY FOLLOWERS");

        followers.setOnClickListener(
                v -> showFollowers());

        root.addView(followers);

        Button following =
                button("MY FOLLOWING");

        following.setOnClickListener(
                v -> showFollowing());

        root.addView(following);

        Button back =
                button("BACK");

        back.setOnClickListener(
                v -> showHome());

        root.addView(back);

        setContentView(wrap());
    }

    private void showFollowers() {

        root = baseRoot();

        addTitle("FOLLOWERS");

        db.collection("users")
                .document(currentUid)
                .collection("followers")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            for (DocumentSnapshot doc :
                                    snapshot.getDocuments()) {

                                addLabel(
                                        doc.getId());
                            }

                            Button back =
                                    button("BACK");

                            back.setOnClickListener(
                                    v -> showFollowLists());

                            root.addView(back);

                            setContentView(wrap());
                        });
    }

    private void showFollowing() {

        root = baseRoot();

        addTitle("FOLLOWING");

        db.collection("users")
                .document(currentUid)
                .collection("following")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            for (DocumentSnapshot doc :
                                    snapshot.getDocuments()) {

                                addLabel(
                                        doc.getId());
                            }

                            Button back =
                                    button("BACK");

                            back.setOnClickListener(
                                    v -> showFollowLists());

                            root.addView(back);

                            setContentView(wrap());
                        });
    }

    /* =========================================================
       MESSAGES
       ========================================================= */

    private void showMessages() {

        root = baseRoot();

        addTitle("MESSAGES");

        EditText receiver =
                input("Receiver UID");

        EditText message =
                input("Message");

        root.addView(receiver);
        root.addView(message);

        Button send =
                button("SEND MESSAGE");

        send.setOnClickListener(v -> {

            String uid =
                    receiver.getText()
                            .toString()
                            .trim();

            String text =
                    message.getText()
                            .toString()
                            .trim();

            if (uid.isEmpty()
                    || text.isEmpty()) {

                toast("Receiver and message required");

                return;
            }

            String chatId =
                    currentUid.compareTo(uid) < 0
                            ? currentUid + "_" + uid
                            : uid + "_" + currentUid;

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "senderUid",
                    currentUid);

            data.put(
                    "receiverUid",
                    uid);

            data.put(
                    "message",
                    text);

            data.put(
                    "createdAt",
                    FieldValue.serverTimestamp());

            db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(data)
                    .addOnSuccessListener(
                            x -> {

                                toast(
                                        "Message sent");

                                addNotificationToUser(
                                        uid,
                                        "message",
                                        "You received a Viyzo message");
                            })
                    .addOnFailureListener(
                            e -> toast(
                                    "Message failed: "
                                            + e.getMessage()));
        });

        root.addView(send);

        Button back =
                button("BACK");

        back.setOnClickListener(
                v -> showHome());

        root.addView(back);

        setContentView(wrap());
    }

    /* =========================================================
       NOTIFICATIONS
       ========================================================= */

    private void showNotifications() {

        root = baseRoot();

        addTitle("NOTIFICATIONS");

        db.collection("users")
                .document(currentUid)
                .collection("notifications")
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            for (DocumentSnapshot doc :
                                    snapshot.getDocuments()) {

                                String message =
                                        doc.getString(
                                                "message");

                                addLabel(
                                        message == null
                                                ? ""
                                                : message);
                            }

                            Button read =
                                    button(
                                            "MARK ALL AS READ");

                            read.setOnClickListener(
                                    v -> markNotificationsRead());

                            root.addView(read);

                            Button back =
                                    button("BACK");

                            back.setOnClickListener(
                                    v -> showHome());

                            root.addView(back);

                            setContentView(
                                    wrap());
                        });
    }

    private void markNotificationsRead() {

        db.collection("users")
                .document(currentUid)
                .collection("notifications")
                .whereEqualTo(
                        "read",
                        false)
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            WriteBatch batch =
                                    db.batch();

                            for (DocumentSnapshot doc :
                                    snapshot.getDocuments()) {

                                batch.update(
                                        doc.getReference(),
                                        "read",
                                        true);
                            }

                            batch.commit()
                                    .addOnSuccessListener(
                                            x -> toast(
                                                    "Notifications read"));
                        });
    }

    private void addNotificationToUser(
            String uid,
            String type,
            String message) {

        if (uid == null
                || uid.equals(currentUid)) {
            return;
        }

        Map<String, Object> n =
                new HashMap<>();

        n.put(
                "type",
                type);

        n.put(
                "message",
                message);

        n.put(
                "fromUid",
                currentUid);

        n.put(
                "read",
                false);

        n.put(
                "createdAt",
                FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .collection("notifications")
                .add(n);
    }

    /* =========================================================
       REPORT
       ========================================================= */

    private void showReport() {

        root = baseRoot();

        addTitle("REPORT / DISPUTE");

        EditText reason =
                input("Write report or dispute");

        root.addView(reason);

        Button send =
                button("SEND REPORT");

        send.setOnClickListener(v -> {

            String text =
                    reason.getText()
                            .toString()
                            .trim();

            if (text.isEmpty()) {

                toast("Write the reason");

                return;
            }

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "uid",
                    currentUid);

            data.put(
                    "reason",
                    text);

            data.put(
                    "status",
                    "open");

            data.put(
                    "createdAt",
                    FieldValue.serverTimestamp());

            db.collection("reports")
                    .add(data)
                    .addOnSuccessListener(
                            x -> {

                                toast(
                                        "Report submitted");

                                showHome();
                            })
                    .addOnFailureListener(
                            e -> toast(
                                    "Report failed: "
                                            + e.getMessage()));
        });

        root.addView(send);

        Button back =
                button("BACK");

        back.setOnClickListener(
                v -> showHome());

        root.addView(back);

        setContentView(wrap());
    }

    /* =========================================================
       NOTIFICATION CHANNEL
       ========================================================= */

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                    new NotificationChannel(
                            "viyzo_notifications",
                            "Viyzo Notifications",
                            NotificationManager
                                    .IMPORTANCE_DEFAULT);

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class);

            if (manager != null) {

                manager.createNotificationChannel(
                        channel);
            }
        }
    }

    /* =========================================================
       UI HELPERS
       ========================================================= */

    private LinearLayout baseRoot() {

        LinearLayout r =
                new LinearLayout(this);

        r.setOrientation(
                LinearLayout.VERTICAL);

        r.setPadding(
                28,
                28,
                28,
                28);

        r.setBackgroundColor(
                Color.rgb(20, 20, 24));

        return r;
    }

    private ScrollView wrap() {

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);

        scroll.addView(root);

        return scroll;
    }

    private void addTitle(
            String text) {

        TextView title =
                new TextView(this);

        title.setText(text);

        title.setTextColor(
                Color.WHITE);

        title.setTextSize(28);

        title.setGravity(
                Gravity.CENTER);

        title.setPadding(
                0,
                10,
                0,
                25);

        root.addView(title);
    }

    private void addLabel(
            String text) {

        TextView label =
                new TextView(this);

        label.setText(
                text == null
                        ? ""
                        : text);

        label.setTextColor(
                Color.WHITE);

        label.setTextSize(17);

        label.setPadding(
                10,
                12,
                10,
                12);

        root.addView(label);
    }

    private EditText input(
            String hint) {

        EditText edit =
                new EditText(this);

        edit.setHint(hint);

        edit.setTextColor(
                Color.WHITE);

        edit.setHintTextColor(
                Color.LTGRAY);

        edit.setTextSize(16);

        edit.setPadding(
                15,
                12,
                15,
                12);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        -1,
                        -2);

        lp.setMargins(
                0,
                8,
                0,
                8);

        edit.setLayoutParams(lp);

        return edit;
    }

    private Button button(
            String text) {

        Button b =
                new Button(this);

        b.setText(text);

        b.setTextSize(15);

        b.setAllCaps(false);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        -1,
                        -2);

        lp.setMargins(
                0,
                7,
                0,
                7);

        b.setLayoutParams(lp);

        return b;
    }

    private void toast(
            String message) {

        runOnUiThread(() ->
                Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG)
                        .show());
    }

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }

    private String value(
            Object value) {

        return value == null
                ? "0"
                : String.valueOf(value);
    }
}
