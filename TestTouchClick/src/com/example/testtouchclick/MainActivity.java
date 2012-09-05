package com.example.testtouchclick;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.support.v4.app.NavUtils;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button)this.findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				// TODO Auto-generated method stub
				Log.d("Test","This is OnClick");
			}
		});
        button.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event)
			{
				// TODO Auto-generated method stub
				Log.d("Test","This is onTouch,return false");
				return false;
			}
		});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    
}
