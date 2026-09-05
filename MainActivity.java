package com.viyzo.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    int white = Color.WHITE;
    int dark = Color.rgb(15, 15, 20);
    int card = Color.rgb(30, 30, 38);

    int likes = 0;
    boolean following = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(dark);

        TextView title = new TextView(this);
        title.setText("VIYZO");
        title.setTextColor(white);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 35, 0, 25);
        main.addView(title);

        ScrollView scroll = new ScrollView(this);

        LinearLayout feed = new LinearLayout(this);
        feed.setOrientation(LinearLayout.VERTICAL);
        feed.setPadding(20, 10, 20, 20);

        TextView video = new TextView(this);
        video.setText("VIYZO VIDEO\n\nShort videos will appear here");
        video.setTextColor(white);
        video.setTextSize(22);
        video.setGravity(Gravity.CENTER);
        video.setBackgroundColor(card);
        video.setPadding(10, 120, 10, 120);
        feed.addView(video);

        Button like = new Button(this);
        like.setText("❤️ Like 0");
        like.setOnClickListener(v -> {
            likes++;
            like.setText("❤️ Like " + likes);
        });
        feed.addView(like);

        Button comment = new Button(this);
        comment.setText("💬 Comment");
        comment.setOnClickListener(v -> {

            EditText input = new EditText(this);
            input.setHint("Write a comment");

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Comment")
                    .setView(input)
                    .setPositiveButton("Post", (dialog, which) -> {
                        Toast.makeText(
                                this,
                                "Comment posted",
                                Toast.LENGTH_SHORT
                        ).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        feed.addView(comment);

        Button share = new Button(this);
        share.setText("↗ Share");
        share.setOnClickListener(v -> {

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(
                    Intent.EXTRA_TEXT,
                    "Watch this video on VIYZO!"
            );

            startActivity(
                    Intent.createChooser(send, "Share with")
            );
        });
        feed.addView(share);

        Button follow = new Button(this);
        follow.setText("Follow");
        follow.setOnClickListener(v -> {

            following = !following;

            if (following) {
                follow.setText("Following");
                Toast.makeText(
                        this,
                        "Following",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                follow.setText("Follow");
                Toast.makeText(
                        this,
                        "Unfollowed",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
        feed.addView(follow);

        Button messenger = new Button(this);
        messenger.setText("💬 Messenger");
        messenger.setOnClickListener(v -> {

            EditText message = new EditText(this);
            message.setHint("Write a message");

            new android.app.AlertDialog.Builder(this)
                    .setTitle("VIYZO Messenger")
                    .setView(message)
                    .setPositiveButton("Send", (dialog, which) -> {
                        Toast.makeText(
                                this,
                                "Message sent",
                                Toast.LENGTH_SHORT
                        ).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        feed.addView(messenger);

        scroll.addView(feed);

        main.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);

        Button home = new Button(this);
        home.setText("HOME");
        home.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Home",
                        Toast.LENGTH_SHORT
                ).show()
        );

        Button upload = new Button(this);
        upload.setText("UPLOAD");
        upload.setOnClickListener(v -> {

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("video/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            startActivityForResult(intent, 100);
        });

        Button profile = new Button(this);
        profile.setText("PROFILE");
        profile.setOnClickListener(v -> {

            Toast.makeText(
                    this,
                    "VIYZO Profile",
                    Toast.LENGTH_SHORT
            ).show();
        });

        bottom.addView(
                home,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        bottom.addView(
                upload,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        bottom.addView(
                profile,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        main.addView(bottom);

        setContentView(main);
    }
}
