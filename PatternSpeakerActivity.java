package net.ddns.jrw.patternspeaker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.view.View;
import android.speech.tts.TextToSpeech;

import java.net.FileNameMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.widget.Spinner;
import android.widget.Toast;
import java.io.FileReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import static android.os.Environment.getExternalStorageDirectory;

public class PatternSpeakerActivity extends AppCompatActivity {
    TextToSpeech t1;

    List<ArrayList<String>> Patterns = new ArrayList<ArrayList<String>>();

    private class Pattern {
        ArrayList<String> ManeuverList;
        int NextManeuver;

        public Pattern(ArrayList<String> MList) {
            ManeuverList = MList;
            this.Rewind();
        }

        public void SpeakManeuver() {
            if (0 <= NextManeuver && NextManeuver < ManeuverList.size()) {
                String toSpeak = ManeuverList.get(NextManeuver);
                Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        }

        public void Rewind() {
            NextManeuver = -1;
        }

        public void SpeakNext() {
            if (NextManeuver < ManeuverList.size()) {
                ++NextManeuver;
                this.SpeakManeuver();
            }
        }

        public void SpeakPrevious() {
            if (NextManeuver >= 0) {
                --NextManeuver;
                this.SpeakManeuver();
            }
        }
    }
    Pattern CurrentPattern;

    private void Alert(String Message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle("Error");

        alertDialogBuilder
            .setMessage(Message)
            .setCancelable(false)
            .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    // if this button is clicked, close
                    // current activity
                    finish();
                }
              });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
    }

    private boolean ReadPatternsAndPopulatePatternSpinner() {
        String FileName = getExternalStorageDirectory().getPath() +
                          getApplicationContext().getFilesDir().getPath() +
                          "/Patterns.xml";
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        0);
            }

            List<String> PatternList = new ArrayList<String>();
            ArrayList<String> PatternSequence = null;
            xpp.setInput(new FileReader(FileName));
            for (int eventType = xpp.getEventType();
                 eventType != XmlPullParser.END_DOCUMENT;
                 eventType = xpp.next()
                )
            {
                if(eventType == XmlPullParser.START_TAG) {
                    String TagName = xpp.getName();
                    if (TagName.equals("Sequence")) {
                        PatternList.add(xpp.getAttributeValue(null, "name"));
                        PatternSequence = new ArrayList<String>();
                    } else if (TagName.equals("Maneuver")) {
                        if (PatternSequence != null)
                            PatternSequence.add(xpp.getAttributeValue(null, "name"));
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                    String TagName = xpp.getName();
                    if (TagName.equals("Sequence"))
                        Patterns.add(PatternSequence);
                }
            }

            Spinner PatternSpinner = (Spinner) findViewById(R.id.pattern_spinner);
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, PatternList);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            PatternSpinner.setAdapter(dataAdapter);
            PatternSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                                           long arg3) {
                    CurrentPattern = new Pattern(Patterns.get(arg2));
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    // shouldn't happen
                }
            });

        } catch (Exception e) {
            Alert("File " + FileName + " not found or invalid format");
            return false;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pattern_speaker);

        if (ReadPatternsAndPopulatePatternSpinner()) {

            t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        t1.setLanguage(Locale.US);
                    }
                }
            });

            Button NB = (Button) findViewById(R.id.next_button);
            NB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CurrentPattern.SpeakNext();
                }
            });

            Button RB = (Button) findViewById(R.id.repeat_button);
            RB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CurrentPattern.SpeakManeuver();
                }
            });

            Button FB = (Button) findViewById(R.id.first_button);
            FB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CurrentPattern.Rewind();
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            CurrentPattern.SpeakPrevious();
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            CurrentPattern.SpeakNext();
        }
        return true;
    }
}
