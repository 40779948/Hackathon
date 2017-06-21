package com.google.cloud.doc_samples;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.apache.commons.codec.binary.Base64;

import com.google.api.gax.grpc.ApiStreamObserver;
import com.google.api.gax.grpc.StreamingCallable;
import com.google.cloud.speech.spi.v1.SpeechClient;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;

public class VoiceRecorder {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Start Sounding Test...");
		try {
			//AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,16000,16,2,4,16000,false);
			AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, true);
			
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			if(!AudioSystem.isLineSupported(info)) {
				System.out.println("'Line not supported");
				System.exit(0);
			}
			
			// Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
			final SpeechClient speech = SpeechClient.create();
			
			// Configure request with local raw PCM audio
			RecognitionConfig recConfig = RecognitionConfig.newBuilder().setEncoding(AudioEncoding.LINEAR16)
					.setLanguageCode("en-US").setSampleRateHertz(16000).build();
			StreamingRecognitionConfig config = StreamingRecognitionConfig.newBuilder().setConfig(recConfig).build();

			class ResponseApiStreamingObserver<T> implements ApiStreamObserver<T> {
				private final SettableFuture<List<T>> future = SettableFuture.create();
				private final List<T> messages = new java.util.ArrayList<T>();

				public void onNext(T message) {
					messages.add(message);
				}

				public void onError(Throwable t) {
					future.setException(t);
				}

				public void onCompleted() {
					future.set(messages);
				}

				// Returns the SettableFuture object to get received messages /
				// exceptions.
				public SettableFuture<List<T>> future() {
					return future;
				}
			}

			final ResponseApiStreamingObserver<StreamingRecognizeResponse> responseObserver = new ResponseApiStreamingObserver<StreamingRecognizeResponse>();

			StreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable = speech
					.streamingRecognizeCallable();

			final ApiStreamObserver<StreamingRecognizeRequest> requestObserver = callable.bidiStreamingCall(responseObserver);

			// The first request must **only** contain the audio configuration:
			requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(config).build());

			
			final TargetDataLine targetLine = (TargetDataLine) AudioSystem.getLine(info);
			targetLine.open();
			
			System.out.println("Start Recording...");
			targetLine.start();
			
			Thread thread = new Thread()
			{
				@Override public void run() {
					
					
					AudioInputStream audioStream = new AudioInputStream(targetLine);
					File audioFile = new File("record.wav");
					try {
						AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, audioFile);
					} catch (IOException e) {			
						e.printStackTrace();
					}
					
					
					
					//ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
					//int bytesRead = 0;
					//int numBytesRead;
					//byte[] data = new byte[targetLine.getBufferSize()];
					//targetLine.read(data, 0, data.length);
					
					/*
					while (bytesRead < 10000) { 
					numBytesRead = targetLine.read(data, 0, data.length);
					bytesRead = bytesRead + numBytesRead;
					
					byteArrayOutput.write(data, 0, numBytesRead);
					}
					
					
					byte[] encodedData = Base64.encodeBase64(data);
					*/
					
					Path path = Paths.get("record.wav");
					byte[] data = null;
					try {
						data = Files.readAllBytes(path);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					
					System.out.println("Calling google speech...");

					
					// Subsequent requests must **only** contain the audio data.
					requestObserver
							.onNext(StreamingRecognizeRequest.newBuilder().setAudioContent(ByteString.copyFrom(data)).build());

					// Mark transmission as completed after sending the data.
					requestObserver.onCompleted();

					List<StreamingRecognizeResponse> responses = null;
					try {
						responses = responseObserver.future().get();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					for (StreamingRecognizeResponse response : responses) {
						for (StreamingRecognitionResult result : response.getResultsList()) {
							for (SpeechRecognitionAlternative alternative : result.getAlternativesList()) {
								System.out.println(alternative.getTranscript());
							}
						}
					}
					
					if(responses.isEmpty()){
						System.out.println("Null Response... :(");
					}
					
					try {
						speech.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					
				}	
			};
			
			System.out.println("Stopped Recording...");
			
			thread.start();
			Thread.sleep(5000);
			targetLine.stop();
			targetLine.close();
			
			
			
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		catch(LineUnavailableException lue) {
			lue.printStackTrace();
		}
		catch(InterruptedException ie) {
			ie.printStackTrace();
		} finally {
			
		}
	}
}