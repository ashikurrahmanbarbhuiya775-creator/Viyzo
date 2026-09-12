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

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
                        videoView.requestFocus();
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
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
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
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
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
                        try {
                            showHome();
                        } catch (RuntimeException ex) {
                            toast("Home screen error: " + ex.getMessage());
                            showLogin();
                        }
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

                        db.collection("users").document(currentUid)
                                .set(user, SetOptions.merge())
                                .addOnSuccessListener(x -> {
                                    toast("Account created");
                                    showHome();
                                })
                                .addOnFailureListener(err ->
                                        toast("Profile save failed: " + err.getMessage()));
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
        videoView.setZOrderOnTop(true);
        videoView.setZOrderMediaOverlay(true);
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            videoView.requestFocus();
            videoView.start();
        });
        LinearLayout.LayoutParams vp =
                new LinearLayout.LayoutParams(-1, 520);
        vp.setMargins(0, 15, 0, 15);
        root.addView(videoView, vp);

        Button select = button("SELECT VIDEO");
        select.setOnClickListener(v -> videoPicker.launch("video/*"));
        root.addView(select);

        Button like = button("LIKE");
        like.setOnClickListener(v -> saveLike());
        root.addView(like);

        Button comment = button("COMMENT");
        comment.setOnClickListener(v -> showCommentBox());
        root.addView(comment);

        Button comments = button("VIEW COMMENTS");
        comments.setOnClickListener(v -> showComments());
        root.addView(comments);

        Button share = button("SHARE VIDEO");
        share.setOnClickListener(v -> shareVideo());
        root.addView(share);

        Button delete = button("DELETE SELECTED VIDEO");
        delete.setOnClickListener(v -> {
            selectedVideo = null;
            if (videoView != null) videoView.stopPlayback();
            toast("Selected video removed from this screen");
        });
        root.addView(delete);

        Button profile = button("PROFILE");
        profile.setOnClickListener(v -> showProfile());
        root.addView(profile);

        Button search = button("SEARCH USERS");
        search.setOnClickListener(v -> searchUsers());
        root.addView(search);

        Button follow = button("FOLLOWERS / FOLLOWING");
        follow.setOnClickListener(v -> showFollowLists());
        root.addView(follow);

        Button messages = button("MESSAGES");
        messages.setOnClickListener(v -> searchUsersForMessage());
        root.addView(messages);

        Button notifications = button("NOTIFICATIONS");
        notifications.setOnClickListener(v -> showNotifications());
        root.addView(notifications);

        Button report = button("REPORT / DISPUTE");
        report.setOnClickListener(v -> showReport());
        root.addView(report);

        Button admin = button("ADMIN DASHBOARD");
        admin.setOnClickListener(v -> showAdminDashboard());
        root.addView(admin);

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

    private void showAdminDashboard() {
        FirebaseUser adminUser = auth.getCurrentUser();
        if (adminUser == null) {
            toast("Please login first");
            return;
        }

        root = baseRoot();
        addTitle("VIYZO ADMIN DASHBOARD");
        addLabel("Loading dashboard...");
        setContentView(wrap());

        db.collection("users").get()
                .addOnSuccessListener(users -> {
                    int userCount = users.size();
                    db.collection("videos").get()
                            .addOnSuccessListener(videos -> {
                                int videoCount = videos.size();
                                db.collection("reports").get()
                                        .addOnSuccessListener(reports -> {
                                            int reportCount = reports.size();
                                            root.removeAllViews();
                                            addTitle("VIYZO ADMIN DASHBOARD");
                                            addLabel("Total users: " + userCount);
                                            addLabel("Total video records: " + videoCount);
                                            addLabel("Total reports: " + reportCount);
                                            addLabel("Your account: " +
                                                    (adminUser.getEmail() == null ? "" : adminUser.getEmail()));

                                            Button usersButton = button("VIEW USERS");
                                            usersButton.setOnClickListener(v -> showAdminUsers());
                                            root.addView(usersButton);

                                            Button reportsButton = button("VIEW REPORTS");
                                            reportsButton.setOnClickListener(v -> showAdminReports());
                                            root.addView(reportsButton);

                                            Button refresh = button("REFRESH DASHBOARD");
                                            refresh.setOnClickListener(v -> showAdminDashboard());
                                            root.addView(refresh);

                                            Button back = button("BACK TO HOME");
                                            back.setOnClickListener(v -> showHome());
                                            root.addView(back);
                                        })
                                        .addOnFailureListener(e ->
                                                showAdminError("Reports failed: " + e.getMessage()));
                            })
                            .addOnFailureListener(e ->
                                    showAdminError("Videos failed: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        showAdminError("Users failed: " + e.getMessage()));
    }

    private void showAdminUsers() {
        root = baseRoot();
        addTitle("ALL USERS");
        addLabel("Loading users...");
        setContentView(wrap());

        db.collection("users").orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(snapshot -> {
                    root.removeAllViews();
                    addTitle("ALL USERS");
                    if (snapshot.isEmpty()) {
                        addLabel("No users found.");
                    } else {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String name = doc.getString("name");
                            String email = doc.getString("email");
                            Long followers = doc.getLong("followersCount");
                            Long following = doc.getLong("followingCount");
                            Long likes = doc.getLong("likesReceived");
                            addLabel(
                                    "Name: " + (name == null ? "User" : name) +
                                    "\nEmail: " + (email == null ? "" : email) +
                                    "\nFollowers: " + (followers == null ? 0 : followers) +
                                    " | Following: " + (following == null ? 0 : following) +
                                    " | Likes: " + (likes == null ? 0 : likes) +
                                    "\nUID: " + doc.getId()
                            );
                        }
                    }
                    Button back = button("BACK TO ADMIN DASHBOARD");
                    back.setOnClickListener(v -> showAdminDashboard());
                    root.addView(back);
                    setContentView(wrap());
                })
                .addOnFailureListener(e ->
                        showAdminError("User list failed: " + e.getMessage()));
    }

    private void showAdminReports() {
        root = baseRoot();
        addTitle("USER REPORTS");
        addLabel("Loading reports...");
        setContentView(wrap());

        db.collection("reports").orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(snapshot -> {
                    root.removeAllViews();
                    addTitle("USER REPORTS");
                    if (snapshot.isEmpty()) {
                        addLabel("No reports found.");
                    } else {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String uid = doc.getString("uid");
                            String email = doc.getString("email");
                            String reason = doc.getString("reason");
                            String status = doc.getString("status");

                            addLabel(
                                    "Email: " + (email == null ? "" : email) +
                                    "\nReason: " + (reason == null ? "" : reason) +
                                    "\nStatus: " + (status == null ? "open" : status) +
                                    "\nUID: " + (uid == null ? "" : uid)
                            );

                            Button resolve = button("MARK RESOLVED");
                            resolve.setOnClickListener(v -> {
                                resolve.setEnabled(false);
                                doc.getReference().update("status", "resolved")
                                        .addOnSuccessListener(x -> {
                                            toast("Report resolved");
                                            showAdminReports();
                                        })
                                        .addOnFailureListener(e -> {
                                            resolve.setEnabled(true);
                                            toast("Update failed: " + e.getMessage());
                                        });
                            });
                            root.addView(resolve);
                        }
                    }
                    Button back = button("BACK TO ADMIN DASHBOARD");
                    back.setOnClickListener(v -> showAdminDashboard());
                    root.addView(back);
                    setContentView(wrap());
                })
                .addOnFailureListener(e ->
                        showAdminError("Reports list failed: " + e.getMessage()));
    }

    private void showAdminError(String message) {
        root.removeAllViews();
        addTitle("ADMIN DASHBOARD");
        addLabel(message);
        Button retry = button("RETRY");
        retry.setOnClickListener(v -> showAdminDashboard());
        root.addView(retry);
        Button back = button("BACK TO HOME");
        back.setOnClickListener(v -> showHome());
        root.addView(back);
        setContentView(wrap());
    }

    private void playSelectedVideo(Uri uri) {
        if (uri == null || videoView == null || !videoView.isAvailable()) return;
        try {
            stopVideo();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, uri);
            Surface surface = new Surface(videoView.getSurfaceTexture());
            mediaPlayer.setSurface(surface);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.start();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                toast("Video playback failed");
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            toast("Video failed: " + e.getMessage());
        }
    }

    private void stopVideo() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (Exception ignored) {}
            try { mediaPlayer.reset(); } catch (Exception ignored) {}
            try { mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    private void saveVideoMetadata() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || selectedVideo == null) return;

        DocumentReference ref = db.collection("videos").document(user.getUid());

        Map<String, Object> data = new HashMap<>();
        data.put("ownerId", user.getUid());
        data.put("ownerEmail", user.getEmail());
        data.put("videoName", selectedVideo.getLastPathSegment());
        data.put("localOnly", true);
        data.put("updatedAt", FieldValue.serverTimestamp());

        ref.set(data, SetOptions.merge())
                .addOnFailureListener(e ->
                        toast("Video data save failed: " + e.getMessage()));
    }

    private void saveLike() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || selectedVideo == null) {
            toast("First select a video");
            return;
        }

        DocumentReference videoRef =
                db.collection("videos").document(user.getUid());

        Map<String, Object> data = new HashMap<>();
        data.put("ownerId", user.getUid());
        data.put("videoName", selectedVideo.getLastPathSegment());
        data.put("localOnly", true);
        data.put("likes", FieldValue.increment(1));

        videoRef.set(data, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    toast("Liked");
                    addNotification(
                            "like",
                            "You liked your selected video."
                    );
                })
                .addOnFailureListener(e ->
                        toast("Like failed: " + e.getMessage()));
    }

    private void showCommentBox() {
        if (auth.getCurrentUser() == null || selectedVideo == null) {
            toast("First select a video");
            return;
        }

        root = baseRoot();
        addTitle("Write Comment");

        EditText comment = input("Write your comment");

        Button send = button("SEND COMMENT");
        send.setOnClickListener(v -> {
            String text = comment.getText().toString().trim();
            if (text.isEmpty()) {
                toast("Write a comment first");
                return;
            }

            String uid = auth.getCurrentUser().getUid();

            Map<String, Object> data = new HashMap<>();
            data.put("uid", uid);
            data.put("email", auth.getCurrentUser().getEmail());
            data.put("text", text);
            data.put("createdAt", FieldValue.serverTimestamp());

            db.collection("videos").document(uid)
                    .collection("comments")
                    .add(data)
                    .addOnSuccessListener(ref -> {
                        toast("Comment sent");
                        addNotification("comment", "New comment: " + text);
                        showComments();
                    })
                    .addOnFailureListener(e ->
                            toast("Comment failed: " + e.getMessage()));
        });
        root.addView(send);

        Button back = button("BACK");
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        setContentView(wrap());
    }

    private void showComments() {
        if (auth.getCurrentUser() == null || selectedVideo == null) {
            toast("First select a video");
            return;
        }

        root = baseRoot();
        addTitle("Comments");

        db.collection("videos")
                .document(auth.getCurrentUser().getUid())
                .collection("comments")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        addLabel("No comments yet.");
                    } else {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String email = doc.getString("email");
                            String text = doc.getString("text");
                            addLabel((email == null ? "User" : email) + "\n" +
                                    (text == null ? "" : text) + "\n");
                        }
                    }

                    Button back = button("BACK");
                    back.setOnClickListener(v -> showHome());
                    root.addView(back);
                    setContentView(wrap());
                })
                .addOnFailureListener(e ->
                        toast("Comments failed: " + e.getMessage()));
    }

    private void shareVideo() {
        if (selectedVideo == null) {
            toast("First select a video");
            return;
        }

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("video/*");
        send.putExtra(Intent.EXTRA_STREAM, selectedVideo);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Share video with"));
    }

    private void showProfile() {
        root = baseRoot();
        addTitle("My Profile");

        String uid = auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    Long followers = doc.getLong("followersCount");
                    Long following = doc.getLong("followingCount");
                    Long likes = doc.getLong("likesReceived");

                    addLabel("Name: " + (name == null ? "" : name));
                    addLabel("Email: " + (email == null ? "" : email));
                    addLabel("Followers: " + (followers == null ? 0 : followers));
                    addLabel("Following: " + (following == null ? 0 : following));
                    addLabel("Likes received: " + (likes == null ? 0 : likes));

                    Button back = button("BACK");
                    back.setOnClickListener(v -> showHome());
                    root.addView(back);
                    setContentView(wrap());
                })
                .addOnFailureListener(e ->
                        toast("Profile failed: " + e.getMessage()));
    }

    private void searchUsers() {
        root = baseRoot();
        addTitle("Search Users");

        EditText q = input("Enter email beginning");
        Button find = button("SEARCH");
        root.addView(find);

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        root.addView(results);

        find.setOnClickListener(v -> {
            String text = q.getText().toString().trim().toLowerCase();
            if (text.isEmpty()) {
                toast("Enter an email");
                return;
            }

            results.removeAllViews();

            db.collection("users")
                    .orderBy("email")
                    .startAt(text)
                    .endAt(text + "\uf8ff")
                    .limit(20)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.isEmpty()) {
                            addLabelTo(results, "No users found");
                            return;
                        }

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String uid = doc.getId();
                            String name = doc.getString("name");
                            String email = doc.getString("email");

                            Button b = new Button(this);
                            b.setText((name == null ? "User" : name) +
                                    "\n" + (email == null ? "" : email));
                            b.setOnClickListener(v2 -> showOtherProfile(uid));
                            results.addView(b);
                        }
                    })
                    .addOnFailureListener(e ->
                            addLabelTo(results, "Search failed: " + e.getMessage()));
        });

        Button back = button("BACK");
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        setContentView(wrap());
    }

    private void showOtherProfile(String targetUid) {
        if (targetUid.equals(currentUid)) {
            showProfile();
            return;
        }

        root = baseRoot();
        addTitle("User Profile");

        db.collection("users").document(targetUid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    Long followers = doc.getLong("followersCount");
                    Long following = doc.getLong("followingCount");

                    addLabel("Name: " + (name == null ? "" : name));
                    addLabel("Email: " + (email == null ? "" : email));
                    addLabel("Followers: " + (followers == null ? 0 : followers));
                    addLabel("Following: " + (following == null ? 0 : following));

                    Button f = button("FOLLOW / UNFOLLOW");
                    f.setOnClickListener(v -> toggleFollow(targetUid, name));
                    root.addView(f);

                    Button msg = button("SEND MESSAGE");
                    msg.setOnClickListener(v -> openChat(targetUid, name));
                    root.addView(msg);

                    Button back = button("BACK");
                    back.setOnClickListener(v -> searchUsers());
                    root.addView(back);

                    setContentView(wrap());
                })
                .addOnFailureListener(e ->
                        toast("Profile failed: " + e.getMessage()));
    }

    private void toggleFollow(String targetUid, String targetName) {
        String myUid = auth.getCurrentUser().getUid();

        DocumentReference followingRef = db.collection("users")
                .document(myUid)
                .collection("following")
                .document(targetUid);

        DocumentReference followerRef = db.collection("users")
                .document(targetUid)
                .collection("followers")
                .document(myUid);

        followingRef.get().addOnSuccessListener(existing -> {
            WriteBatch batch = db.batch();

            if (existing.exists()) {
                batch.delete(followingRef);
                batch.delete(followerRef);
                batch.update(db.collection("users").document(myUid),
                        "followingCount", FieldValue.increment(-1));
                batch.update(db.collection("users").document(targetUid),
                        "followersCount", FieldValue.increment(-1));

                batch.commit()
                        .addOnSuccessListener(x -> toast("Unfollowed"))
                        .addOnFailureListener(e ->
                                toast("Unfollow failed: " + e.getMessage()));
            } else {
                Map<String, Object> followData = new HashMap<>();
                followData.put("uid", myUid);
                followData.put("createdAt", FieldValue.serverTimestamp());

                batch.set(followingRef, followData);
                batch.set(followerRef, followData);
                batch.update(db.collection("users").document(myUid),
                        "followingCount", FieldValue.increment(1));
                batch.update(db.collection("users").document(targetUid),
                        "followersCount", FieldValue.increment(1));

                batch.commit()
                        .addOnSuccessListener(x -> {
                            toast("Followed");
                            addNotificationToUser(
                                    targetUid,
                                    "follow",
                                    "You have a new follower: " +
                                            auth.getCurrentUser().getEmail()
                            );
                            showLocalNotification("New follower",
                                    "Someone followed you on Viyzo");
                        })
                        .addOnFailureListener(e ->
                                toast("Follow failed: " + e.getMessage()));
            }
        });
    }

    private void showFollowLists() {
        root = baseRoot();
        addTitle("Followers / Following");

        Button followers = button("MY FOLLOWERS");
        followers.setOnClickListener(v -> showList("followers"));
        root.addView(followers);

        Button following = button("MY FOLLOWING");
        following.setOnClickListener(v -> showList("following"));
        root.addView(following);

        Button back = button("BACK");
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        setContentView(wrap());
    }

    private void showList(String type) {
        root = baseRoot();
        addTitle(type.equals("followers") ? "My Followers" : "My Following");

        db.collection("users").document(currentUid)
                .collection(type)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        addLabel("No users yet.");
                    } else {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            addLabel("User ID: " + doc.getId());
                        }
                    }

                    Button back = button("BACK");
                    back.setOnClickListener(v -> showFollowLists());
                    root.addView(back);
                    setContentView(wrap());
                })
                .addOnFailureListener(e ->
                        toast("List failed: " + e.getMessage()));
    }

    private void searchUsersForMessage() {
        root = baseRoot();
        addTitle("Find User For Message");

        EditText q = input("User email beginning");
        Button find = button("SEARCH");
        root.addView(find);

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        root.addView(results);

        find.setOnClickListener(v -> {
            String text = q.getText().toString().trim().toLowerCase();
            if (text.isEmpty()) return;

            results.removeAllViews();

            db.collection("users")
                    .orderBy("email")
                    .startAt(text)
                    .endAt(text + "\uf8ff")
                    .limit(20)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            if (doc.getId().equals(currentUid)) continue;

                            String name = doc.getString("name");
                            Button b = button("MESSAGE: " +
                                    (name == null ? "User" : name));
                            b.setOnClickListener(v2 ->
                                    openChat(doc.getId(), name));
                            results.addView(b);
                        }
                    })
                    .addOnFailureListener(e ->
                            addLabelTo(results, "Search failed: " + e.getMessage()));
        });

        Button back = button("BACK");
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        setContentView(wrap());
    }

    private String chatId(String a, String b) {
        return a.compareTo(b) < 0 ? a + "_" + b : b + "_" + a;
    }

    private void openChat(String targetUid, String targetName) {
        String id = chatId(currentUid, targetUid);

        root = baseRoot();
        addTitle("Chat with " + (targetName == null ? "User" : targetName));

        LinearLayout messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        root.addView(messages);

        EditText text = input("Type message");
        root.addView(text);

        Button send = button("SEND");
        root.addView(send);

        Button back = button("BACK");
        root.addView(back);

        loadMessages(id, messages);

        send.setOnClickListener(v -> {
            String msg = text.getText().toString().trim();
            if (msg.isEmpty()) return;

            Map<String, Object> data = new HashMap<>();
            data.put("senderId", currentUid);
            data.put("receiverId", targetUid);
            data.put("text", msg);
            data.put("createdAt", FieldValue.serverTimestamp());

            db.collection("chats").document(id)
                    .collection("messages")
                    .add(data)
                    .addOnSuccessListener(x -> {
                        text.setText("");
                        loadMessages(id, messages);
                        addNotificationToUser(targetUid, "message",
                                "New message from " +
                                        auth.getCurrentUser().getEmail());
                        showLocalNotification("Viyzo message", "New message sent");
                    })
                    .addOnFailureListener(e ->
                            toast("Message failed: " + e.getMessage()));
        });

        back.setOnClickListener(v -> showHome());
        setContentView(wrap());
    }

    private void loadMessages(String chatId, LinearLayout messages) {
        db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    messages.removeAllViews();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String sender = doc.getString("senderId");
                        String text = doc.getString("text");
                        addLabelTo(messages,
                                (currentUid.equals(sender) ? "You: " : "Them: ") +
                                        (text == null ? "" : text));
                    }
                })
                .addOnFailureListener(e ->
                        addLabelTo(messages, "Messages failed: " + e.getMessage()));
    }

    private void showNotifications() {
        root = baseRoot();
        addTitle("Notifications");

        db.collection("users").document(currentUid)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        addLabel("No notifications yet.");
                    } else {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String msg = doc.getString("message");
                            Boolean read = doc.getBoolean("read");
                            addLabel((Boolean.TRUE.equals(read) ? "" : "● ") +
                                    (msg == null ? "Notification" : msg));
                        }
                    }

                    Button mark = button("MARK ALL AS READ");
                    mark.setOnClickListener(v -> markNotificationsRead());
                    root.addView(mark);

                    Button back = button("BACK");
                    back.setOnClickListener(v -> showHome());
                    root.addView(back);

                    setContentView(wrap());
                })
                .addOnFailureListener(e ->
                        toast("Notifications failed: " + e.getMessage()));
    }

    private void markNotificationsRead() {
        db.collection("users").document(currentUid)
                .collection("notifications")
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.update(doc.getReference(), "read", true);
                    }
                    batch.commit()
                            .addOnSuccessListener(x -> {
                                toast("Notifications marked as read");
                                showNotifications();
                            })
                            .addOnFailureListener(e ->
                                    toast("Update failed: " + e.getMessage()));
                });
    }

    private void addNotification(String type, String message) {
        addNotificationToUser(currentUid, type, message);
        showLocalNotification("Viyzo", message);
    }

    private void addNotificationToUser(String uid, String type, String message) {
        if (uid == null) return;

        Map<String, Object> n = new HashMap<>();
        n.put("type", type);
        n.put("message", message);
        n.put("fromUid", currentUid);
        n.put("fromEmail",
                auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getEmail());
        n.put("read", false);
        n.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid)
                .collection("notifications")
                .add(n);
    }

    private void showLocalNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, "viyzo_notifications")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void showReport() {
        root = baseRoot();
        addTitle("Report / Dispute");

        EditText reason = input("Write report or dispute");
        Button send = button("SEND REPORT");

        send.setOnClickListener(v -> {
            String r = reason.getText().toString().trim();
            if (r.isEmpty()) {
                toast("Write the reason");
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("uid", currentUid);
            data.put("email", auth.getCurrentUser().getEmail());
            data.put("reason", r);
            data.put("status", "open");
            data.put("createdAt", FieldValue.serverTimestamp());

            db.collection("reports")
                    .add(data)
                    .addOnSuccessListener(x -> {
                        toast("Report submitted");
                        addNotification("report", "Your report was submitted.");
                        showHome();
                    })
                    .addOnFailureListener(e ->
                            toast("Report failed: " + e.getMessage()));
        });

        root.addView(send);

        Button back = button("BACK");
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        setContentView(wrap());
    }

    private LinearLayout baseRoot() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(28, 28, 28, 28);
        r.setBackgroundColor(Color.rgb(20, 20, 24));
        return r;
    }

    private ScrollView wrap() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        return scroll;
    }

    private void addTitle(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(28);
        t.setTypeface(null, 1);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 10, 0, 25);
        root.addView(t);
    }

    private void addLabel(String text) {
        addLabelTo(root, text);
    }

    private void addLabelTo(LinearLayout parent, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(17);
        t.setPadding(10, 14, 10, 14);
        parent.addView(t);
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.LTGRAY);
        e.setTextColor(Color.WHITE);
        e.setTextSize(17);
        e.setPadding(15, 12, 15, 12);
        root.addView(e,
                new LinearLayout.LayoutParams(-1, -2));
        return e;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setPadding(10, 10, 10, 10);
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 8, 0, 8);
        b.setLayoutParams(p);
        return b;
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
