package com.viyzo.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends Activity {
    private FirebaseAuth auth;
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) showHome();
        else showLogin();
    }

    private LinearLayout baseRoot() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(40, 60, 40, 40);
        layout.setBackgroundColor(Color.rgb(20, 20, 24));
        return layout;
    }

    private void addTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 40);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private EditText input(String hint) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setHintTextColor(Color.LTGRAY);
        edit.setTextColor(Color.WHITE);
        edit.setTextSize(17);
        edit.setSingleLine(true);
        edit.setPadding(20, 15, 20, 15);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        root.addView(edit, params);
        return edit;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16);
        button.setAllCaps(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 12, 0, 4);
        root.addView(button, params);
        return button;
    }

    private void showLogin() {
        root = baseRoot();
        addTitle("Welcome to VIYZO");

        TextView label = new TextView(this);
        label.setText("Login");
        label.setTextColor(Color.WHITE);
        label.setTextSize(22);
        label.setGravity(Gravity.CENTER);
        root.addView(label);

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

            login.setEnabled(false);
            auth.signInWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> showHome())
                    .addOnFailureListener(error -> {
                        login.setEnabled(true);
                        toast("Login failed: " + error.getMessage());
                    });
        });

        Button signup = button("CREATE NEW ACCOUNT");
        signup.setOnClickListener(v -> showSignup());

        setContentView(root);
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

            if (n.isEmpty()) {
                toast("Enter your name");
                return;
            }
            if (e.isEmpty()) {
                toast("Enter your email");
                return;
            }
            if (p.length() < 6) {
                toast("Password must be at least 6 characters");
                return;
            }

            create.setEnabled(false);
            auth.createUserWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> {
                        toast("Account created");
                        showHome();
                    })
                    .addOnFailureListener(error -> {
                        create.setEnabled(true);
                        toast("Sign up failed: " + error.getMessage());
                    });
        });

        Button back = button("BACK TO LOGIN");
        back.setOnClickListener(v -> showLogin());

        setContentView(root);
    }

    private void showHome() {
        root = baseRoot();
        addTitle("VIYZO");

        TextView message = new TextView(this);
        message.setText("Login successful\n\nViyzo is ready.\n\nNext features will be added one at a time.");
        message.setTextColor(Color.WHITE);
        message.setTextSize(18);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 20, 0, 30);

        root.addView(message, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button logout = button("LOGOUT");
        logout.setOnClickListener(v -> {
            auth.signOut();
            showLogin();
        });

        setContentView(root);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
