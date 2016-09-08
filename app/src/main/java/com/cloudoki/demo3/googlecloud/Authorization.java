package com.cloudoki.demo3.googlecloud;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import com.google.auth.oauth2.GoogleCredentials;
import io.grpc.auth.ClientAuthInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;

/**
 * Created by rmalta on 06/09/16.
 */
public class Authorization {

    private static final List<String> OAUTH2_SCOPES = Arrays.asList("https://www.googleapis.com/auth/cloud-platform");

    public static ManagedChannel createChannel(InputStream authorizationFile, String host, int port) throws IOException {

        GoogleCredentials creds = GoogleCredentials.fromStream(authorizationFile);
        //GoogleCredentials creds = GoogleCredentials.getApplicationDefault();
        creds = creds.createScoped(OAUTH2_SCOPES);
        ManagedChannel channel =
                NettyChannelBuilder.forAddress(host, port)
                        .negotiationType(NegotiationType.TLS)
                        .intercept(new ClientAuthInterceptor(creds, Executors.newSingleThreadExecutor()))
                        .build();

        return channel;
    }
}
