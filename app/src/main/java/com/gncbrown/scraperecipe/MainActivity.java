package com.gncbrown.scraperecipe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MainActivity extends Activity {

    private String TAG = MainActivity.class.getName();

    private static boolean firstInput = true;
    private static ProgressBar progressBar;
    private static LinearLayout progressLayout;
    private TextView urlResults;
    private TextView ingredientsResult;
    private TextView recipeUrl;
    private Button buttonGet;
    private Button buttonClear;
    private RetrieveUrlTask retrievalUrlTask;

    private String launchType = "MAIN";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressLayout = (LinearLayout) findViewById(R.id.layoutProgress);

        urlResults = (TextView) findViewById(R.id.editTextResults);
        urlResults.setText("THIS IS WHERE\nRESULTS GO");
        ingredientsResult = (TextView) findViewById(R.id.editTextIngredients);
        ingredientsResult.setText("Ingredient 1\nIngredient 2");
        recipeUrl = (TextView) findViewById(R.id.textRecipeUrl);
        recipeUrl.setText("https://sallysbakingaddiction.com/whole-wheat-bread/");
                //"https://thebigmansworld.com/almond-flour-biscotti/#wprm-recipe-container-39177");
                //"https://www.peta.org/recipes/auntie-bonnie-s-chickpea-salad/?utm_source=PETA::Google&utm_medium=Ad&utm_campaign=0422::veg::PETA::Google::SEA-Vegan-Grant::::searchad&gad_source=1&gclid=CjwKCAiAibeuBhAAEiwAiXBoJMxp2f6hu4WYiSOfGxEFFilDfchADUOA4N0LTuwYLJAXnQW2C9EPNhoCZPkQAvD_BwE");
        //"https://www.serenabakessimplyfromscratch.com/2011/05/homemade-egg-noodles.html"); //"https://www.google.com/");

        buttonGet = (Button) findViewById(R.id.buttonGet);
        buttonGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "buttonGet.onClick");
                String urlString = recipeUrl.getText().toString();
                if (!urlString.equals("")) {
                    if (!isValidURL(urlString)) {
                        String message = "URL '" + urlString + "' is invalid.";
                        ingredientsResult.setText("");
                        urlResults.setText(message);
                        alert("URL error", message);
                    } else {
                        String buttonName = buttonGet.getText().toString();
                        firstInput = false;
                        if (buttonName.equals("Get")) {
                            showProgress(true); //progressBar.setVisibility(View.VISIBLE);
                            ingredientsResult.setText("");
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
        buttonClear = (Button) findViewById(R.id.buttonClear);
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "buttonClear.onClick");
                recipeUrl.setText("");
                ingredientsResult.setText("");
                urlResults.setText("");
                showProgress(false);
            }
        });

        showProgress(false); //progressBar.setVisibility(View.INVISIBLE);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        launchType = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && launchType != null) {
            Log.d(TAG, "LAUNCH action=SEND, launchType=" + launchType);
            if ("text/html".equals(launchType) || "text/plain".equals(launchType)) {
                handleSendText(intent); // Handle text being sent
            }
        } else {
            // Handle other intents, such as being started from the home screen
            Log.d(TAG, "LAUNCH from MAIN");
            launchType = "MAIN";
        }

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

    private class RetrieveUrlTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder stringBuilder = new StringBuilder();
            try {
                // Create a URL object from the provided URL string
                URL url = new URL(urls[0]);
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
                        stringBuilder.append(line + '\n');
                    }

                    // Close the streams
                    bufferedReader.close();
                    inputStream.close();

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

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "RetrieveUrlTask.onPostExecute; result=" + result);
            firstInput = true;
            buttonGet.setText("Get");
            if (result != null) {
                // Display the retrieved contents in the EditText
                urlResults.setText("Analyzing...");
                ArrayList<String> ingredients = findIngredients(result);
                urlResults.setText(result);
                ingredientsResult.setText(ingredients.stream().collect(
                        Collectors.joining("\n")));
            }
            showProgress(false); //progressBar.setVisibility(View.INVISIBLE);
        }
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
        Pattern singles = Pattern.compile("([0-9\u00BC-\u00BE\u2150-\u215E] *[/0-9-.]*)\\s+(oz|oz.|pound|can|small|medium|large|pint|teaspoon|tsp|tsp.|tablespoon|tbsp|tbsp.|Tbsp.|cup|pinch|stick|stalk|container|egg|clove|bunch|whole)\\s?([A-Za-z0-9 -]+)");
        Pattern plurals = Pattern.compile("([0-9\u00BC-\u00BE\u2150-\u215E] *[/0-9-.]*)\\s+(cans|ounces|pounds|pints|teaspoons|tablespoons|cups|stalks|containers|eggs|cloves|bunches)\\s?([A-Za-z0-9 -]+)");
        ArrayList<String> ingredients = new ArrayList<String>();
        String[] lines = recipe
                .replaceAll("<br/>", System.lineSeparator())
                .replaceAll("<p>", System.lineSeparator())
                .replaceAll("<.*?>", "")
                .replaceAll(" \\([0-9][0-9-]*[A-Za-z]+\\)", "") // eliminates " (240ml)" types
                .replaceAll("([0-9\u00BC-\u00BE\u2150-\u215E][/0-9-.]*) and ([0-9\u00BC-\u00BE\u2150-\u215E][/0-9-.]*)", "$1 $2") // eliminates " 1 and 1/4 cups" types
                .replaceAll("\\(about .*?\\)", "") // eliminates "(about 1-2)" types
                .split(System.lineSeparator());

        if (ingredients.size() == 0) {
            Log.d(TAG, "Reprocess without looking for INGREDIENTS");
            int i = 0;
            while (i < lines.length) {
                String line = lines[i];
                Matcher ignoreMatcher = ignore.matcher(line);
                Matcher pluralsMatcher = plurals.matcher(line);
                Matcher singleMatcher = singles.matcher(line);

                boolean matched = true;
                while (matched) {
                    int start = 0;
                    int end = 0;
                    if (ignoreMatcher.find()) {
                        matched = false;
                        Log.d(TAG, "line=" + line + "; IGNORE");
                    } else if (pluralsMatcher.find()) {
                        matched = true;
                        String ingredient = String.format("%s %s of %s",
                                pluralsMatcher.group(1),
                                pluralsMatcher.group(2),
                                pluralsMatcher.group(3).replaceAll("of ", ""));
                        if (!ingredients.contains(ingredient)) {
                            Log.d(TAG, "line=" + line + "; add plurals INGREDIENT: " + ingredient);
                            ingredients.add(ingredient);
                        } else {
                            Log.d(TAG, "line=" + line + "; already added plurals INGREDIENT: " + ingredient);
                        }
                        start = pluralsMatcher.start();
                        end = pluralsMatcher.end();
                    } else if (singleMatcher.find()) {
                        matched = true;
                        String ingredient = String.format("%s %s of %s",
                                singleMatcher.group(1),
                                singleMatcher.group(2),
                                singleMatcher.group(3).replaceAll("of ", ""));
                        if (!ingredients.contains(ingredient)) {
                            Log.d(TAG, "line=" + line + "; add singles INGREDIENT: " + ingredient);
                            ingredients.add(ingredient);
                        } else {
                            Log.d(TAG, "line=" + line + "; already added singles INGREDIENT: " + ingredient);
                        }
                        start = singleMatcher.start();
                        end = singleMatcher.end();
                    } else {
                        matched = false;
                    }
                    if (matched && end < line.length()) {
                        line = line.substring(end+1);

                        ignoreMatcher = ignore.matcher(line);
                        pluralsMatcher = plurals.matcher(line);
                        singleMatcher = singles.matcher(line);
                    } else {
                        matched = false;
                    }
                }

                i++;
            }
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