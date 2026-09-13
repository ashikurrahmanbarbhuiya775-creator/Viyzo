private FirebaseAuth auth;
private LinearLayout root;
private SurfaceView videoSurface;
private MediaPlayer mediaPlayer;
private Uri selectedVideoUri;
private boolean surfaceReady = false;

private static final int PICK_VIDEO = 1001;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    auth = FirebaseAuth.getInstance();

    if (auth.getCurrentUser() != null) {
        showHome();
    } else {
        showLogin();
    }
}

private LinearLayout baseRoot() {
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    layout.setPadding(45, 55, 45, 35);
    layout.setBackgroundColor(Color.rgb(18, 17, 22));
    return layout;
}

private TextView title(String text) {
    TextView t = new TextView(this);
    t.setText(text);
    t.setTextColor(Color.WHITE);
    t.setTextSize(32);
    t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    t.setGravity(Gravity.CENTER);
    t.setPadding(0, 0, 0, 35);
    return t;
}

private TextView message(String text) {
    TextView t = new TextView(this);
    t.setText(text);
    t.setTextColor(Color.LTGRAY);
    t.setTextSize(17);
    t.setGravity(Gravity.CENTER);
    t.setPadding(0, 0, 0, 25);
    return t;
}

private EditText input(String hint, boolean password) {
    EditText e = new EditText(this);
    e.setHint(hint);
    e.setHintTextColor(Color.GRAY);
    e.setTextColor(Color.WHITE);
    e.setTextSize(17);
    e.setSingleLine(true);
    e.setPadding(25, 15, 25, 15);
    if (password) {
        e.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
    } else {
        e.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
    }
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.setMargins(0, 8, 0, 8);
    e.setLayoutParams(lp);
    return e;
}

private Button button(String text) {
    Button b = new Button(this);
    b.setText(text);
    b.setTextSize(16);
    b.setAllCaps(false);
    b.setTextColor(Color.BLACK);
    b.setBackgroundColor(Color.rgb(225, 225, 225));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.setMargins(0, 10, 0, 10);
    b.setLayoutParams(lp);
    return b;
}

private void showLogin() {
    stopVideo();

    root = baseRoot();
    root.addView(title("VIYZO"));
    root.addView(message("Login to continue"));

    EditText email = input("Email", false);
    EditText password = input("Password", true);
    root.addView(email);
    root.addView(password);

    Button login = button("LOGIN");
    Button signup = button("CREATE NEW ACCOUNT");
    root.addView(login);
    root.addView(signup);

    login.setOnClickListener(v -> {
        String e = email.getText().toString().trim();
        String p = password.getText().toString();

        if (e.isEmpty() || p.isEmpty()) {
            toast("Email and password required");
            return;
        }

        login.setEnabled(false);
        auth.signInWithEmailAndPassword(e, p)
                .addOnSuccessListener(result -> {
                    login.setEnabled(true);
                    showHome();
                })
                .addOnFailureListener(err -> {
                    login.setEnabled(true);
                    toast("Login failed: " + err.getMessage());
                });
    });

    signup.setOnClickListener(v -> showSignup());

    setContentView(root);
}

private void showSignup() {
    stopVideo();

    root = baseRoot();
    root.addView(title("VIYZO"));
    root.addView(message("Create your account"));

    EditText name = input("Name", false);
    EditText email = input("Email", false);
    EditText password = input("Password (minimum 6 characters)", true);

    root.addView(name);
    root.addView(email);
    root.addView(password);

    Button create = button("SIGN UP");
    Button back = button("BACK TO LOGIN");
    root.addView(create);
    root.addView(back);

    create.setOnClickListener(v -> {
        String n = name.getText().toString().trim();
        String e = email.getText().toString().trim();
        String p = password.getText().toString();

        if (n.isEmpty() || e.isEmpty() || p.isEmpty()) {
            toast("Please fill all fields");
            return;
        }

        if (p.length() < 6) {
            toast("Password must be at least 6 characters");
            return;
        }

        create.setEnabled(false);
        auth.createUserWithEmailAndPassword(e, p)
                .addOnSuccessListener(result -> {
                    create.setEnabled(true);
                    showHome();
                })
                .addOnFailureListener(err -> {
                    create.setEnabled(true);
                    toast("Signup failed: " + err.getMessage());
                });
    });

    back.setOnClickListener(v -> showLogin());

    setContentView(root);
}

private void showHome() {
    stopVideo();

    root = baseRoot();
    root.addView(title("VIYZO"));
    root.addView(message("Login successful\n\nSelect a video and open the video screen."));

    Button select = button("UPLOAD / SELECT VIDEO");
    Button view = button("VIEW VIDEO");
    Button logout = button("LOGOUT");

    root.addView(select);
    root.addView(view);
    root.addView(logout);

    select.setOnClickListener(v -> selectVideo());

    view.setOnClickListener(v -> {
        if (selectedVideoUri == null) {
            toast("First select a video");
            return;
        }
        showVideoScreen();
    });

    logout.setOnClickListener(v -> {
        stopVideo();
        auth.signOut();
        selectedVideoUri = null;
        showLogin();
    });

    setContentView(root);
}

private void selectVideo() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("video/*");
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

    try {
        startActivityForResult(intent, PICK_VIDEO);
    } catch (Exception e) {
        Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
        fallback.setType("video/*");
        fallback.addCategory(Intent.CATEGORY_OPENABLE);
        fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(fallback, PICK_VIDEO);
    }
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == PICK_VIDEO && resultCode == RESULT_OK && data != null
            && data.getData() != null) {

        selectedVideoUri = data.getData();

        try {
            getContentResolver().takePersistableUriPermission(
                    selectedVideoUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {
        }

        toast("Video selected successfully");
        showVideoScreen();
    }
}

private void showVideoScreen() {
    stopVideo();

    root = baseRoot();
    root.setPadding(0, 25, 0, 25);

    TextView top = title("VIDEO");
    top.setPadding(0, 10, 0, 15);
    root.addView(top);

    if (selectedVideoUri == null) {
        root.addView(message("No video selected"));
        Button back = button("BACK TO HOME");
        root.addView(back);
        back.setOnClickListener(v -> showHome());
        setContentView(root);
        return;
    }

    videoSurface = new SurfaceView(this);
    videoSurface.setBackgroundColor(Color.BLACK);

    // Keep the video surface above the dark activity background.
    videoSurface.setZOrderOnTop(true);

    LinearLayout.LayoutParams videoLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0);
    videoLp.weight = 1;
    videoLp.setMargins(0, 5, 0, 10);
    videoSurface.setLayoutParams(videoLp);

    root.addView(videoSurface);

    Button back = button("BACK TO HOME");
    root.addView(back);

    videoSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            surfaceReady = true;
            startVideo(holder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                try {
                    mediaPlayer.setDisplay(holder);
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            surfaceReady = false;
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.setDisplay(null);
                } catch (Exception ignored) {
                }
            }
        }
    });

    back.setOnClickListener(v -> showHome());

    setContentView(root);
}

private void startVideo(SurfaceHolder holder) {
    if (!surfaceReady || selectedVideoUri == null) return;

    stopMediaPlayerOnly();

    mediaPlayer = new MediaPlayer();

    try {
        mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);
        mediaPlayer.setDisplay(holder);
        mediaPlayer.setDataSource(this, selectedVideoUri);
        mediaPlayer.setLooping(true);

        mediaPlayer.setOnPreparedListener(mp -> {
            try {
                mp.setDisplay(holder);
                mp.setLooping(true);
                mp.start();
            } catch (Exception e) {
                toast("Video start error");
            }
        });

        mediaPlayer.setOnVideoSizeChangedListener((mp, width, height) -> {
            // The SurfaceView keeps the video visible while Android scales it.
            try {
                mp.setDisplay(holder);
            } catch (Exception ignored) {
            }
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            toast("This video format cannot be played");
            return true;
        });

        mediaPlayer.prepareAsync();

    } catch (Exception e) {
        toast("Video error: " + e.getMessage());
        stopMediaPlayerOnly();
    }
}

private void stopMediaPlayerOnly() {
    if (mediaPlayer != null) {
        try {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
        } catch (Exception ignored) {
        }
        try {
            mediaPlayer.reset();
        } catch (Exception ignored) {
        }
        try {
            mediaPlayer.release();
        } catch (Exception ignored) {
        }
        mediaPlayer = null;
    }
}

private void stopVideo() {
    surfaceReady = false;
    stopMediaPlayerOnly();
    videoSurface = null;
}

@Override
protected void onPause() {
    super.onPause();
    if (mediaPlayer != null) {
        try {
            if (mediaPlayer.isPlaying()) mediaPlayer.pause();
        } catch (Exception ignored) {
        }
    }
}

@Override
protected void onDestroy() {
    stopVideo();
    super.onDestroy();
}

@Override
public void onBackPressed() {
    if (videoSurface != null) {
        showHome();
    } else {
        super.onBackPressed();
    }
}

private void toast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
}
