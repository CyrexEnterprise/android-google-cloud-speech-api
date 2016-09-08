package com.cloudoki.demo3;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.cloudoki.demo3.googlecloud.MicrophoneStreamRecognize;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

public class MainActivity extends AppCompatActivity {

    private MicrophoneStreamRecognize client;
    private final String AUTHORIZATION_FILE = "ricmalta_d929e2f283d9_service.json";
    private Thread runner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        final Button startButton = (Button) findViewById(R.id.startStreaming);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                runner = new Thread() {

                    public void run(){

                        try {

                            client = new MicrophoneStreamRecognize(getResources().openRawResource(R.raw.ricmalta_d929e2f283d9_service));
                            client.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                };

                runner.start();
            }
        });

        final Button stop = (Button) findViewById(R.id.stopStreaming);
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    runner.join();
                    client.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
