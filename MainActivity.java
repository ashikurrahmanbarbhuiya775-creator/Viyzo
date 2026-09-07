package com.viyzo.app;

import android.app.Activity;
import android.os.Bundle;
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
                "You are successfully logged in."
        );
        main.addView(welcome);

        Button profile = makeButton("Profile");
        main.addView(profile);

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProfile();
            }
        });

        Button logout = makeButton("Logout");
        main.addView(logout);

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                auth.signOut();
                showLogin();
            }
        });
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

                String emailText = email.getText().toString().trim();
                String passwordText = password.getText().toString();

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
                ).addOnCompleteListener(MainActivity.this,
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
                });
            }
        });

        Button signup = makeButton("CREATE NEW ACCOUNT");
        main.addView(signup);

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSignup();
            }
        });
    }

    void showSignup() {

        setupScreen();

        main.addView(titleText("VIYZO"));

        TextView heading = normalText("Create Account");
        heading.setTextSize(22);
        heading.setGravity(Gravity.CENTER);
        main.addView(heading);

        final EditText name = makeInput("Your Name");
        main.addView(name);

        final EditText email = makeInput("Email");
        main.addView(email);

        final EditText password = makeInput("Password");
        password.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        main.addView(password);

        Button create = makeButton("SIGN UP");
        main.addView(create);

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String nameText = name.getText().toString().trim();
                String emailText = email.getText().toString().trim();
                String passwordText = password.getText().toString();

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
                ).addOnCompleteListener(MainActivity.this,
                        task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user =
                                auth.getCurrentUser();

                        if (user != null) {

                            Map<String, Object> profile =
                                    new HashMap<>();

                            profile.put("name", nameText);
                            profile.put("email", emailText);
                            profile.put(
                                    "uid",
                                    user.getUid()
                            );

                            db.collection("users")
                                    .document(user.getUid())
                                    .set(profile)
                                    .addOnCompleteListener(
                                            saveTask -> {

                                        Toast.makeText(
                                                MainActivity.this,
                                                "Account created",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        showHome();
                                    });
                        }

                    } else {

                        Toast.makeText(
                                MainActivity.this,
                                "Signup failed: " +
                                task.getException().getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }
        });

        Button back = makeButton("BACK TO LOGIN");
        main.addView(back);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogin();
            }
        });
    }

    void showProfile() {

        setupScreen();

        main.addView(titleText("PROFILE"));

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            showLogin();
            return;
        }

        final TextView profileText = normalText(
                "Loading profile..."
        );

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
                                "\n\nEmail: " + email
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
                            "Email: " + user.getEmail()
                    );
                });

        Button home = makeButton("HOME");
        main.addView(home);

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHome();
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

    @Override
    public void onBackPressed() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            showLogin();
        } else {
            showHome();
        }
    }
}
