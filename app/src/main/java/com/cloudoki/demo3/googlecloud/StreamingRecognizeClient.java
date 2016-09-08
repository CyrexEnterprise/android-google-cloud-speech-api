package com.cloudoki.demo3.googlecloud;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.google.cloud.speech.v1beta1.RecognitionAudio;
import com.google.cloud.speech.v1beta1.RecognitionConfig;
import com.google.cloud.speech.v1beta1.SpeechGrpc;
import com.google.cloud.speech.v1beta1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1beta1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1beta1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

/**
 * Created by rmalta on 06/09/16.
 */
public class StreamingRecognizeClient {

    private int samplingRate;

    private static final Logger logger = Logger.getLogger(StreamingRecognizeClient.class.getName());

    private final ManagedChannel channel;

    private final SpeechGrpc.SpeechStub speechClient;

    private static final int BYTES_PER_BUFFER = 3200; //buffer size in bytes
    private static final int BYTES_PER_SAMPLE = 2; //bytes per sample for LINEAR16

    private static final List<String> OAUTH2_SCOPES =
            Arrays.asList("https://www.googleapis.com/auth/cloud-platform");

    /**
     * Construct client connecting to Cloud Speech server at {@code host:port}.
     */
    public StreamingRecognizeClient(ManagedChannel channel)
            throws IOException {

        this.samplingRate = samplingRate =16000;
        this.channel = channel;

        speechClient = SpeechGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /** Send streaming recognize requests to server. */
    public void recognize() throws InterruptedException, IOException {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<StreamingRecognizeResponse> responseObserver =
                new StreamObserver<StreamingRecognizeResponse>() {
                    @Override
                    public void onNext(StreamingRecognizeResponse response) {
                        logger.info("Received response: " + TextFormat.printToString(response));
                    }

                    @Override
                    public void onError(Throwable error) {
                        logger.log(Level.WARNING, "recognize failed: {0}", error);
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("recognize completed.");
                        finishLatch.countDown();
                    }
                };

        StreamObserver<StreamingRecognizeRequest> requestObserver = speechClient.streamingRecognize(responseObserver);
        try {
            // Build and send a StreamingRecognizeRequest containing the parameters for
            // processing the audio.
            RecognitionConfig config =
                    RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                            .setSampleRate(samplingRate)
                            .build();
            // Sreaming config
            StreamingRecognitionConfig streamingConfig =
                    StreamingRecognitionConfig.newBuilder()
                            .setConfig(config)
                            .setInterimResults(true)
                            .setSingleUtterance(true)
                            .build();
            // First request
            StreamingRecognizeRequest initial =
                    StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingConfig).build();

            requestObserver.onNext(initial);

            // Microphone listener and recorder
            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    samplingRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    BYTES_PER_BUFFER);

            // For LINEAR16 at 16000 Hz sample rate, 3200 bytes corresponds to 100 milliseconds of audio.
            byte[] buffer = new byte[BYTES_PER_BUFFER];
            //int bytesRead;
            //int totalBytes = 0;
            int samplesPerBuffer = BYTES_PER_BUFFER / BYTES_PER_SAMPLE;
            int samplesPerMillis = samplingRate / 1000;
            int recordState;

            recorder.startRecording();

            while ( (recordState = recorder.read(buffer, 0, buffer.length) ) > -1 ) {

                // skip if there is no data
                if( recordState < 0 )
                    continue;

                StreamingRecognizeRequest request =
                        StreamingRecognizeRequest.newBuilder()
                                .setAudioContent(ByteString.copyFrom(buffer, 0, buffer.length))
                                .build();
                requestObserver.onNext(request);
                // To simulate real-time audio, sleep after sending each audio buffer.
                Thread.sleep(samplesPerBuffer / samplesPerMillis);
            }

            Log.d("-- Last record state", String.valueOf(recordState));

            recorder.stop();

        } catch (RuntimeException e) {
            // Cancel RPC.
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests.
        requestObserver.onCompleted();


        // Receiving happens asynchronously.
        finishLatch.await(1, TimeUnit.MINUTES);
    }
}
