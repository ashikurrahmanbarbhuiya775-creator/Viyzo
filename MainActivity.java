package com.viyzo.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private LinearLayout main;
    private SharedPreferences prefs;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private int likeCount = 12;
    private boolean liked = false;
    private boolean followed = false;

    private Uri selectedVideo = null;

    private final ArrayList<String> comments = new ArrayList<>();
    private final ArrayList<String> messages = new ArrayList<>();

    private static final int VIDEO_PICKER = 1001;

    private int dp(float n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setPadding(dp(12), dp(8), dp(12), dp(8));
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("viyzo", MODE_PRIVATE);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        comments.add("Nice video!");
        messages.add("Welcome to VIYZO Messenger");

        showHome();
    }

    private void baseScreen(String title) {
        main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(8), dp(8), dp(8), dp(8));

        TextView heading = text(title, 24);
        heading.setGravity(Gravity.CENTER);
        main.addView(
                heading,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(60)
                )
        );

        setContentView(main);
    }

    private void showHome() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            showLoginRequired();
            return;
        }

        baseScreen("VIYZO");

        TextView welcome = text(
                "Welcome to VIYZO\n@" +
                        getDisplayName(),
                18
        );
        welcome.setGravity(Gravity.CENTER);
        main.addView(welcome);

        VideoView videoView = new VideoView(this);

        LinearLayout.LayoutParams videoParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(360)
                );

        videoParams.setMargins(
                dp(4),
                dp(8),
                dp(4),
                dp(8)
        );

        main.addView(videoView, videoParams);

        if (selectedVideo != null) {
            videoView.setVideoURI(selectedVideo);
            videoView.start();
        }

        Button playButton = button("▶ Play Video");

        playButton.setOnClickListener(v -> {

            if (selectedVideo != null) {
                videoView.setVideoURI(selectedVideo);
                videoView.start();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("VIYZO")
                        .setMessage("पहले Upload से वीडियो चुनिए।")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });

        main.addView(playButton);

        Button likeButton =
                button("❤️ Like " + likeCount);

        likeButton.setOnClickListener(v -> {

            if (liked) {
                likeCount--;
                liked = false;
            } else {
                likeCount++;
                liked = true;
            }

            likeButton.setText(
                    "❤️ Like " + likeCount
            );
        });

        main.addView(likeButton);

        Button commentButton =
                button("💬 Comment");

        commentButton.setOnClickListener(
                v -> showCommentBox()
        );

        main.addView(commentButton);

        Button shareButton =
                button("↗ Share");

        shareButton.setOnClickListener(v -> {

            Intent shareIntent =
                    new Intent(Intent.ACTION_SEND);

            shareIntent.setType("text/plain");

            shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Watch this video on VIYZO!"
            );

            startActivity(
                    Intent.createChooser(
                            shareIntent,
                            "Share with"
                    )
            );
        });

        main.addView(shareButton);

        Button followButton =
                button(
                        followed
                                ? "Following ✓"
                                : "Follow"
                );

        followButton.setOnClickListener(v -> {

            followed = !followed;

            followButton.setText(
                    followed
                            ? "Following ✓"
                            : "Follow"
            );
        });

        main.addView(followButton);

        addBottomNavigation();
    }

    private void showLoginRequired() {

        baseScreen("Welcome to VIYZO");

        TextView info = text(
                "VIYZO इस्तेमाल करने के लिए\nपहले Login या Signup करें।",
                20
        );

        info.setGravity(Gravity.CENTER);
        main.addView(info);

        Button login = button("Login");

        login.setOnClickListener(
                v -> showLogin()
        );

        main.addView(login);

        Button signup = button("Create Account");

        signup.setOnClickListener(
                v -> showSignup()
        );

        main.addView(signup);
    }

    private void showCommentBox() {

        baseScreen("Comments");

        EditText commentInput =
                new EditText(this);

        commentInput.setHint(
                "Write a comment..."
        );

        main.addView(
                commentInput,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(60)
                )
        );

        Button post = button("Post Comment");

        post.setOnClickListener(v -> {

            String comment =
                    commentInput.getText()
                            .toString()
                            .trim();

            if (comment.isEmpty()) {
                commentInput.setError(
                        "Comment लिखिए"
                );
                return;
            }

            comments.add(comment);

            commentInput.setText("");

            showCommentBox();
        });

        main.addView(post);

        for (String comment : comments) {

            TextView item =
                    text("💬 " + comment, 16);

            main.addView(item);
        }

        addBackButton();
    }

    private void showMessenger() {

        baseScreen("Messenger");

        TextView title =
                text(
                        "VIYZO Messenger",
                        22
                );

        title.setGravity(Gravity.CENTER);

        main.addView(title);

        for (String message : messages) {

            main.addView(
                    text(
                            "💬 " + message,
                            16
                    )
            );
        }

        EditText messageInput =
                new EditText(this);

        messageInput.setHint(
                "Write a message..."
        );

        main.addView(messageInput);

        Button send =
                button("Send");

        send.setOnClickListener(v -> {

            String message =
                    messageInput.getText()
                            .toString()
                            .trim();

            if (message.isEmpty()) {
                return;
            }

            messages.add(message);

            messageInput.setText("");

            showMessenger();
        });

        main.addView(send);

        addBackButton();
    }

    private void showProfile() {

        baseScreen("Profile");

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            TextView info =
                    text(
                            "You are not logged in.",
                            18
                    );

            main.addView(info);

            Button login =
                    button("Login");

            login.setOnClickListener(
                    v -> showLogin()
            );

            main.addView(login);

            Button signup =
                    button("Create Account");

            signup.setOnClickListener(
                    v -> showSignup()
            );

            main.addView(signup);

            addBackButton();
            return;
        }

        String name =
                prefs.getString(
                        "name",
                        "VIYZO User"
                );

        String email =
                user.getEmail();

        TextView profile =
                text(
                        "👤 " + name +
                                "\n\n📧 " +
                                (email == null
                                        ? ""
                                        : email),
                        19
                );

        profile.setGravity(Gravity.CENTER);

        main.addView(profile);

        Button logout =
                button("Logout");

        logout.setOnClickListener(v -> {

            auth.signOut();

            prefs.edit()
                    .clear()
                    .apply();

            showHome();
        });

        main.addView(logout);

        addBackButton();
    }

    private void showLogin() {

        baseScreen("VIYZO Login");

        EditText emailInput =
                new EditText(this);

        emailInput.setHint(
                "Email"
        );

        emailInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        main.addView(emailInput);

        EditText passwordInput =
                new EditText(this);

        passwordInput.setHint(
                "Password"
        );

        passwordInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        main.addView(passwordInput);

        Button login =
                button("Login");

        login.setOnClickListener(v -> {

            String email =
                    emailInput.getText()
                            .toString()
                            .trim();

            String password =
                    passwordInput.getText()
                            .toString();

            if (email.isEmpty()) {
                emailInput.setError(
                        "Email डालिए"
                );
                return;
            }

            if (password.isEmpty()) {
                passwordInput.setError(
                        "Password डालिए"
                );
                return;
            }

            login.setEnabled(false);
            login.setText("Logging in...");

            auth.signInWithEmailAndPassword(
                    email,
                    password
            ).addOnCompleteListener(task -> {

                if (task.isSuccessful()) {

                    Toast.makeText(
                            this,
                            "Login successful",
                            Toast.LENGTH_SHORT
                    ).show();

                    showHome();

                } else {

                    login.setEnabled(true);
                    login.setText("Login");

                    String error =
                            task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Login failed";

                    new AlertDialog.Builder(this)
                            .setTitle("Login failed")
                            .setMessage(error)
                            .setPositiveButton(
                                    "OK",
                                    null
                            )
                            .show();
                }
            });
        });

        main.addView(login);

        Button signup =
                button("Create New Account");

        signup.setOnClickListener(
                v -> showSignup()
        );

        main.addView(signup);

        addBackButton();
    }

    private void showSignup() {

        baseScreen("Create VIYZO Account");

        EditText nameInput =
                new EditText(this);

        nameInput.setHint(
                "Your Name"
        );

        main.addView(nameInput);

        EditText emailInput =
                new EditText(this);

        emailInput.setHint(
                "Email"
        );

        emailInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        main.addView(emailInput);

        EditText passwordInput =
                new EditText(this);

        passwordInput.setHint(
                "Password (minimum 6 characters)"
        );

        passwordInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        main.addView(passwordInput);

        Button signup =
                button("Create Account");

        signup.setOnClickListener(v -> {

            String name =
                    nameInput.getText()
                            .toString()
                            .trim();

            String email =
                    emailInput.getText()
                            .toString()
                            .trim();

            String password =
                    passwordInput.getText()
                            .toString();

            if (name.isEmpty()) {
                nameInput.setError(
                        "Name डालिए"
                );
                return;
            }

            if (email.isEmpty()) {
                emailInput.setError(
                        "Email डालिए"
                );
                return;
            }

            if (password.length() < 6) {
                passwordInput.setError(
                        "Password कम से कम 6 characters का होना चाहिए"
                );
                return;
            }

            signup.setEnabled(false);
            signup.setText("Creating account...");

            auth.createUserWithEmailAndPassword(
                    email,
                    password
            ).addOnCompleteListener(task -> {

                if (task.isSuccessful()) {

                    FirebaseUser user =
                            auth.getCurrentUser();

                    if (user != null) {

                        String uid =
                                user.getUid();

                        Map<String, Object>
                                profile =
                                new HashMap<>();

                        profile.put(
                                "name",
                                name
                        );

                        profile.put(
                                "email",
                                email
                        );

                        profile.put(
                                "uid",
                                uid
                        );

                        db.collection("users")
                                .document(uid)
                                .set(profile)
                                .addOnCompleteListener(
                                        firestoreTask -> {

                                            prefs.edit()
                                                    .putString(
                                                            "name",
                                                            name
                                                    )
                                                    .apply();

                                            Toast.makeText(
                                                    this,
                                                    "Account created successfully",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            showHome();
                                        }
                                );
                    }

                } else {

                    signup.setEnabled(true);
                    signup.setText(
                            "Create Account"
                    );

                    String error =
                            task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Signup failed";

                    new AlertDialog.Builder(this)
                            .setTitle("Signup failed")
                            .setMessage(error)
                            .setPositiveButton(
                                    "OK",
                                    null
                            )
                            .show();
                }
            });
        });

        main.addView(signup);

        Button login =
                button("Already have an account? Login");

        login.setOnClickListener(
                v -> showLogin()
        );

        main.addView(login);

        addBackButton();
    }

    private void uploadVideo() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType("video/*");

        startActivityForResult(
                intent,
                VIDEO_PICKER
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

        if (requestCode == VIDEO_PICKER &&
                resultCode == RESULT_OK &&
                data != null) {

            selectedVideo =
                    data.getData();

            if (selectedVideo != null) {

                try {
                    getContentResolver()
                            .takePersistableUriPermission(
                                    selectedVideo,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                } catch (Exception ignored) {
                }

                Toast.makeText(
                        this,
                        "Video selected successfully",
                        Toast.LENGTH_SHORT
                ).show();

                showHome();
            }
        }
    }

    private void addBottomNavigation() {

        LinearLayout nav =
                new LinearLayout(this);

        nav.setOrientation(
                LinearLayout.HORIZONTAL
        );

        nav.setGravity(
                Gravity.CENTER
        );

        Button home =
                button("🏠 Home");

        home.setOnClickListener(
                v -> showHome()
        );

        nav.addView(
                home,
                new LinearLayout.LayoutParams(
                        0,
                        dp(60),
                        1
                )
        );

        Button upload =
                button("⬆ Upload");

        upload.setOnClickListener(
                v -> uploadVideo()
        );

        nav.addView(
                upload,
                new LinearLayout.LayoutParams(
                        0,
                        dp(60),
                        1
                )
        );

        Button messenger =
                button("💬 Messenger");

        messenger.setOnClickListener(
                v -> showMessenger()
        );

        nav.addView(
                messenger,
                new LinearLayout.LayoutParams(
                        0,
                        dp(60),
                        1
                )
        );

        Button profile =
                button("👤 Profile");

        profile.setOnClickListener(
                v -> showProfile()
        );

        nav.addView(
                profile,
                new LinearLayout.LayoutParams(
                        0,
                        dp(60),
                        1
                )
        );

        main.addView(nav);
    }

    private void addBackButton() {

        Button back =
                button("← Back");

        back.setOnClickListener(
                v -> showHome()
        );

        main.addView(back);
    }

    private String getDisplayName() {

        String name =
                prefs.getString(
                        "name",
                        ""
                );

        if (!name.isEmpty()) {
            return name;
        }

        FirebaseUser user =
                auth.getCurrentUser();

        if (user != null &&
                user.getEmail() != null) {

            return user.getEmail();
        }

        return "VIYZO User";
    }

    @Override
    public void onBackPressed() {

        showHome();
    }
}
