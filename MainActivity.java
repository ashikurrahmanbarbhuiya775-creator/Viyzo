package com.viyzo.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private int white = Color.WHITE;
    private int dark = Color.rgb(15, 15, 20);
    private int card = Color.rgb(30, 30, 38);

    @Override
    public void onCreate(Bundle savedInstanceState) {
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

        TextView actions = new TextView(this);
        actions.setText("Like     Comment     Share");
        actions.setTextColor(white);
        actions.setTextSize(17);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(10, 20, 10, 25);
        feed.addView(actions);

        scroll.addView(feed);

        main.addView(scroll,
                new LinearLayout.LayoutParams(
                        -1, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);

        Button home = new Button(this);
        home.setText("Home");
        home.setOnClickListener(v ->
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show());

        Button upload = new Button(this);
        upload.setText("Upload");
        upload.setOnClickListener(v ->
                Toast.makeText(this, "Upload", Toast.LENGTH_SHORT).show());

        Button profile = new Button(this);
        profile.setText("Profile");
        profile.setOnClickListener(v ->
                Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show());

        bottom.addView(home,
                new LinearLayout.LayoutParams(0, 70, 1));

        bottom.addView(upload,
                new LinearLayout.LayoutParams(0, 70, 1));

        bottom.addView(profile,
                new LinearLayout.LayoutParams(0, 70, 1));

        main.addView(bottom);

        setContentView(main);
    }
}
