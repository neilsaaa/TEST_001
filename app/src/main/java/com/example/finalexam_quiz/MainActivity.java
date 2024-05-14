package com.example.finalexam_quiz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private TextView questionTextView;
    private Button optionButton1, optionButton2, optionButton3, optionButton4;
    AppDatabase db;
    private int currentIndex = 0;
    private Button backButton, nextButton;
    private int score = 0;
    private List<Question> questions = new ArrayList<>();
    private Button currentButton = null;
    private ArrayList<Button> buttons = new ArrayList<>(); // Add this line
    private ArrayList<String> selectedAnswers = new ArrayList<>();
    private ArrayList<String> correctAnswers = new ArrayList<>();

    @SuppressLint("UnsafeIntentLaunch")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getDatabase(getApplicationContext());
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = database.getReference("Questions");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for (DataSnapshot questionSnapshot: dataSnapshot.getChildren()) {
                    Object question = questionSnapshot.child("content").getValue();
                    Object answer = questionSnapshot.child("answer").getValue();
                    Object a = questionSnapshot.child("a").getValue();
                    if (question instanceof String) {
                        Log.d("FirebaseData", "Answer is: " + answer);
                        Log.d("FirebaseData", "Question is: " + question);
                    } else if (question instanceof Long) {
                        Log.d("FirebaseData", "Question is: " + Long.toString((Long) question));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        try {
            InputStream is = getAssets().open("questions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            Log.d("debug", json);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        SharedPreferences sharedPreferences = getSharedPreferences("OptionPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        // Write a message to the database

        questionTextView = findViewById(R.id.questionTextView);
        optionButton1 = findViewById(R.id.optionButton1);
        optionButton2 = findViewById(R.id.optionButton2);
        optionButton3 = findViewById(R.id.optionButton3);
        optionButton4 = findViewById(R.id.optionButton4);
//        optionButton1.setOnClickListener(optionListener);
//        optionButton2.setOnClickListener(optionListener);
//        optionButton3.setOnClickListener(optionListener);
//        optionButton4.setOnClickListener(optionListener);

        View.OnClickListener optionClickListener = v -> onOptionSelected(v, currentIndex);

        optionButton1.setOnClickListener(optionClickListener);
        optionButton2.setOnClickListener(optionClickListener);
        optionButton3.setOnClickListener(optionClickListener);
        optionButton4.setOnClickListener(optionClickListener);

        optionButton1.setBackgroundColor(Color.parseColor("#D6EBFD"));
        optionButton2.setBackgroundColor(Color.parseColor("#D6EBFD"));
        optionButton3.setBackgroundColor(Color.parseColor("#D6EBFD"));
        optionButton4.setBackgroundColor(Color.parseColor("#D6EBFD"));
        optionButton1.setTextColor(Color.BLACK);
        optionButton2.setTextColor(Color.BLACK);
        optionButton3.setTextColor(Color.BLACK);
        optionButton4.setTextColor(Color.BLACK);

        GridLayout questionsGridLayout = findViewById(R.id.questionsGridLayout);
        Button finishButton = findViewById(R.id.finishButton);
        finishButton.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Finish Quiz")
                    .setMessage("Are you sure you want to finish?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        finishButton.setVisibility(View.GONE);
                        displayResult();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
        new Thread(() -> {
            questions.addAll(db.questionDao().getAll());
            runOnUiThread(() -> {
                for (int i = 0; i < questions.size(); i++) {
                    selectedAnswers.add("No Answer");
                    Button button = new Button(MainActivity.this);
                    button.setText(String.valueOf(i + 1));
                    button.setTextColor(Color.WHITE);
                    final int questionIndex = i;
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = 0;
                    params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                    params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                    params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);


                    button.setLayoutParams(params);
                    button.setBackground(ContextCompat.getDrawable(this, R.drawable.button_states));

                    button.setOnClickListener(v -> {
                        // Reset color of previously selected button
                        if (currentButton != null) {
                            currentButton.setSelected(false);

                        }
                        // Change color of the selected button
                        button.setSelected(true);

                        // Keep track of the current selected button
                        currentButton = button;

                        currentIndex = questionIndex;
                        showQuestion(questions.get(currentIndex));
                    });
                    buttons.add(button); // Add this line

                    questionsGridLayout.addView(button);
                    correctAnswers.add(questions.get(i).answer);
                }
                if (!questions.isEmpty()) {
                    showQuestion(questions.get(0));
                }
            });
        }).start();

        backButton = findViewById(R.id.backButton);
        nextButton = findViewById(R.id.nextButton);
        Button homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            // reset all
            finish();
            startActivity(getIntent());
            overridePendingTransition(0, 0);
        });
        nextButton.setOnClickListener(v -> {
            if (currentIndex < questions.size() - 1) {
                currentIndex++;
                showQuestion(questions.get(currentIndex));
            } else {
                displayResult();
            }
        });
        backButton.setOnClickListener(v -> {
            currentIndex = currentIndex -  1;
            showQuestion(questions.get(currentIndex));
        });
    }

    private void onOptionSelected(View view, int questionNumber) {
        Button option1Button = findViewById(R.id.optionButton1);
        Button option2Button = findViewById(R.id.optionButton2);
        Button option3Button = findViewById(R.id.optionButton3);
        Button option4Button = findViewById(R.id.optionButton4);
        // Reset background color for all buttons
        option1Button.setBackgroundColor(Color.parseColor("#D6EBFD"));
        option2Button.setBackgroundColor(Color.parseColor("#D6EBFD"));
        option3Button.setBackgroundColor(Color.parseColor("#D6EBFD"));
        option4Button.setBackgroundColor(Color.parseColor("#D6EBFD"));

        Button selectedOption = (Button) view;

        selectedOption.setBackgroundColor(Color.parseColor("#6a5be2"));

        SharedPreferences sharedPreferences = getSharedPreferences("OptionPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("lastSelectedOptionState" + questionNumber, selectedOption.getText().toString());
        editor.apply();

        buttons.get(currentIndex).setBackgroundResource(R.drawable.button_answered_state);
        String selectedAnswer = selectedOption.getText().toString();
        selectedAnswers.set(questionNumber, selectedAnswer);
    }


    View.OnClickListener optionListener = view -> {
        Button b = (Button) view;
        String answer = b.getText().toString();
        if (answer.equals(questions.get(currentIndex).answer)) {
            score++;
        }

        buttons.get(currentIndex).setBackgroundResource(R.drawable.button_answered_state);
    };

    private void showQuestion(Question question) {
        Type type = new TypeToken<List<String>>() {
        }.getType();
        List<String> options = new Gson().fromJson(question.options, type);
        optionButton1.setVisibility(View.VISIBLE);
        optionButton2.setVisibility(View.VISIBLE);
        optionButton3.setVisibility(View.VISIBLE);
        optionButton4.setVisibility(View.VISIBLE);
        optionButton1.setBackgroundColor(Color.parseColor("#D6EBFD"));
        optionButton2.setBackgroundColor(Color.parseColor("#D6EBFD"));
        optionButton3.setBackgroundColor(Color.parseColor("#D6EBFD"));
        optionButton4.setBackgroundColor(Color.parseColor("#D6EBFD"));
        if (currentButton != null) {
            currentButton.setSelected(false);
        }

        // Change color of the current button
        currentButton = buttons.get(currentIndex);
        currentButton.setSelected(true);
        String questionWithIndex = (currentIndex + 1) + ". " + question.content;
        questionTextView.setText(questionWithIndex);

        optionButton1.setText(options.get(0));
        optionButton2.setText(options.get(1));
        optionButton3.setText(options.get(2));
        optionButton4.setText(options.get(3));
        SharedPreferences sharedPreferences = getSharedPreferences("OptionPrefs", Context.MODE_PRIVATE);
        String lastSelectedOptionState = sharedPreferences.getString("lastSelectedOptionState" + currentIndex, null);

        if (lastSelectedOptionState != null) {
            if (optionButton1.getText().toString().equals(lastSelectedOptionState)) {
                optionButton1.setBackgroundColor(Color.parseColor("#6a5be2"));
            } else if (optionButton2.getText().toString().equals(lastSelectedOptionState)) {
                optionButton2.setBackgroundColor(Color.parseColor("#6a5be2"));
            } else if (optionButton3.getText().toString().equals(lastSelectedOptionState)) {
                optionButton3.setBackgroundColor(Color.parseColor("#6a5be2"));
            } else if (optionButton4.getText().toString().equals(lastSelectedOptionState)) {
                optionButton4.setBackgroundColor(Color.parseColor("#6a5be2"));
            }
        }
        if (currentIndex > 0) {
            backButton.setVisibility(View.VISIBLE);
        }else{
            backButton.setVisibility(View.GONE);
        }
    }
    private int calculateScore() {
        score = 0; // Reset the score
        for (int i = 0; i < correctAnswers.size(); i++) {
            if (i < selectedAnswers.size() && correctAnswers.get(i).equals(selectedAnswers.get(i))) {
                score++;
            }
        }
        return score;
    }

    private void displayResult() {
        questionTextView.setText("Quiz Finished! Your score: " + calculateScore() + "/" + questions.size());
        optionButton1.setVisibility(View.GONE);
        optionButton2.setVisibility(View.GONE);
        optionButton3.setVisibility(View.GONE);
        optionButton4.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
        Button finishButton = findViewById(R.id.finishButton);
        finishButton.setVisibility(View.GONE);
        //hide the buttons of questions
        for (Button button : buttons) {
            button.setVisibility(View.GONE);
        }
        Button homeButton = findViewById(R.id.homeButton);
        homeButton.setVisibility(View.VISIBLE);
        ScrollView resultScrollView = findViewById(R.id.resultLayout);
        LinearLayout resultLayout = new LinearLayout(this);
        resultLayout.setOrientation(LinearLayout.VERTICAL);
        resultScrollView.removeAllViews(); // Clear old results
        resultScrollView.addView(resultLayout); // Add LinearLayout to ScrollView

        for (int i = 0; i < questions.size(); i++) {
            TextView questionView = new TextView(this);
            questionView.setText("Question " + (i + 1) + ": " + questions.get(i).content);
            resultLayout.addView(questionView);
            TextView userAnswerView = new TextView(this);
            String userAnswer = selectedAnswers.get(i);
            userAnswerView.setText("Your answer: " + userAnswer);
            resultLayout.addView(userAnswerView); // Add userAnswerView to resultLayout first

            String correctAnswer = correctAnswers.get(i);
            if (userAnswer.equals(correctAnswer)) {
                questionView.setTextColor(Color.GREEN);
            } else {
                questionView.setTextColor(Color.RED);
                TextView correctAnswerView = new TextView(this);
                correctAnswerView.setText("Correct answer: " + correctAnswer);
                resultLayout.addView(correctAnswerView); // Add correctAnswerView to resultLayout after userAnswerView
            }
        }


        resultScrollView.setVisibility(View.VISIBLE); // Show the result layout
    }
}