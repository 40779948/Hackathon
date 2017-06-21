package com.google.cloud.doc_samples;

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

import com.google.api.gax.grpc.OperationFuture;
import com.google.cloud.speech.spi.v1.SpeechClient;
import com.google.cloud.speech.v1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.protobuf.ByteString;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;

	class GoogleSpeech {
	
		private SpeechClient speech;
		private RecognitionConfig config;
		private String fileURI;
	
		GoogleSpeech(String fileURI) throws IOException {
			// TODO Auto-generated constructor stub
			this.fileURI = fileURI;
			initSpeechConfig();
		}
	
		private void initSpeechConfig() throws IOException {
			speech = SpeechClient.create();
			config = RecognitionConfig.newBuilder().setEncoding(AudioEncoding.LINEAR16).setLanguageCode("en-US")
					.setSampleRateHertz(16000).build();
		}
	
		public void processRequest() throws InterruptedException, ExecutionException, IOException {
			
			Path path = Paths.get(fileURI);
			byte[] data = Files.readAllBytes(path);
			ByteString audioBytes = ByteString.copyFrom(data);
			
			RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();
	
			// Use non-blocking call for getting file transcription
			OperationFuture<LongRunningRecognizeResponse> response = speech.longRunningRecognizeAsync(config, audio);
			while (!response.isDone()) {
				System.out.println("Waiting for response...");
				Thread.sleep(10000);
			}
	
			List<SpeechRecognitionResult> results = response.get().getResultsList();
	
			for (SpeechRecognitionResult result : results) {
				List<SpeechRecognitionAlternative> alternatives = result.getAlternativesList();
				for (SpeechRecognitionAlternative alternative : alternatives) {
					System.out.printf("Transcription: %s%n", alternative.getTranscript());
				}
			}
			try {
				speech.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
	}
	
	class AudioRecorder {
	
		public void recordAudio() {
			try {
				AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, true);
	
				DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
				if (!AudioSystem.isLineSupported(info)) {
					System.out.println("'Line not supported");
					System.exit(0);
				}
	
				final TargetDataLine targetLine = (TargetDataLine) AudioSystem.getLine(info);
				targetLine.open();
	
				System.out.println("Start Recording...");
				targetLine.start();
	
				Thread thread = new Thread() {
					@Override
					public void run() {
	
						AudioInputStream audioStream = new AudioInputStream(targetLine);
						File audioFile = new File("record.wav");
						try {
							AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, audioFile);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				};
				thread.start();
				Thread.sleep(10000);
				targetLine.stop();
				targetLine.close();
			} catch (LineUnavailableException lue) {
				lue.printStackTrace();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}
	
	public class AsyncVoiceRecorder {
		public static void main(String[] args) {
			AudioRecorder audioRecorder = new AudioRecorder();
			audioRecorder.recordAudio();
			System.out.println("Stopped Recording...");
			try {
				GoogleSpeech googleSpeech = new GoogleSpeech("F:\\Tools\\workspace\\doc-samples\\record.wav");
				googleSpeech.processRequest();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
	}