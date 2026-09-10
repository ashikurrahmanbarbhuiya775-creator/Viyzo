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

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout root;
    private VideoView videoView;
    private Uri selectedVideo;
    private String currentUid;

    private final ActivityResultLauncher<String> videoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedVideo = uri;

                    if (videoView != null) {
                        videoView.setVideoURI(uri);
                        videoView.setMediaController(new MediaController(this));
                        videoView.start();
                    }

                    saveVideoMetadata();
                    toast("Video selected");
                }
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        createNotificationChannel();

        if (auth.getCurrentUser() != null) {
            currentUid = auth.getCurrentUser().getUid();
            requestNotificationPermission();
            showHome();
        } else {
            showLogin();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {

            notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
            );
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel = new NotificationChannel(
                    "viyzo_notifications",
                    "Viyzo Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            channel.setDescription("Viyzo social notifications");

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showLogin() {

        root = baseRoot();

        addTitle("Welcome to VIYZO");
        addLabel("Login");

        EditText email = input("Email");
        EditText password = input("Password");

        password.setInputType(0x00000081);

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

                        requestNotificationPermission();

                        showHome();
                    })
                    .addOnFailureListener(err ->
                            toast("Login failed: " + err.getMessage()));
        });

        root.addView(login);

        Button signup = button("CREATE NEW ACCOUNT");

        signup.setOnClickListener(v -> showSignup());

        root.addView(signup);

        setContentView(wrap());
    }

    private void showSignup() {

        root = baseRoot();

        addTitle("Create VIYZO Account");

        EditText name = input("Your name");
        EditText email = input("Email");
        EditText password = input("Password");

        password.setInputType(0x00000081);

        Button create = button("SIGN UP");

        create.setOnClickListener(v -> {

            String n = name.getText().toString().trim();
            String e = email.getText().toString().trim();
            String p = password.getText().toString();

            if (n.isEmpty() || e.isEmpty() || p.length() < 6) {
                toast("Enter name, valid email and 6+ character password");
                return;
            }

            auth.createUserWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> {

                        currentUid = result.getUser().getUid();

                        Map<String, Object> user = new HashMap<>();

                        user.put("uid", currentUid);
                        user.put("name", n);
                        user.put("email", e);
                        user.put("followersCount", 0L);
                        user.put("followingCount", 0L);
                        user.put("likesReceived", 0L);
                        user.put("createdAt", FieldValue.serverTimestamp());

                        db.collection("users")
                                .document(currentUid)
                                .set(user, SetOptions.merge())
                                .addOnSuccessListener(x -> {

                                    toast("Account created");

                                    showHome();
                                })
                                .addOnFailureListener(err ->
                                        toast("Profile save failed: " +
                                                err.getMessage()));
                    })
                    .addOnFailureListener(err ->
                            toast("Sign up failed: " + err.getMessage()));
        });

        root.addView(create);

        Button back = button("BACK TO LOGIN");

        back.setOnClickListener(v -> showLogin());

        root.addView(back);

        setContentView(wrap());
    }

    private void showHome() {

        root = baseRoot();

        addTitle("VIYZO");
        addLabel("Welcome to VIYZO");

        videoView = new VideoView(this);
        videoView.setBackgroundColor(Color.BLACK);

        LinearLayout.LayoutParams vp =
                new LinearLayout.LayoutParams(-1, 520);

        vp.setMargins(0, 15, 0, 15);

        root.addView(videoView, vp);

        Button select = button("SELECT VIDEO");

        select.setOnClickListener(v ->
                videoPicker.launch("video/*"));

        root.addView(select);

        Button like = button("LIKE ❤️");

        like.setOnClickListener(v -> saveLike());

        root.addView(like);

        Button comment = button("COMMENT 💬");

        comment.setOnClickListener(v -> showCommentBox());

        root.addView(comment);

        Button comments = button("VIEW COMMENTS");

        comments.setOnClickListener(v -> showComments());

        root.addView(comments);

        Button share = button("SHARE VIDEO");

        share.setOnClickListener(v -> shareVideo());

        root.addView(share);

        Button profile = button("MY PROFILE");

        profile.setOnClickListener(v -> showProfile());

        root.addView(profile);

        Button search = button("SEARCH USERS");

        search.setOnClickListener(v -> searchUsers());

        root.addView(search);

        Button followLists = button("FOLLOWERS / FOLLOWING");

        followLists.setOnClickListener(v -> showFollowLists());

        root.addView(followLists);

        Button messages = button("MESSAGES");

        messages.setOnClickListener(v -> searchUsersForMessage());

        root.addView(messages);

        Button notifications = button("NOTIFICATIONS 🔔");

        notifications.setOnClickListener(v -> showNotifications());

        root.addView(notifications);

        Button report = button("REPORT / DISPUTE");

        report.setOnClickListener(v -> showReport());

        root.addView(report);

        Button delete = button("DELETE MY VIDEO");

        delete.setOnClickListener(v -> {

            if (selectedVideo == null) {
                toast("Select a video first");
                return;
            }

            selectedVideo = null;

            if (videoView != null) {
                videoView.stopPlayback();
                videoView.setVideoURI(null);
            }

            toast("Local video removed");
        });

        root.addView(delete);

        Button logout = button("LOGOUT");

        logout.setOnClickListener(v -> {

            auth.signOut();
            currentUid = null;
            selectedVideo = null;

            showLogin();
        });

        root.addView(logout);

        setContentView(wrap());
    }

    private void saveVideoMetadata() {

        if (currentUid == null || selectedVideo == null) {
            return;
        }

        Map<String, Object> video = new HashMap<>();

        video.put("ownerUid", currentUid);
        video.put("videoUri", selectedVideo.toString());
        video.put("likes", 0L);
        video.put("createdAt", FieldValue.serverTimestamp());

        db.collection("videos")
                .document(currentUid)
                .set(video, SetOptions.merge())
                .addOnFailureListener(e ->
                        toast("Video data save failed"));
    }

    private void saveLike() {

        if (currentUid == null) {
            toast("Login required");
            return;
        }

        if (selectedVideo == null) {
            toast("Select a video first");
            return;
        }

        DocumentReference videoRef =
                db.collection("videos")
                        .document(currentUid);

        Map<String, Object> data = new HashMap<>();

        data.put("ownerUid", currentUid);
        data.put("videoUri", selectedVideo.toString());
        data.put("likes", FieldValue.increment(1));
        data.put("updatedAt", FieldValue.serverTimestamp());

        videoRef.set(data, SetOptions.merge())
                .addOnSuccessListener(v -> {

                    toast("❤️ Like saved");

                    addNotification(
                            currentUid,
                            "like",
                            "Your video received a like"
                    );

                    showLocalNotification(
                            "Viyzo",
                            "Like saved"
                    );
                })
                .addOnFailureListener(e ->
                        toast("Like failed: " + e.getMessage()));
    }

    private void showCommentBox() {

        if (currentUid == null) {
            toast("Login required");
            return;
        }

        if (selectedVideo == null) {
            toast("Select a video first");
            return;
        }

        root = baseRoot();

        addTitle("Comment");

        EditText comment = input("Write your comment");

        Button send = button("SEND COMMENT");

        send.setOnClickListener(v -> {

            String text =
                    comment.getText().toString().trim();

            if (text.isEmpty()) {
                toast("Write a comment");
                return;
            }

            Map<String, Object> data =
                    new HashMap<>();

            data.put("uid", currentUid);
            data.put("comment", text);
            data.put("createdAt",
                    FieldValue.serverTimestamp());

            db.collection("videos")
                    .document(currentUid)
                    .collection("comments")
                    .add(data)
                    .addOnSuccessListener(x -> {

                        toast("Comment saved");

                        addNotification(
                                currentUid,
                                "comment",
                                "Your video received a comment"
                        );

                        showLocalNotification(
                                "Viyzo",
                                "Comment saved"
                        );

                        showHome();
                    })
                    .addOnFailureListener(e ->
                            toast("Comment failed: " +
                                    e.getMessage()));
        });

        root.addView(send);

        Button back = button("BACK");

        back.setOnClickListener(v -> showHome());

        root.addView(back);

        setContentView(wrap());
    }

    private void showComments() {

        if (currentUid == null ||
                selectedVideo == null) {

            toast("Select a video first");
            return;
        }

        root = baseRoot();

        addTitle("Comments");

        db.collection("videos")
                .document(currentUid)
                .collection("comments")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        addLabel("No comments yet");
                    }

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        String uid =
                                doc.getString("uid");

                        String text =
                                doc.getString("comment");

                        TextView tv =
                                new TextView(this);

                        tv.setText(
                                "👤 " +
                                (uid == null ? "" : uid) +
                                "\n" +
                                (text == null ? "" : text)
                        );

                        tv.setTextColor(Color.WHITE);
                        tv.setTextSize(16);
                        tv.setPadding(
                                15, 15, 15, 15
                        );

                        root.addView(tv);
                    }
                })
                .addOnFailureListener(e ->
                        addLabel("Could not load comments"));

        Button back = button("BACK");

        back.setOnClickListener(v -> showHome());

        root.addView(back);

        setContentView(wrap());
    }

    private void shareVideo() {

        if (selectedVideo == null) {
            toast("Select a video first");
            return;
        }

        Intent intent =
                new Intent(Intent.ACTION_SEND);

        intent.setType("text/plain");

        intent.putExtra(
                Intent.EXTRA_TEXT,
                "Watch this video on VIYZO"
        );

        startActivity(
                Intent.createChooser(
                        intent,
                        "Share Video"
                )
        );
    }

    private void showProfile() {

        root = baseRoot();

        addTitle("My Profile");

        db.collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(doc -> {

                    String name =
                            doc.getString("name");

                    String email =
                            doc.getString("email");

                    Long followers =
                            doc.getLong("followersCount");

                    Long following =
                            doc.getLong("followingCount");

                    Long likes =
                            doc.getLong("likesReceived");

                    addLabel(
                            "Name: " +
                            (name == null ? "" : name)
                    );

                    addLabel(
                            "Email: " +
                            (email == null ? "" : email)
                    );

                    addLabel(
                            "Followers: " +
                            (followers == null ?
                                    0 : followers)
                    );

                    addLabel(
                            "Following: " +
                            (following == null ?
                                    0 : following)
                    );

                    addLabel(
                            "Likes received: " +
                            (likes == null ? 0 : likes)
                    );
                })
                .addOnFailureListener(e ->
                        addLabel("Profile loading failed"));

        Button back = button("BACK");

        back.setOnClickListener(v -> showHome());

        root.addView(back);

        setContentView(wrap());
    }

    private void searchUsers() {

        root = baseRoot();

        addTitle("Search Users");

        EditText search =
                input("Enter name");

        Button find = button("SEARCH");

        find.setOnClickListener(v -> {

            String term =
                    search.getText().toString().trim();

            if (term.isEmpty()) {
                toast("Enter a name");
                return;
            }

            db.collection("users")
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        boolean found = false;

                        for (DocumentSnapshot doc :
                                snapshot.getDocuments()) {

                            String name =
                                    doc.getString("name");

                            if (name != null &&
                                    name.toLowerCase()
                                            .contains(
                                                    term.toLowerCase()
                                            )) {

                                found = true;

                                Button userButton =
                                        button(name);

                                String uid = doc.getId();

                                userButton.setOnClickListener(
                                        x ->
                                                showOtherProfile(uid)
                                );

                                root.addView(userButton);
                            }
                        }

                        if (!found) {
                            addLabel("No user found");
                        }
                    })
                    .addOnFailureListener(e ->
                            toast("Search failed"));
        });

        root.addView(find);

        Button back = button("BACK");

        back.setOnClickListener(v -> showHome());

        root.addView(back);

        setContentView(wrap());
    }

    private void showOtherProfile(String targetUid) {

        root = baseRoot();

        addTitle("User Profile");

        db.collection("users")
                .document(targetUid)
                .get()
                .addOnSuccessListener(doc -> {

                    String name =
                            doc.getString("name");

                    Long followers =
                            doc.getLong("followersCount");

                    Long following =
                            doc.getLong("followingCount");

                    addLabel(
                            "Name: " +
                            (name == null ? "" : name)
                    );

                    addLabel(
                            "Followers: " +
                            (followers == null ?
                                    0 : followers)
                    );

                    addLabel(
                            "Following: " +
                            (following == null ?
                                    0 : following)
                    );

                    if (!targetUid.equals(currentUid)) {

                        Button follow =
                                button("FOLLOW / UNFOLLOW");

                        follow.setOnClickListener(v ->
                                toggleFollow(
                                        targetUid,
                                        name == null ?
                                                "User" : name
                                ));

                        root.addView(follow);
                    }
                });

        Button back = button("BACK");

        back.setOnClickListener(v -> searchUsers());

        root.addView(back);

        setContentView(wrap());
    }

    private void toggleFollow(
            String targetUid,
            String targetName) {

        if (targetUid.equals(currentUid)) {
            toast("You cannot follow yourself");
            return;
        }

        DocumentReference followingRef =
                db.collection("users")
                        .document(currentUid)
                        .collection("following")
                        .document(targetUid);

        DocumentReference followerRef =
                db.collection("users")
                        .document(targetUid)
                        .collection("followers")
                        .document(currentUid);

        followingRef.get()
                .addOnSuccessListener(existing -> {

                    WriteBatch batch =
                            db.batch();

                    if (existing.exists()) {

                        batch.delete(followingRef);
                        batch.delete(followerRef);

                        batch.update(
                                db.collection("users")
                                        .document(currentUid),
                                "followingCount",
                                FieldValue.increment(-1)
                        );

                        batch.update(
                                db.collection("users")
                                        .document(targetUid),
                                "followersCount",
                                FieldValue.increment(-1)
                        );

                        batch.commit()
                                .addOnSuccessListener(x ->
                                        toast("Unfollowed"))
                                .addOnFailureListener(e ->
                                        toast("Unfollow failed"));

                    } else {

                        Map<String, Object> following =
                                new HashMap<>();

                        following.put(
                                "uid",
                                targetUid
                        );

                        following.put(
                                "name",
                                targetName
                        );

                        following.put(
                                "createdAt",
                                FieldValue.serverTimestamp()
                        );

                        Map<String, Object> follower =
                                new HashMap<>();

                        follower.put(
                                "uid",
                                currentUid
                        );

                        batch.set(
                                followingRef,
                                following
                        );

                        batch.set(
                                followerRef,
                                follower
                        );

                        batch.update(
                                db.collection("users")
                                        .document(currentUid),
                                "followingCount",
                                FieldValue.increment(1)
                        );

                        batch.update(
                                db.collection("users")
                                        .document(targetUid),
                                "followersCount",
                                FieldValue.increment(1)
                        );

                        batch.commit()
                                .addOnSuccessListener(x -> {

                                    toast("Followed");

                                    addNotification(
                                            targetUid,
                                            "follow",
                                            "Someone followed you"
                                    );

                                    showLocalNotification(
                                            "Viyzo",
                                            "Follow saved"
                                    );
                                })
                                .addOnFailureListener(e ->
                                        toast("Follow failed"));
                    }
                });
    }

    private void showFollowLists() {

        root = baseRoot();

        addTitle("Followers / Following");

        Button followers =
                button("MY FOLLOWERS");

        followers.setOnClickListener(
                v -> showList("followers")
        );

        root.addView(followers);

        Button following =
                button("MY FOLLOWING");

        following.setOnClickListener(
                v -> showList("following")
        );

        root.addView(following);

        Button back = button("BACK");

        back.setOnClickListener(v -> showHome());

        root.addView(back);

        setContentView(wrap());
    }

    private void showList(String type) {

        root = baseRoot();

        addTitle(
                type.equals("followers") ?
                        "Followers" :
                        "Following"
        );

        db.collection("users")
                .document(currentUid)
                .collection(type)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        addLabel("No users");
                    }

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        String name =
                                doc.getString("name");

                        if (name == null) {
                            name = doc.getId();
                        }

                        addLabel("👤 " + name);
                    }
                });

        Button back = button("BACK");

        back.setOnClickListener(v ->
                showFollowLists());

        root.addView(back);

        setContentView(wrap());
    }

    private void searchUsersForMessage() {

        root = baseRoot();

        addTitle("Start Message");

        EditText search =
                input("Search user");

        Button find =
                button("SEARCH USER");

        find.setOnClickListener(v -> {

            String term =
                    search.getText().toString().trim();

            if (term.isEmpty()) {
                toast("Enter name");
                return;
            }

            db.collection("users")
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        for (DocumentSnapshot doc :
                                snapshot.getDocuments()) {

                            String name =
                                    doc.getString("name");

                            if (name != null &&
                                    name.toLowerCase()
                                            .contains(
                                                    term.toLowerCase()
                                            )) {

                                Button b =
                                        button(name);

                                String uid =
                                        doc.getId();

                                b.setOnClickListener(
                                        x ->
                                                openChat(
                                                        uid,
                                                        name
                                                )
                                );

                                root.addView(b);
                            }
                        }
                    });
        });

        root.addView(find);

        Button back = button("BACK");

        back.setOnClickListener(v -> showHome());

        root.addView(back);

        setContentView(wrap());
    }

    private String chatId(
            String a,
            String b) {

        if (a.compareTo(b) < 0) {
            return a + "_" + b;
        }

        return b + "_" + a;
    }

    private void openChat(
            String targetUid,
            String targetName) {

        String id =
                chatId(currentUid, targetUid);

        root = baseRoot();

        addTitle("Chat: " + targetName);

        LinearLayout messages =
                new LinearLayout(this);

        messages.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(messages);

        loadMessages(id, messages);

        EditText text =
                input("Write message");

        root.addView(text);

        Button send =
                button("SEND");

        send.setOnClickListener(v -> {

            String message =
                    text.getText().toString().trim();

            if (message.isEmpty()) {
                return;
            }

            Map<String, Object> data =
                    new HashMap<>();

            data.put("senderUid", currentUid);
            data.put("receiverUid", targetUid);
            data.put("message", message);
            data.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            db.collection("chats")
                    .document(id)
                    .collection("messages")
                    .add(data)
                    .addOnSuccessListener(x -> {

                        text.setText("");

                        loadMessages(
                                id,
                                messages
                        );

                        addNotification(
                                targetUid,
                                "message",
                                "You received a new message"
                        );
                    })
                    .addOnFailureListener(e ->
                            toast("Message failed"));
        });

        root.addView(send);

        Button back =
                button("BACK");

        back.setOnClickListener(v ->
                searchUsersForMessage());

        root.addView(back);

        setContentView(wrap());
    }

    private void loadMessages(
            String id,
            LinearLayout messages) {

        messages.removeAllViews();

        db.collection("chats")
                .document(id)
                .collection("messages")
                .get()
                .addOnSuccessListener(snapshot -> {

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        String sender =
                                doc.getString("senderUid");

                        String message =
                                doc.getString("message");

                        addLabelTo(
                                messages,
                                (currentUid.equals(sender) ?
                                        "You: " :
                                        "User: ") +
                                        message
                        );
                    }
                });
    }

    private void showNotifications() {

        root = baseRoot();

        addTitle("Notifications 🔔");

        db.collection("users")
                .document(currentUid)
                .collection("notifications")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        addLabel("No notifications");
                    }

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        String type =
                                doc.getString("type");

                        String message =
                                doc.getString("message");

                        addLabel(
                                "🔔 " +
                                (type == null ?
                                        "" : type) +
                                "\n" +
                                (message == null ?
                                        "" : message)
                        );
                    }
                });

        Button mark =
                button("MARK AS READ");

        mark.setOnClickListener(
                v -> markNotificationsRead()
        );

        root.addView(mark);

        Button back =
                button("BACK");

        back.setOnClickListener(
                v -> showHome()
        );

        root.addView(back);

        setContentView(wrap());
    }

    private void markNotificationsRead() {

        db.collection("users")
                .document(currentUid)
                .collection("notifications")
                .get()
                .addOnSuccessListener(snapshot -> {

                    WriteBatch batch =
                            db.batch();

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        batch.update(
                                doc.getReference(),
                                "read",
                                true
                        );
                    }

                    batch.commit()
                            .addOnSuccessListener(
                                    x -> toast(
                                            "Notifications marked read"
                                    )
                            );
                });
    }

    private void addNotification(
            String type,
            String message) {

        addNotification(
                currentUid,
                type,
                message
        );
    }

    private void addNotification(
            String uid,
            String type,
            String message) {

        if (uid == null) {
            return;
        }

        addNotificationToUser(
                uid,
                type,
                message
        );
    }

    private void addNotificationToUser(
            String uid,
            String type,
            String message) {

        Map<String, Object> data =
                new HashMap<>();

        data.put("type", type);
        data.put("message", message);
        data.put("read", false);
        data.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(uid)
                .collection("notifications")
                .add(data);
    }

    private void showLocalNotification(
            String title,
            String message) {

        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );

        if (manager == null) {
            return;
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        "viyzo_notifications"
                )
                        .setSmallIcon(
                                android.R.drawable.ic_dialog_info
                        )
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(
                                NotificationCompat.PRIORITY_DEFAULT
                        )
                        .setAutoCancel(true);

        manager.notify(
                (int) System.currentTimeMillis(),
                builder.build()
        );
    }

    private void showReport() {

        root = baseRoot();

        addTitle("Report / Dispute");

        EditText reason =
                input("Enter your complaint");

        Button send =
                button("SUBMIT REPORT");

        send.setOnClickListener(v -> {

            String text =
                    reason.getText().toString().trim();

            if (text.isEmpty()) {
                toast("Enter complaint");
                return;
            }

            Map<String, Object> data =
                    new HashMap<>();

            data.put("uid", currentUid);
            data.put("reason", text);
            data.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );
            data.put("status", "pending");

            db.collection("reports")
                    .add(data)
                    .addOnSuccessListener(x -> {

                        toast(
                                "Report submitted"
                        );

                        addNotification(
                                currentUid,
                                "report",
                                "Your report was submitted"
                        );

                        showHome();
                    })
                    .addOnFailureListener(e ->
                            toast("Report failed"));
        });

        root.addView(send);

        Button back =
                button("BACK");

        back.setOnClickListener(
                v -> showHome()
        );

        root.addView(back);

        setContentView(wrap());
    }

    private LinearLayout baseRoot() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                25, 25, 25, 25
        );

        layout.setBackgroundColor(
                Color.rgb(18, 18, 22)
        );

        return layout;
    }

    private ScrollView wrap() {

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);

        scroll.addView(root);

        return scroll;
    }

    private void addTitle(String text) {

        TextView title =
                new TextView(this);

        title.setText(text);
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        title.setPadding(
                0, 10, 0, 25
        );

        root.addView(title);
    }

    private void addLabel(String text) {

        addLabelTo(root, text);
    }

    private void addLabelTo(
            LinearLayout parent,
            String text) {

        TextView tv =
                new TextView(this);

        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(17);
        tv.setPadding(
                10, 15, 10, 15
        );

        parent.addView(tv);
    }

    private EditText input(String hint) {

        EditText edit =
                new EditText(this);

        edit.setHint(hint);
        edit.setHintTextColor(
                Color.LTGRAY
        );
        edit.setTextColor(Color.WHITE);
        edit.setTextSize(16);

        edit.setSingleLine(false);

        edit.setPadding(
                15, 15, 15, 15
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.setMargins(
                0, 8, 0, 8
        );

        root.addView(edit, params);

        return edit;
    }

    private Button button(String text) {

        Button b =
                new Button(this);

        b.setText(text);
        b.setTextSize(15);
        b.setAllCaps(false);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.setMargins(
                0, 8, 0, 8
        );

        root.addView(b, params);

        return b;
    }

    private void toast(String text) {

        Toast.makeText(
                this,
                text,
                Toast.LENGTH_SHORT
        ).show();
    }
}
