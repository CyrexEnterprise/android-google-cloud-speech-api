package com.cloudoki.demo3.googlecloud;

import android.os.AsyncTask;

import io.grpc.ManagedChannel;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by rmalta on 06/09/16.
 */
public class MicrophoneStreamRecognize  {

    private String host = "speech.googleapis.com";
    private Integer port = 443;
    private ManagedChannel channel;
    private StreamingRecognizeClient client;

    public MicrophoneStreamRecognize(InputStream autorizationFile) throws IOException {

        channel = Authorization.createChannel(autorizationFile, host, port);
        client = new StreamingRecognizeClient(channel);
    }

    public void start() throws IOException, InterruptedException {

        client.recognize();
    }

    public void stop() throws InterruptedException {

        client.shutdown();
    }

}
