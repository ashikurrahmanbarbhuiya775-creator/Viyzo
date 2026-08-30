package com.viyzo.app;
import android.app.Activity; import android.os.Bundle; import android.graphics.Color; import android.view.Gravity; import android.widget.TextView;
public class MainActivity extends Activity { public void onCreate(Bundle b){ super.onCreate(b); TextView t=new TextView(this); t.setText("Viyzo"); t.setTextSize(32); t.setTextColor(Color.WHITE); t.setGravity(Gravity.CENTER); t.setBackgroundColor(Color.rgb(20,20,24)); setContentView(t); } }
