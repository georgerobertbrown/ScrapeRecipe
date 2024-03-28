package com.gncbrown.scraperecipe;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity
        extends AppCompatActivity //Activity
        implements TextToSpeech.OnInitListener {

    private static final String TAG = MainActivity.class.getName();

    private static Context context;

    public static SharedPreferences sharedPreferences;

    private ProgressBar progressBar;
    private LinearLayout progressLayout;

    private static String resultsString;
    private TextView urlResults;
    private TextView ingredientsResult;
    private TextView recipeUrl;
    private Button buttonGet;
    private RetrieveUrlTask retrievalUrlTask;

    private LinearLayout ingredientsLayout;

    private static TextToSpeech tts;
    private static boolean ttsSucceeded = false;
    public static AudioManager audioManager;
    public static int audioMax;
    public static int savedVolume;
    public static int savedMusicVolume;
    public static int savedAlarmVolume;
    public static int savedNotificationVolume;
    public static int savedDtmfVolume;
    public static int savedSystemVolume;

    public static String currentUtterance;

    public static final int DEFAULT_VOLUME = 5;
    public static final int DEFAULT_DELAY = 1000;
    public static final int DELAY_MAX = 3000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        sharedPreferences = getSharedPreferences("USER", MODE_PRIVATE);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressLayout = (LinearLayout) findViewById(R.id.layoutProgress);

        tts = new TextToSpeech(this, this);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                Log.d(TAG, "speak.onStart: " + s);
                currentUtterance = s;
            }

            @Override
            public void onDone(String s) {
                Log.d(TAG, "speak.onDone: " + s);
            }

            @Override
            public void onError(String s) {
                Toast.makeText(context.getApplicationContext(),
                        String.format(Locale.getDefault(), "Could not speak: %s. Error: %s", currentUtterance, s),
                        Toast.LENGTH_SHORT).show();

                Log.d(TAG, "speak.onError: " + s);
            }
        });

        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        audioMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        ingredientsLayout = (LinearLayout) findViewById(R.id.ingredientsLayout);

        urlResults = (TextView) findViewById(R.id.editTextResults);
        urlResults.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("URL Results", resultsString);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Saved " + resultsString.length() + " chars to clipboard.",
                        Toast.LENGTH_LONG).show();
                return true;
            }
        });
        urlResults.setText("");

        ingredientsResult = (TextView) findViewById(R.id.editTextIngredients);
        ingredientsResult.setText("");
        recipeUrl = (TextView) findViewById(R.id.textRecipeUrl);
        recipeUrl.setText("https://www.aspicyperspective.com/old-fashioned-tomato-jam-recipe/");
        // "https://sallysbakingaddiction.com/whole-wheat-bread/");
        //"https://thebigmansworld.com/almond-flour-biscotti/#wprm-recipe-container-39177");
        //"https://www.peta.org/recipes/auntie-bonnie-s-chickpea-salad/?utm_source=PETA::Google&utm_medium=Ad&utm_campaign=0422::veg::PETA::Google::SEA-Vegan-Grant::::searchad&gad_source=1&gclid=CjwKCAiAibeuBhAAEiwAiXBoJMxp2f6hu4WYiSOfGxEFFilDfchADUOA4N0LTuwYLJAXnQW2C9EPNhoCZPkQAvD_BwE");
        //"https://www.serenabakessimplyfromscratch.com/2011/05/homemade-egg-noodles.html"); //"https://www.google.com/");

        buttonGet = (Button) findViewById(R.id.buttonGet);
        buttonGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "buttonGet.onClick");
                String urlString = recipeUrl.getText().toString();
                if (!urlString.isEmpty()) {
                    if (!isValidURL(urlString)) {
                        String message = "URL '" + urlString + "' is invalid.";
                        ingredientsResult.setText("");
                        urlResults.setText(message);
                        alert("URL error", message);
                    } else {
                        String buttonName = buttonGet.getText().toString();
                        if (buttonName.equals("Get")) {
                            showProgress(true); //progressBar.setVisibility(View.VISIBLE);

                            ingredientsLayout.removeAllViews();
                            TextView msg = new TextView(context);
                            msg.setText("Retrieving...\n");
                            msg.setTextSize(18);
                            msg.setBackgroundColor(Color.BLACK);
                            msg.setTextColor(Color.WHITE);
                            msg.setLayoutParams(new LinearLayout.LayoutParams(
                                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT)));
                            msg.setGravity(Gravity.START | Gravity.TOP);
                            msg.setEms(10);
                            msg.setHorizontallyScrolling(true);
                            msg.setMinLines(5);
                            msg.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                            ingredientsLayout.addView(msg);

                            urlResults.setText("Retrieving...");
                            startUrlRetrieval(urlString);
                            buttonGet.setText("Cancel");
                        } else {
                            if (retrievalUrlTask != null)
                                retrievalUrlTask.cancel(true);
                            else
                                alert("Retrieval error", "Retrieval task is apparently not running.");
                            urlResults.setText("Cancelled");
                            ingredientsResult.setText("");
                            buttonGet.setText("Get");
                            showProgress(false); //progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                } else {
                    alert("URL error", "Please enter a recipe URL.");
                }
            }
        });

        Button buttonClear = (Button) findViewById(R.id.buttonClear);
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "buttonClear.onClick");
                recipeUrl.setText("");
                ingredientsLayout.removeAllViews();
                urlResults.setText("");
                showProgress(false);
            }
        });

        Button buttonAlexa = (Button) findViewById(R.id.buttonAlexa);
        buttonAlexa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "buttonAlexa.onClick");
                try {
                    Intent alexaIntent;
                    alexaIntent = new Intent("android.intent.action.ASSIST");
                    alexaIntent.setPackage("com.amazon.dee.app");
                    if (alexaIntent != null) {
                        alexaIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                        context.startActivity(alexaIntent);
                    } else {
                        Toast.makeText(context, "Alexa app not installed?",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch Alexa: " + e.getMessage());
                    Toast.makeText(context, "Failed to launch Alexa: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        Button buttonSpeak = (Button) findViewById(R.id.buttonSpeak);
        buttonSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int speakVolume = Utils.retrieveVolumeFromPreference();
                Log.d(TAG, "buttonSpeak.onClick; savedVolume=" + savedVolume
                        + ", audioMax=" + audioMax
                        + ", speakVolume=" + speakVolume);
                showProgress(true);

                int count = ingredientsLayout.getChildCount();
                String ingredients = "";
                for (int i=0; i<count; i++) {
                    View ingredientView = ingredientsLayout.getChildAt(i);
                    try {
                        CheckBox cb = (CheckBox)ingredientView;
                        String ingredient = cb.getText().toString();
                        Log.d(TAG, "found ingredient=" + ingredient);

                        if (cb.isChecked())
                            ingredients += "ALEXA!!! ADD " + ingredient + "\n";
                    } catch (Exception e) {
                        break;
                    }
                }
                // Restore volume
                if (ingredients.isEmpty())
                    speak("No items checked!");
                else
                    speak(ingredients);

                showProgress(false);
            }
        });

        showProgress(false); //progressBar.setVisibility(View.INVISIBLE);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String launchType = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && launchType != null) {
            Log.d(TAG, "LAUNCH action=SEND, launchType=" + launchType);
            if ("text/html".equals(launchType) || "text/plain".equals(launchType)) {
                handleSendText(intent); // Handle text being sent
            }
        } else {
            // Handle other intents, such as being started from the home screen
            Log.d(TAG, "LAUNCH from MAIN");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings) {
            Intent myIntent = new Intent(context, SettingsActivity.class);
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(myIntent);
            return (true);
        } else if (id == R.id.about) {
            Toast.makeText(this, "About...", Toast.LENGTH_LONG).show();
            return (true);
        } else if (id == R.id.exit) {
            finish();
            return(true);
        }
        return(super.onOptionsItemSelected(item));
    }


    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            Log.d(TAG, "handleSendText; sharedText=" + sharedText);
            try {
                recipeUrl.setText(sharedText);
            } catch (Exception e) {
            }
            urlResults.setText("Retrieving...");
            startUrlRetrieval(sharedText);
        }
    }

    private void startUrlRetrieval(String urlString) {
        Log.d(TAG, "startUrlRetrieval; urlString=" + urlString);
        retrievalUrlTask = new RetrieveUrlTask();
        retrievalUrlTask.execute(urlString);
    }

    @Override
    public void onInit(int status) {
        Log.d(TAG, "TextToSpeech.onInit; status=" + status);
        ttsSucceeded = status == TextToSpeech.SUCCESS;
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(getApplicationContext(), "Language not supported", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "TextToSpeech initialization failed with status "
                    + status, Toast.LENGTH_SHORT).show();
        }
    }

    public static void saveVolumes() {
        savedMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        savedAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        savedNotificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        savedDtmfVolume = audioManager.getStreamVolume(AudioManager.STREAM_DTMF);
        savedSystemVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
        savedVolume = savedMusicVolume;
        Log.d(TAG, "saveVolumes; music=" + savedMusicVolume
                + ", alarm=" + savedAlarmVolume
                + ", notification=" + savedNotificationVolume
                + ", dtmf=" + savedDtmfVolume
                + ", system=" + savedSystemVolume
            );
    }

    public static void restoreVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedMusicVolume, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, savedAlarmVolume, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, savedNotificationVolume, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_DTMF, savedDtmfVolume, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, savedSystemVolume, 0);

        savedVolume = savedMusicVolume;
        Log.d(TAG, "restoreVolume; music=" + savedMusicVolume
                + ", alarm=" + savedAlarmVolume
                + ", notification=" + savedNotificationVolume
                + ", dtmf=" + savedDtmfVolume
                + ", system=" + savedSystemVolume
        );
    }

    public static void setVolume(int volume) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volume, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_DTMF, volume, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, volume, 0);

        Log.d(TAG, "setVolume; volume=" + volume);
    }

    public static void speak(String what) {
        Log.d(TAG, "speak; what=" + what);
        if (tts == null || !ttsSucceeded) {
            String message = String.format(Locale.getDefault(), "Could not speak: %s, speech not initialized.", what);
            Log.d(TAG, message);
            Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            return;
        }

        int voiceVolume = Utils.retrieveVolumeFromPreference();
        int delay = Utils.retrieveDelayFromPreference();
        saveVolumes();
        setVolume(voiceVolume);

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, what); //SPEECH_ID);
        float fraction = (float) voiceVolume / (float) MainActivity.audioMax;
        String volumePercentage = String.format("%.1f", fraction);
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, fraction);
        Log.d(TAG, "speak[" + voiceVolume
                + "/" + volumePercentage + "%"
                + "/" + delay
                + "] what=" + what);

        Toast.makeText(context, "Speaking " + ellipsize(what), Toast.LENGTH_SHORT).show();
        int result = tts.speak(what,
                TextToSpeech.QUEUE_FLUSH,
                params, what);

        if (isStillSpeakingAfter(5))
            Log.d(TAG, "STILL speaking");

        if (result != TextToSpeech.SUCCESS)
            Toast.makeText(context, String.format(Locale.getDefault(),
                    "Failed to speak %s.", ellipsize(what)),  Toast.LENGTH_SHORT).show();
        restoreVolume();
    }

    private static boolean isStillSpeakingAfter(int limit) {
        int i = 0;
        while (i < limit && tts.isSpeaking()) {
            slumber(1000); // Sleep for one second
            i++;
        }
        boolean stillSpeaking = tts.isSpeaking();
        Log.d(TAG, "isStillSpeakingAfter; waited " + i + "sec vs limit " + limit
                + ", stillSpeaking=" + stillSpeaking);

        return stillSpeaking;
    }

    public static void slumber(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String ellipsize(String s) {
        String firstLine = s.split("\n")[0];
        return firstLine;
    }

    private class RetrieveUrlTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return new_getURL(urls[0]); //getURL(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "RetrieveUrlTask.onPostExecute; result=" + result);
            resultsString = result;

            ingredientsLayout.removeAllViews();
            buttonGet.setText("Get");
            if (result != null) {
                // Display the retrieved contents in the EditText
                urlResults.setText("Analyzing...");

                ArrayList<String> ingredients = findIngredients(result);
                ingredients.forEach((String ingredient) -> {
                    CheckBox cb = new CheckBox(context);
                    cb.setText(ingredient);
                    cb.setTextColor(Color.WHITE);
                    cb.setLongClickable(true);
                    cb.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("Ingredient", cb.getText());
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(context, "Saved " + cb.getText() + " to clipboard.",
                                    Toast.LENGTH_LONG).show();
                            return true;
                        }
                    });
                    ingredientsLayout.addView(cb);
                    Log.d(TAG, "ADDED checkbox " + ingredient);
                });
                urlResults.setText("Retrieved " + result.length() + " chars. Long-press to copy to clipboard.");
            }
            showProgress(false); //progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private String new_getURL(String urlString) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            URL url = new URL(urlString);
            BufferedReader bReader = new BufferedReader(
                    new InputStreamReader(url.openStream()));

            String line;
            while ((line = bReader.readLine()) != null)
                stringBuilder.append(line).append("\n");

            bReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return stringBuilder.toString();
    }

    private String getURL(String urlString) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            // Create a URL object from the provided URL string
            URL url = new URL(urlString);
            Log.d(TAG, "RetrieveUrlTask.doInBackground; url=" + url);

            // Open a connection to the URL
            HttpURLConnection.setFollowRedirects(true);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000); // 5 seconds

            // Get the response code
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // If the response code is HTTP_OK, read the contents of the URL
                InputStream inputStream = connection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append('\n');
                }

                // Close the streams
                bufferedReader.close();
                inputStream.close();
                connection.disconnect();

                // Return the contents of the URL
                return stringBuilder.toString();
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return "Could not find URL '" + url + "'";
            } else {
                return "Could not get URL '" + url + "'; responseCode=" + responseCode;
            }
        } catch (IOException e) {
            stringBuilder.append(e.getMessage());
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }

    private void alert(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private ArrayList<String> findIngredients(String recipe) {
        Pattern ignore = Pattern.compile("(akes|erves|div class)");
        Pattern match = Pattern.compile("([0-9¼-¾⅐-⅞] *[/0-9-.]*)\\s+(oz|oz.|pound[s]?|can[s]?|small|medium|large|pint[s]?|teaspoon[s]?|tsp|tsp.|tablespoon[s]?|tbsp|tbsp.|Tbsp.|cup[s]?|pinch|stick[s]?|stalk[s]?|container[s]?|egg[s]?|clove[s]?|bunch[es]?|whole)\\s?([A-Za-z0-9 -]+)");
        ArrayList<String> ingredients = new ArrayList<String>();
        String[] lines = recipe
                .replaceAll("<br/>", System.lineSeparator())
                .replaceAll("<p>", System.lineSeparator())
                .replaceAll("<.*?>", "")
                .replaceAll(" \\([0-9][0-9-]*[A-Za-z]+\\)", "") // eliminates " (240ml)" types
                .replaceAll("([0-9¼-¾⅐-⅞][/0-9-.]*) and ([0-9¼-¾⅐-⅞][/0-9-.]*)", "$1 $2") // eliminates " 1 and 1/4 cups" types
                .replaceAll("\\(about .*?\\)", "") // eliminates "(about 1-2)" types
                .split(System.lineSeparator());

        ingredients.size();
        Log.d(TAG, "Reprocess without looking for INGREDIENTS");
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            Matcher matchMatcher = match.matcher(line);
            Matcher ignoreMatcher = ignore.matcher(line);

            boolean matched = true;
            while (matched) {
                int end = 0;
                if (!line.contains("recipeIngredient") && ignoreMatcher.find()) {
                    matched = false;
                    Log.d(TAG, "line=" + line + "; IGNORE");
                } else if (matchMatcher.find()) {
                    matched = true;
                    String ingredient = String.format("%s %s of %s",
                            matchMatcher.group(1),
                            matchMatcher.group(2),
                            matchMatcher.group(3).replaceAll("of ", ""));
                    if (!ingredients.contains(ingredient)) {
                        Log.d(TAG, "line=" + line + "; add match INGREDIENT: " + ingredient);
                        ingredients.add(ingredient);
                    } else {
                        Log.d(TAG, "line=" + line + "; already added match INGREDIENT: " + ingredient);
                    }
                    end = matchMatcher.end();
                } else {
                    matched = false;
                }
                if (matched && end < line.length()) {
                    line = line.substring(end + 1);

                    matchMatcher = match.matcher(line);
                    ignoreMatcher = ignore.matcher(line);
                } else {
                    matched = false;
                }
            }

            i++;
        }

        return ingredients;
    }

    private static boolean isValidURL(String urlString) {
        try {
            URL url = new URL(urlString);
            url.toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void showProgress(boolean flag) {
        progressLayout.setVisibility(flag ? View.VISIBLE : View.INVISIBLE);
        progressBar.setVisibility(flag ? View.VISIBLE : View.INVISIBLE);
    }
}