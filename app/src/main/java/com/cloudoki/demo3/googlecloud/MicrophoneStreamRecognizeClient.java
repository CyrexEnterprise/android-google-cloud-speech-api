package com.cloudoki.demo3.googlecloud;

import android.os.AsyncTask;

import com.google.auth.oauth2.GoogleCredentials;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.ClientAuthInterceptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Created by rmalta on 06/09/16.
 *
 * more help here: https://cloud.google.com/speech/reference/rpc/google.cloud.speech.v1beta1
 */
public class MicrophoneStreamRecognizeClient {

    private String host = "speech.googleapis.com";
    private Integer port = 443;
    private ManagedChannel channel;
    private StreamingRecognizeClient client;

    private final List<String> OAUTH2_SCOPES = Arrays.asList("https://www.googleapis.com/auth/cloud-platform");

    /**
     *
     * @param authorizationFile
     * @param host
     * @param port
     * @return
     * @throws IOException
     */
    private ManagedChannel createChannel(InputStream authorizationFile, String host, int port) throws IOException {

        GoogleCredentials creds = GoogleCredentials.fromStream(authorizationFile);
        creds = creds.createScoped(OAUTH2_SCOPES);
        return ManagedChannelBuilder.forAddress(host, port)
            .intercept(new ClientAuthInterceptor(creds, Executors.newSingleThreadExecutor()))
            .build();
    }

    /**
     *
     * @param autorizationFile
     * @throws IOException
     */
    public MicrophoneStreamRecognizeClient(InputStream autorizationFile, IResults screen) throws IOException {

        channel = createChannel(autorizationFile, host, port);
        client = new StreamingRecognizeClient(channel, screen);
    }

    /**
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void start() throws IOException, InterruptedException {

        client.recognize();
    }

    /**
     *
     * @throws InterruptedException
     */
    public void stop() throws InterruptedException {

        client.shutdown();
    }

}
