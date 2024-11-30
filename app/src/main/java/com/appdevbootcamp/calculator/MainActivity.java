package com.appdevbootcamp.calculator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView textScreen;
    private ImageView btnSpeak;
    private boolean lastNumeric;
    private boolean stateError;
    private boolean lastDot;

    // Arrays to store numeric and operator button IDs
    private int[] numericButtons = {
            R.id.btnZero, R.id.btnOne, R.id.btnTwo, R.id.btnThree,
            R.id.btnFour, R.id.btnFive, R.id.btnSix,
            R.id.btnSeven, R.id.btnEight, R.id.btnNine
    };

    private int[] operatorButtons = {
            R.id.btnAdd, R.id.btnSubs, R.id.btnMult, R.id.btnDevi
    };

    private final int REQ_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        btnSpeak = findViewById(R.id.btnSpeak);
        textScreen = findViewById(R.id.textScreen);

        // Set up button click listeners
        setNumericOnClickListener();
        setOperatorOnClickListener();
    }

    /**
     * Set up click listeners for numeric buttons
     */
    private void setNumericOnClickListener() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button button = (Button) view;
                if (stateError) {
                    textScreen.setText(button.getText());
                    stateError = false;
                } else {
                    textScreen.append(button.getText());
                }
                lastNumeric = true;
            }
        };

        // Assign the listener to all numeric buttons
        for (int id : numericButtons) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    /**
     * Set up click listeners for operators and other control buttons
     */
    private void setOperatorOnClickListener() {
        // Operator buttons listener
        View.OnClickListener operatorListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lastNumeric && !stateError) {
                    Button button = (Button) view;
                    textScreen.append(button.getText());
                    lastNumeric = false;
                    lastDot = false;
                }
            }
        };

        // Set the listener for each operator button
        for (int id : operatorButtons) {
            findViewById(id).setOnClickListener(operatorListener);
        }

        // Decimal point button listener
        findViewById(R.id.btnPoint).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lastNumeric && !stateError && !lastDot) {
                    textScreen.append(".");
                    lastNumeric = false;
                    lastDot = true;
                }
            }
        });

        // Clear button listener
        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textScreen.setText("");
                lastNumeric = false;
                stateError = false;
                lastDot = false;
            }
        });

        // Equal button listener
        findViewById(R.id.btnEqual).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEqual();
            }
        });

        // Speech button listener
        findViewById(R.id.btnSpeak).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (stateError) {
                    textScreen.setText("Try Again");
                    stateError = false;
                } else {
                    speechInput();
                }
                lastNumeric = true;
            }
        });
    }

    /**
     * Handle equals button click
     */
    private void onEqual() {
        if (lastNumeric && !stateError) {
            String text = textScreen.getText().toString();
            try {
                Expression expression = new ExpressionBuilder(text).build();
                double result = expression.evaluate();
                textScreen.setText(Double.toString(result));
                lastDot = true; // Result contains a decimal point
            } catch (ArithmeticException e) {
                textScreen.setText("Error");
                stateError = true;
                lastNumeric = false;
            } catch (Exception e) {
                textScreen.setText("Error");
                stateError = true;
                lastNumeric = false;
            }
        }
    }

    /**
     * Initialize speech input
     */
    private void speechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_promot));

        try {
            startActivityForResult(intent, REQ_CODE);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle speech recognition result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String change = result.get(0);
                    textScreen.setText(result.get(0));

                    openResponse(result.get(0));

                    // Process mathematical operations
                    change = processMathOperations(change);

                    // Handle equals sign
                    if (change.contains("=")) {
                        change = change.replace("=", "");
                        textScreen.setText(change);
                        onEqual();
                    } else {
                        textScreen.setText(change);
                    }
                }
            }
        }
    }

    /**
     * Process mathematical operations from speech input
     */
    private String processMathOperations(String input) {
        String result = input;

        // Basic operations
        result = result.replace("divide by", "/")
                .replace("into", "*")
                .replace("X", "*")
                .replace("x", "*")
                .replace("add", "+")
                .replace("plus", "+")
                .replace("subtract", "-")
                .replace("subtract by", "-")
                .replace("equal", "=")
                .replace("equals", "=");

        // Trigonometric values
        result = processTrigValues(result);

        return result;
    }

    /**
     * Process trigonometric values from speech input
     */
    private String processTrigValues(String input) {
        String result = input;

        // Sin values
        result = result.replace("sin 30 equals", "0.5")
                .replace("sin 30 equal", "0.5")
                .replace("sin 45 equals", "0.707106781187")
                .replace("sin 45 equal", "0.707106781187")
                .replace("sin 60 equals", "0.866025403784")
                .replace("sin 60 equal", "0.866025403784")
                .replace("sin 90 equals", "1")
                .replace("sin 90 equal", "1");

        // Cos values
        result = result.replace("cos 30 equals", "0.866025403784")
                .replace("cos 30 equal", "0.866025403784")
                .replace("cos 45 equals", "0.707106781187")
                .replace("cos 45 equal", "0.707106781187")
                .replace("cos 60 equals", "0.5")
                .replace("cos 60 equal", "0.5")
                .replace("cos 90 equals", "0")
                .replace("cos 90 equal", "0");

        // Tan values
        result = result.replace("tan 30 equals", "0.57735026919")
                .replace("tan 30 equal", "0.57735026919")
                .replace("tan 45 equals", "1")
                .replace("tan 45 equal", "1")
                .replace("tan 60 equals", "1.73205080757")
                .replace("tan 60 equal", "1.73205080757")
                .replace("tan 90 equals", "Undefined")
                .replace("tan 90 equal", "Undefined");

        return result;
    }

    /**
     * Handle voice commands to open applications
     */
    private void openResponse(String msg) {
        String msgs = msg.toLowerCase(Locale.ROOT);

        if (msgs.contains("open")) {
            if (msgs.contains("google") || msgs.contains("chrome")) {
                openUrl("https://www.google.com");
            } else if (msgs.contains("youtube")) {
                openUrl("https://www.youtube.com");
            } else if (msgs.contains("facebook")) {
                openUrl("https://www.facebook.com");
            }
        }
    }

    /**
     * Helper method to open URLs
     */
    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}