package com.cloudoki.demo3;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cloudoki.demo3.googlecloud.IResults;
import com.cloudoki.demo3.googlecloud.MicrophoneStreamRecognizeClient;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements IResults {

    private IResults Self = this;
    private TextView textView;

    private Thread runner = new Thread() {

        public void run(){

            try {
                Log.d("Main Activity", "Start");
                MicrophoneStreamRecognizeClient client;
                client = new MicrophoneStreamRecognizeClient(getResources().openRawResource(R.raw.client_secret_apps_googleusercontent_com), Self);
                client.start();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.textView);

        // checking permissions
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            int RECORD_AUDIO = 666;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO);
        }

        permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            int ACCESS_NETWORK_STATE = 333;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                    ACCESS_NETWORK_STATE);
        }


        final Button startButton = (Button) findViewById(R.id.startStreaming);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

            runner.start();
            }
        });

        final Button stop = (Button) findViewById(R.id.stopStreaming);
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    Log.d("Main Activity", "Stop");
                    runner.join();
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

    private void setText(String text){
        runOnUiThread(new Runnable() {

            String text;
            public Runnable set(String text) {
                this.text = text;
                return this;
            }

            @Override
            public void run() {

                if(textView != null){
                    textView.setText(text+textView.getText());

                }
            }

        }.set(text));
    }

    @Override
    public void onPartial(String text) {

        setText("Partial: "+text+"\n");
    }

    @Override
    public void onFinal(String text) {

        setText("Final: "+text+"\n");
    }
}
