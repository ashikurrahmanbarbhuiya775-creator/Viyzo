package com.viyzo.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    LinearLayout main;
    FirebaseAuth auth;
    FirebaseFirestore db;

    TextView videoStatus;
    Uri selectedVideo;

    int padding = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        showHome();
    }

    int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }

    TextView titleText(String value) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(28);
        t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10), dp(20), dp(10), dp(20));
        return t;
    }

    TextView normalText(String value) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(17);
        t.setTextColor(Color.WHITE);
        t.setPadding(dp(10), dp(10), dp(10), dp(10));
        return t;
    }

    Button makeButton(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(16);
        return b;
    }

    EditText makeInput(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.LTGRAY);
        e.setTextSize(16);
        e.setPadding(dp(15), dp(12), dp(15), dp(12));
        return e;
    }

    void setupScreen() {
        main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(padding), dp(padding), dp(padding), dp(padding));
        main.setBackgroundColor(Color.rgb(20, 20, 24));

        setContentView(main);
    }

    void showHome() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            showLogin();
            return;
        }

        setupScreen();

        main.addView(titleText("VIYZO"));

        TextView welcome = normalText(
                "Welcome to VIYZO\n\n" +
                "Short videos for everyone."
        );
        main.addView(welcome);

        Button upload = makeButton("🎥 UPLOAD VIDEO");
        main.addView(upload);

        videoStatus = normalText("No video selected.");
        main.addView(videoStatus);

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openVideoPicker();
            }
        });

        Button like = makeButton("❤️ LIKE  0");
        main.addView(like);

        like.setOnClickListener(new View.OnClickListener() {
            int likes = 0;

            @Override
            public void onClick(View v) {
                likes++;
                like.setText("❤️ LIKE  " + likes);
            }
        });

        Button comment = makeButton("💬 COMMENT");
        main.addView(comment);

        comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCommentBox();
            }
        });

        Button share = makeButton("🔄 SHARE");
        main.addView(share);

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        "Watch this video on VIYZO!"
                );

                startActivity(
                        Intent.createChooser(sendIntent, "Share with")
                );
            }
        });

        Button delete = makeButton("🗑️ DELETE VIDEO");
        main.addView(delete);

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (selectedVideo != null) {
                    selectedVideo = null;
                    videoStatus.setText("Video deleted.");
                    Toast.makeText(
                            MainActivity.this,
                            "Video deleted",
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(
                            MainActivity.this,
                            "पहले वीडियो चुनिए",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });

        Button profile = makeButton("👤 PROFILE");
        main.addView(profile);

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProfile();
            }
        });

        Button search = makeButton("🔍 SEARCH");
        main.addView(search);

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearch();
            }
        });

        Button followers = makeButton("👥 FOLLOWERS / FOLLOWING");
        main.addView(followers);

        followers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFollowers();
            }
        });

        Button message = makeButton("✉️ MESSAGE");
        main.addView(message);

        message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMessage();
            }
        });

        Button report = makeButton("🚫 REPORT / DISPUTE");
        main.addView(report);

        report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReport();
            }
        });

        Button logout = makeButton("LOGOUT");
        main.addView(logout);

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                auth.signOut();
                showLogin();
            }
        });
    }

    void openVideoPicker() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(intent, 100);
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

        if (requestCode == 100 &&
                resultCode == RESULT_OK &&
                data != null) {

            selectedVideo = data.getData();

            if (selectedVideo != null) {

                videoStatus.setText(
                        "Video selected successfully.\n\n" +
                        "Ready to upload."
                );

                Toast.makeText(
                        this,
                        "Video selected",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    void showCommentBox() {

        setupScreen();

        main.addView(titleText("COMMENTS"));

        final EditText comment =
                makeInput("Write a comment...");

        main.addView(comment);

        Button post = makeButton("POST COMMENT");
        main.addView(post);

        post.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String text =
                        comment.getText().toString().trim();

                if (text.length() == 0) {
                    Toast.makeText(
                            MainActivity.this,
                            "Comment लिखिए",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                Toast.makeText(
                        MainActivity.this,
                        "Comment posted",
                        Toast.LENGTH_SHORT
                ).show();

                showHome();
            }
        });

        Button back = makeButton("BACK");
        main.addView(back);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHome();
            }
        });
    }

    void showSearch() {

        setupScreen();

        main.addView(titleText("SEARCH"));

        EditText search =
                makeInput("Search users or videos");

        main.addView(search);

        Button searchButton =
                makeButton("SEARCH");

        main.addView(searchButton);

        searchButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Toast.makeText(
                                MainActivity.this,
                                "Search system ready",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        Button back = makeButton("BACK HOME");
        main.addView(back);

        back.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showHome();
                    }
                }
        );
    }

    void showFollowers() {

        setupScreen();

        main.addView(titleText("FOLLOWERS"));

        main.addView(
                normalText(
                        "Followers: 0\n\n" +
                        "Following: 0"
                )
        );

        Button follow = makeButton("➕ FOLLOW");
        main.addView(follow);

        follow.setOnClickListener(
                new View.OnClickListener() {
                    boolean following = false;

                    @Override
                    public void onClick(View v) {

                        following = !following;

                        if (following) {
                            follow.setText("✓ FOLLOWING");
                        } else {
                            follow.setText("➕ FOLLOW");
                        }
                    }
                }
        );

        Button back = makeButton("BACK HOME");
        main.addView(back);

        back.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showHome();
                    }
                }
        );
    }

    void showMessage() {

        setupScreen();

        main.addView(titleText("MESSAGE"));

        main.addView(
                normalText(
                        "Messaging system\n\n" +
                        "Chat feature will be connected to Firebase."
                )
        );

        EditText message =
                makeInput("Write message...");

        main.addView(message);

        Button send = makeButton("SEND");
        main.addView(send);

        send.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Toast.makeText(
                                MainActivity.this,
                                "Message sent",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        Button back = makeButton("BACK HOME");
        main.addView(back);

        back.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showHome();
                    }
                }
        );
    }

    void showReport() {

        setupScreen();

        main.addView(titleText("REPORT / DISPUTE"));

        final EditText report =
                makeInput("Why are you reporting this?");

        main.addView(report);

        Button submit =
                makeButton("SUBMIT REPORT");

        main.addView(submit);

        submit.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Toast.makeText(
                                MainActivity.this,
                                "Report submitted",
                                Toast.LENGTH_SHORT
                        ).show();

                        showHome();
                    }
                }
        );

        Button back = makeButton("BACK HOME");
        main.addView(back);

        back.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showHome();
                    }
                }
        );
    }

    void showLogin() {

        setupScreen();

        main.addView(titleText("VIYZO"));

        TextView heading = normalText("Login");
        heading.setTextSize(22);
        heading.setGravity(Gravity.CENTER);
        main.addView(heading);

        final EditText email = makeInput("Email");
        main.addView(email);

        final EditText password = makeInput("Password");

        password.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        main.addView(password);

        Button login = makeButton("LOGIN");
        main.addView(login);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String emailText =
                        email.getText().toString().trim();

                String passwordText =
                        password.getText().toString();

                if (emailText.length() == 0) {
                    Toast.makeText(
                            MainActivity.this,
                            "Email डालिए",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                if (passwordText.length() < 6) {
                    Toast.makeText(
                            MainActivity.this,
                            "Password कम से कम 6 अक्षर का होना चाहिए",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                auth.signInWithEmailAndPassword(
                        emailText,
                        passwordText
                ).addOnCompleteListener(
                        MainActivity.this,
                        task -> {

                            if (task.isSuccessful()) {

                                Toast.makeText(
                                        MainActivity.this,
                                        "Login successful",
                                        Toast.LENGTH_SHORT
                                ).show();

                                showHome();

                            } else {

                                Toast.makeText(
                                        MainActivity.this,
                                        "Login failed: " +
                                        task.getException().getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
            }
        });

        Button signup =
                makeButton("CREATE NEW ACCOUNT");

        main.addView(signup);

        signup.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSignup();
                    }
                }
        );
    }

    void showSignup() {

        setupScreen();

        main.addView(titleText("VIYZO"));

        TextView heading =
                normalText("Create Account");

        heading.setTextSize(22);
        heading.setGravity(Gravity.CENTER);

        main.addView(heading);

        final EditText name =
                makeInput("Your Name");

        main.addView(name);

        final EditText email =
                makeInput("Email");

        main.addView(email);

        final EditText password =
                makeInput("Password");

        password.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        main.addView(password);

        Button create =
                makeButton("SIGN UP");

        main.addView(create);

        create.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        String nameText =
                                name.getText().toString().trim();

                        String emailText =
                                email.getText().toString().trim();

                        String passwordText =
                                password.getText().toString();

                        if (nameText.length() == 0) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Name डालिए",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        if (emailText.length() == 0) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Email डालिए",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        if (passwordText.length() < 6) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Password कम से कम 6 अक्षर का होना चाहिए",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        auth.createUserWithEmailAndPassword(
                                emailText,
                                passwordText
                        ).addOnCompleteListener(
                                MainActivity.this,
                                task -> {

                                    if (task.isSuccessful()) {

                                        FirebaseUser user =
                                                auth.getCurrentUser();

                                        if (user != null) {

                                            Map<String, Object> profile =
                                                    new HashMap<>();

                                            profile.put(
                                                    "name",
                                                    nameText
                                            );

                                            profile.put(
                                                    "email",
                                                    emailText
                                            );

                                            profile.put(
                                                    "uid",
                                                    user.getUid()
                                            );

                                            db.collection("users")
                                                    .document(
                                                            user.getUid()
                                                    )
                                                    .set(profile)
                                                    .addOnCompleteListener(
                                                            saveTask -> {

                                                                Toast.makeText(
                                                                        MainActivity.this,
                                                                        "Account created",
                                                                        Toast.LENGTH_SHORT
                                                                ).show();

                                                                showHome();
                                                            }
                                                    );
                                        }

                                    } else {

                                        Toast.makeText(
                                                MainActivity.this,
                                                "Signup failed: " +
                                                task.getException()
                                                        .getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }
                        );
                    }
                }
        );

        Button back =
                makeButton("BACK TO LOGIN");

        main.addView(back);

        back.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showLogin();
                    }
                }
        );
    }

    void showProfile() {

        setupScreen();

        main.addView(titleText("PROFILE"));

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            showLogin();
            return;
        }

        final TextView profileText =
                normalText("Loading profile...");

        main.addView(profileText);

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String name =
                                document.getString("name");

                        String email =
                                document.getString("email");

                        profileText.setText(
                                "Name: " + name +
                                "\n\nEmail: " + email +
                                "\n\nFollowers: 0" +
                                "\nFollowing: 0"
                        );

                    } else {

                        profileText.setText(
                                "Email: " +
                                user.getEmail()
                        );
                    }
                })
                .addOnFailureListener(error -> {

                    profileText.setText(
                            "Email: " +
                            user.getEmail()
                    );
                });

        Button home =
                makeButton("HOME");

        main.addView(home);

        home.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showHome();
                    }
                }
        );

        Button logout =
                makeButton("LOGOUT");

        main.addView(logout);

        logout.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        auth.signOut();
                        showLogin();
                    }
                }
        );
    }

    @Override
    public void onBackPressed() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            showLogin();
        } else {
            showHome();
        }
    }
}
