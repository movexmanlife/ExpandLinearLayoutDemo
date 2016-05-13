package com.robot.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity implements View.OnClickListener{

    private ExpandableLinearLayout expandableLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        expandableLayout = (ExpandableLinearLayout) findViewById(R.id.expandable_layout);
        findViewById(R.id.expand_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        expandableLayout.toggle();
    }
}
