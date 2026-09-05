package com.viyzo.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;

public class MainActivity extends Activity {

    LinearLayout main;
    SharedPreferences prefs;

    int likeCount = 12;
    boolean liked = false;
    boolean followed = false;

    Uri selectedVideo = null;

    ArrayList<String> comments = new ArrayList<>();
    ArrayList<String> messages = new ArrayList<>();

    int dp(float n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(0xFFFFFFFF);
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        return t;
    }

    Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("viyzo", MODE_PRIVATE);

        comments.add("Nice video!");
        messages.add("Welcome to VIYZO Messenger");

        showHome();
    }

    void baseScreen(String title) {
        main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(0xFF111111);
        main.setPadding(dp(10), dp(10), dp(10), dp(10));

        TextView header = text(title, 25);
        header.setGravity(Gravity.CENTER);
        header.setTypeface(null, 1);

        main.addView(header,
                new LinearLayout.LayoutParams(-1, dp(55)));

        setContentView(main);
    }

    void showHome() {
        baseScreen("VIYZO");

        TextView videoTitle = text(
                "VIYZO VIDEO\n\nShort videos will appear here",
                19
        );
        videoTitle.setGravity(Gravity.CENTER);
        videoTitle.setBackgroundColor(0xFF222222);

        main.addView(videoTitle,
                new LinearLayout.LayoutParams(-1, dp(260)));

        if (selectedVideo != null) {
            VideoView video = new VideoView(this);
            video.setVideoURI(selectedVideo);

            MediaController controller = new MediaController(this);
            video.setMediaController(controller);
            controller.setAnchorView(video);

            main.removeView(videoTitle);
            main.addView(video,
                    new LinearLayout.LayoutParams(-1, dp(300)));

            video.start();
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button like = button("LIKE " + likeCount);
        Button comment = button("COMMENT");
        Button share = button("SHARE");

        actions.addView(like,
                new LinearLayout.LayoutParams(0, dp(55), 1));
        actions.addView(comment,
                new LinearLayout.LayoutParams(0, dp(55), 1));
        actions.addView(share,
                new LinearLayout.LayoutParams(0, dp(55), 1));

        main.addView(actions);

        LinearLayout second = new LinearLayout(this);
        second.setOrientation(LinearLayout.HORIZONTAL);

        Button follow = button(followed ? "FOLLOWING" : "FOLLOW");
        Button messenger = button("MESSENGER");

        second.addView(follow,
                new LinearLayout.LayoutParams(0, dp(55), 1));
        second.addView(messenger,
                new LinearLayout.LayoutParams(0, dp(55), 1));

        main.addView(second);

        TextView commentList = text("", 15);

        for (String c : comments) {
            commentList.append("\n💬 " + c);
        }

        main.addView(commentList);

        like.setOnClickListener(v -> {
            if (!liked) {
                likeCount++;
                liked = true;
            } else {
                likeCount--;
                liked = false;
            }

            like.setText("LIKE " + likeCount);
        });

        comment.setOnClickListener(v -> showCommentBox());

        share.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Watch this video on VIYZO!"
            );
            startActivity(Intent.createChooser(
                    shareIntent,
                    "Share VIYZO Video"
            ));
        });

        follow.setOnClickListener(v -> {
            followed = !followed;
            follow.setText(followed ? "FOLLOWING" : "FOLLOW");
        });

        messenger.setOnClickListener(v -> showMessenger());

        addBottomNavigation();
    }

    void showCommentBox() {

        final EditText input = new EditText(this);
        input.setHint("Write a comment");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Comment")
                .setView(input)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("POST", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String c = input.getText().toString().trim();

                        if (!c.isEmpty()) {
                            comments.add(c);
                            dialog.dismiss();
                            showHome();
                        }
                    });
        });

        dialog.show();
    }

    void showMessenger() {
        baseScreen("MESSENGER");

        TextView list = text("", 17);

        for (String m : messages) {
            list.append("\n💬 " + m + "\n");
        }

        main.addView(list,
                new LinearLayout.LayoutParams(
                        -1, 0, 1
                ));

        LinearLayout sendBox = new LinearLayout(this);
        sendBox.setOrientation(LinearLayout.HORIZONTAL);

        EditText message = new EditText(this);
        message.setHint("Type message");
        message.setTextColor(0xFFFFFFFF);
        message.setHintTextColor(0xFFAAAAAA);

        Button send = button("SEND");

        sendBox.addView(message,
                new LinearLayout.LayoutParams(0, dp(60), 1));

        sendBox.addView(send,
                new LinearLayout.LayoutParams(dp(100), dp(60)));

        main.addView(sendBox);

        send.setOnClickListener(v -> {

            String m = message.getText().toString().trim();

            if (!m.isEmpty()) {
                messages.add(m);
                showMessenger();
            }
        });

        addBackButton();
    }

    void showProfile() {
        baseScreen("PROFILE");

        String name = prefs.getString("name", "");

        if (name.isEmpty()) {

            TextView info = text(
                    "Welcome to VIYZO\n\nPlease Login or Signup",
                    19
            );
            info.setGravity(Gravity.CENTER);

            main.addView(info,
                    new LinearLayout.LayoutParams(
                            -1, dp(150)
                    ));

            Button login = button("LOGIN");
            Button signup = button("SIGNUP");

            main.addView(login);
            main.addView(signup);

            login.setOnClickListener(v -> showLogin());
            signup.setOnClickListener(v -> showSignup());

        } else {

            TextView profile = text(
                    "👤 PROFILE\n\nName: " + name +
                    "\n\nWelcome to VIYZO!",
                    20
            );

            main.addView(profile,
                    new LinearLayout.LayoutParams(
                            -1, dp(220)
                    ));

            Button logout = button("LOGOUT");
            main.addView(logout);

            logout.setOnClickListener(v -> {
                prefs.edit().clear().apply();
                showProfile();
            });
        }

        addBottomNavigation();
    }

    void showLogin() {
        baseScreen("LOGIN");

        EditText email = new EditText(this);
        email.setHint("Email");
        email.setTextColor(0xFFFFFFFF);
        email.setHintTextColor(0xFFAAAAAA);

        EditText password = new EditText(this);
        password.setHint("Password");
        password.setTextColor(0xFFFFFFFF);
        password.setHintTextColor(0xFFAAAAAA);
        password.setInputType(0x81);

        Button login = button("LOGIN");

        main.addView(email);
        main.addView(password);
        main.addView(login);

        login.setOnClickListener(v -> {

            String e = email.getText().toString().trim();

            if (e.isEmpty()) {
                Toast.makeText(
                        this,
                        "Email डालिए",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String name = e;

            prefs.edit()
                    .putString("name", name)
                    .apply();

            Toast.makeText(
                    this,
                    "Login successful",
                    Toast.LENGTH_SHORT
            ).show();

            showProfile();
        });

        Button signup = button("CREATE NEW ACCOUNT");

        main.addView(signup);

        signup.setOnClickListener(v -> showSignup());

        addBackButton();
    }

    void showSignup() {
        baseScreen("SIGNUP");

        EditText name = new EditText(this);
        name.setHint("Your Name");
        name.setTextColor(0xFFFFFFFF);
        name.setHintTextColor(0xFFAAAAAA);

        EditText email = new EditText(this);
        email.setHint("Email");
        email.setTextColor(0xFFFFFFFF);
        email.setHintTextColor(0xFFAAAAAA);

        EditText password = new EditText(this);
        password.setHint("Password");
        password.setTextColor(0xFFFFFFFF);
        password.setHintTextColor(0xFFAAAAAA);
        password.setInputType(0x81);

        Button signup = button("SIGN UP");

        main.addView(name);
        main.addView(email);
        main.addView(password);
        main.addView(signup);

        signup.setOnClickListener(v -> {

            String n = name.getText().toString().trim();

            if (n.isEmpty()) {
                Toast.makeText(
                        this,
                        "Name डालिए",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            prefs.edit()
                    .putString("name", n)
                    .apply();

            Toast.makeText(
                    this,
                    "Account created",
                    Toast.LENGTH_SHORT
            ).show();

            showProfile();
        });

        addBackButton();
    }

    void uploadVideo() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");

        startActivityForResult(intent, 100);
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

        if (requestCode == 100 &&
                resultCode == RESULT_OK &&
                data != null) {

            selectedVideo = data.getData();

            Toast.makeText(
                    this,
                    "Video selected",
                    Toast.LENGTH_SHORT
            ).show();

            showHome();
        }
    }

    void addBottomNavigation() {

        Space space = new Space(this);

        main.addView(
                space,
                new LinearLayout.LayoutParams(
                        1, 0, 1
                )
        );

        LinearLayout nav = new LinearLayout(this);

        nav.setOrientation(LinearLayout.HORIZONTAL);

        Button home = button("HOME");
        Button upload = button("UPLOAD");
        Button profile = button("PROFILE");

        nav.addView(home,
                new LinearLayout.LayoutParams(0, dp(60), 1));

        nav.addView(upload,
                new LinearLayout.LayoutParams(0, dp(60), 1));

        nav.addView(profile,
                new LinearLayout.LayoutParams(0, dp(60), 1));

        main.addView(nav);

        home.setOnClickListener(v -> showHome());

        upload.setOnClickListener(v -> uploadVideo());

        profile.setOnClickListener(v -> showProfile());
    }

    void addBackButton() {

        Button back = button("BACK");

        main.addView(back);

        back.setOnClickListener(v -> showHome());
    }

    @Override
    public void onBackPressed() {
        showHome();
    }
}
