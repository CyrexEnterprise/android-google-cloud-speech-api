package com.cloudoki.demo3.googlecloud;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import com.google.auth.oauth2.GoogleCredentials;

import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.ClientAuthInterceptor;
import io.grpc.ManagedChannel;

/**
 * Created by rmalta on 06/09/16.
 */
public class Authorization {

    private static final List<String> OAUTH2_SCOPES = Arrays.asList("https://www.googleapis.com/auth/cloud-platform");

    public static ManagedChannel createChannel(InputStream authorizationFile, String host, int port) throws IOException {

        GoogleCredentials creds = GoogleCredentials.fromStream(authorizationFile);
        creds = creds.createScoped(OAUTH2_SCOPES);
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(host, port)
                        .intercept(new ClientAuthInterceptor(creds, Executors.newSingleThreadExecutor()))
                        .build();
        return channel;
    }
}
