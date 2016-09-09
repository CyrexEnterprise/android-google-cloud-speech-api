package com.cloudoki.demo3.googlecloud;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import com.google.cloud.speech.v1beta1.RecognitionAudio;
import com.google.cloud.speech.v1beta1.RecognitionConfig;
import com.google.cloud.speech.v1beta1.SpeechGrpc;
import com.google.cloud.speech.v1beta1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1beta1.StreamingRecognitionResult;
import com.google.cloud.speech.v1beta1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1beta1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.google.longrunning.ListOperationsResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

/**
 * Created by rmalta on 06/09/16.
 */
public class StreamingRecognizeClient {

    private int RECORDER_SAMPLERATE;
    private int RECORDER_CHANNELS;
    private int RECORDER_AUDIO_ENCODING;

    private int bufferSize = 0;

    private static final Logger logger = Logger.getLogger(StreamingRecognizeClient.class.getName());

    private final ManagedChannel channel;

    private final SpeechGrpc.SpeechStub speechClient;

    private static final int BYTES_PER_BUFFER = 3200; //buffer size in bytes
    private static final int BYTES_PER_SAMPLE = 2; //bytes per sample for LINEAR16

    private AudioRecord recorder = null;

    private Thread recordingThread = null;


    /**
     * Construct client connecting to Cloud Speech server at {@code host:port}.
     */
    public StreamingRecognizeClient(ManagedChannel channel)
            throws IOException {

        this.RECORDER_SAMPLERATE = 16000;
        this.RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
        this.RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

        this.bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        this.channel = channel;

        speechClient = SpeechGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {

        if( recorder != null ){
            recorder.stop();
            recorder.release();
            recorder = null;
        }

        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /** Send streaming recognize requests to server. */
    public void recognize() throws InterruptedException, IOException {

        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<StreamingRecognizeResponse> responseObserver =

            new StreamObserver<StreamingRecognizeResponse>() {
                @Override
                public void onNext(StreamingRecognizeResponse response) {

                    int numOfResults = response.getResultsCount();

                    if( numOfResults > 0 ){

                        //Log.d("Results #", String.valueOf(numOfResults));

                        for (int i=0;i<numOfResults;i++){

                            StreamingRecognitionResult result = response.getResultsList().get(i);

                            if( result.getIsFinal() )
                                Log.d("\tFinal",  result.getAlternatives(0).getTranscript());
                            else
                                Log.d(" Partial",  result.getAlternatives(0).getTranscript());

                        }
                    }
                }

                @Override
                public void onError(Throwable error) {
                    //logger.log(Level.WARNING, "recognize failed: {0}", error);
                    finishLatch.countDown();
                    if( recorder != null ){
                        recorder.stop();
                        recorder.release();
                        recorder = null;
                    }

                    try {
                        recognize();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCompleted() {
                    //logger.info("recognize completed.");
                    finishLatch.countDown();
                    if( recorder != null ){
                        recorder.stop();
                        recorder.release();
                        recorder = null;
                    }
                    try {
                        recognize();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

        final StreamObserver<StreamingRecognizeRequest> requestObserver = speechClient.streamingRecognize(responseObserver);
        try {
            // Build and send a StreamingRecognizeRequest containing the parameters for
            // processing the audio.
            RecognitionConfig config =
                    RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                            .setSampleRate(this.RECORDER_SAMPLERATE)
                            .build();

            // Sreaming config
            StreamingRecognitionConfig streamingConfig =
                    StreamingRecognitionConfig.newBuilder()
                            .setConfig(config)
                            .setInterimResults(true)
                            .setSingleUtterance(false)
                            .build();
            // First request
            StreamingRecognizeRequest initial =
                    StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingConfig).build();

            requestObserver.onNext(initial);

            // Microphone listener and recorder
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    this.RECORDER_SAMPLERATE,
                    this.RECORDER_CHANNELS,
                    this.RECORDER_AUDIO_ENCODING,
                    BYTES_PER_BUFFER);

            recorder.startRecording();

            byte[] buffer = new byte[bufferSize];
            int recordState;
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
                //Thread.sleep(samplesPerBuffer / samplesPerMillis);
            }

            Log.d("-- Last record state", String.valueOf(recordState));

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
