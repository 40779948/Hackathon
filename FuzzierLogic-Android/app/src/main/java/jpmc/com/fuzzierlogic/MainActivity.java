package jpmc.com.fuzzierlogic;

import android.content.Intent;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.content.ActivityNotFoundException;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton fab;
    TextToSpeech tts;
    TextView textView;
    private final int REQ_CODE_SPEECH_INPUT = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);

        fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askSpeechInput();
            }
        });
    }

    private void textToVoice(String textString){
        final String text = textString;
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    tts.setLanguage(Locale.ENGLISH);
                }
                if(status == TextToSpeech.SUCCESS){
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ){
                        Log.d("textToVoice", text);

                        tts.speak(text,TextToSpeech.QUEUE_ADD, null, "1");
                        tts.playSilentUtterance(1500, TextToSpeech.QUEUE_ADD, null);
                        tts.setSpeechRate(0.8f);
                    } else{
                        tts.speak(text,TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
            }
        });

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ){
            Log.d("textToVoice", text);

            tts.speak(text,TextToSpeech.QUEUE_ADD, null, "1");
            tts.playSilentUtterance(1500, TextToSpeech.QUEUE_ADD, null);
            tts.setSpeechRate(0.8f);
        } else{
            tts.speak(text,TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    // Showing google speech input dialog
    private void askSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Hi speak something");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
        }
    }

    // Receiving speech input
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String userInput = result.get(0);
                    String txtSpoken = "Could you repeat that again? I didn't get that.";
                    textView.setText(result.get(0));
                    if (userInput.contains("balance")) {
                        txtSpoken = "Your balance is $500,000.";
                    }
                    if(userInput.contains("stocks")){
                        txtSpoken = "The current stocks are $24.50 a share";
                    }
                    if(userInput.contains("credit")) {
                        txtSpoken = "Your credit score is 600, do you want to improve it?";
                    }
                    if(userInput.contains("how") || userInput.contains("do")){
                        txtSpoken = "setup an auto payment first of every month, do you want to schedule it?";
                    }
                    if(userInput.contains("yes") || userInput.contains("sure") || userInput.contains("okay")){
                        txtSpoken = "payment scheduled.";
                    }

                    textView.setText(txtSpoken);
                    textToVoice(txtSpoken);
                }
                break;
            }
        }
    }

    //Release resources
    public void onPause(){
        if(tts !=null){
            tts.stop();
            tts.shutdown();
        }
        super.onPause();
    }
}
