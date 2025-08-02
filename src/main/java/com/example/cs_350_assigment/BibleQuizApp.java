package com.example.cs_350_assigment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

public class BibleQuizApp extends Application {
    
    // Data classes
    public static class Question {
        private String question;
        private String option_a;
        private String option_b;
        private String option_c;
        private String option_d;
        private String answer;
        
        // Constructors
        public Question() {}
        
        public Question(String question, String option_a, String option_b, 
                       String option_c, String option_d, String answer) {
            this.question = question;
            this.option_a = option_a;
            this.option_b = option_b;
            this.option_c = option_c;
            this.option_d = option_d;
            this.answer = answer;
        }
        
        // Getters and setters
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        
        public String getOption_a() { return option_a; }
        public void setOption_a(String option_a) { this.option_a = option_a; }
        
        public String getOption_b() { return option_b; }
        public void setOption_b(String option_b) { this.option_b = option_b; }
        
        public String getOption_c() { return option_c; }
        public void setOption_c(String option_c) { this.option_c = option_c; }
        
        public String getOption_d() { return option_d; }
        public void setOption_d(String option_d) { this.option_d = option_d; }
        
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        
        public String getOptionByKey(String key) {
            return switch (key) {
                case "option_a" -> option_a;
                case "option_b" -> option_b;
                case "option_c" -> option_c;
                case "option_d" -> option_d;
                default -> "";
            };
        }
    }
    
    public static class QuizRecord {
        private final String playerName;
        private final int score;
        private final int totalQuestions;
        private final String date;
        private final String time;
        
        public QuizRecord(String playerName, int score, int totalQuestions) {
            this.playerName = playerName;
            this.score = score;
            this.totalQuestions = totalQuestions;
            LocalDateTime now = LocalDateTime.now();
            this.date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            this.time = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
        
        // Getters
        public String getPlayerName() { return playerName; }
        public int getScore() { return score; }
        public int getTotalQuestions() { return totalQuestions; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        
        public double getPercentage() {
            return (double) score / totalQuestions * 100;
        }
    }
    
    // UI components
    private Stage primaryStage;
    private Scene startScene, nameScene, quizScene, endScene, historyScene;
    private Label questionLabel, timerLabel, scoreLabel, finalScoreLabel;
    private Button optionA, optionB, optionC, optionD;
    private Button startButton, restartButton, exitButton, historyButton, backButton;
    private TextField nameField;
    private ProgressBar progressBar;
    private TableView<QuizRecord> historyTable;
    
    // Quiz data
    private List<Question> allQuestions;
    private List<Question> quizQuestions;
    private List<QuizRecord> quizHistory;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int timeRemaining = 30;
    private Timeline timer;
    private boolean answerSelected = false;
    private String playerName = "";
    private final int QUIZ_SIZE = 15;
    private final String HISTORY_FILE = "quiz_history.txt";
    
    // Styling
    private final String BUTTON_STYLE = 
        "-fx-font-size: 14px; -fx-padding: 10 20 10 20; -fx-background-radius: 5;";
    private final String OPTION_BUTTON_STYLE = 
        BUTTON_STYLE + " -fx-background-color: #E8E8E8; -fx-text-fill: #333;";
    private final String CORRECT_STYLE = 
        BUTTON_STYLE + " -fx-background-color: #4CAF50; -fx-text-fill: white;";
    private final String INCORRECT_STYLE = 
        BUTTON_STYLE + " -fx-background-color: #F44336; -fx-text-fill: white;";
    private final String START_BUTTON_STYLE = 
        "-fx-font-size: 14px; -fx-padding: 10 20 10 20; -fx-background-color: #2196F3; " +
        "-fx-text-fill: white; -fx-background-radius: 5;";
    

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Load questions and history
        loadQuestions();
        loadHistory();
        
        // Create scenes
        createStartScene();
        createNameScene();
        createQuizScene();
        createEndScene();
        createHistoryScene();
        
        // Setup stage
        primaryStage.setTitle("Bible Quiz Application");
        primaryStage.setScene(startScene);
        primaryStage.setResizable(false);
        primaryStage.show();
        
        // Handle window closing
        primaryStage.setOnCloseRequest(e -> {
            if (timer != null) {
                timer.stop();
            }
            Platform.exit();
        });
    }
    
    private void loadQuestions() {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/bible_questions.json");
            if (inputStream == null) {
                throw new FileNotFoundException("bible_questions.json not found in resources");
            }
            InputStreamReader reader = new InputStreamReader(inputStream);
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            JsonArray questionsArray = jsonObject.getAsJsonArray("questions");
            
            allQuestions = new ArrayList<>();
            for (JsonElement element : questionsArray) {
                JsonObject questionObj = element.getAsJsonObject();
                Question q = new Question();
                q.setQuestion(questionObj.get("question").getAsString());
                q.setOption_a(questionObj.get("option_a").getAsString());
                q.setOption_b(questionObj.get("option_b").getAsString());
                q.setOption_c(questionObj.get("option_c").getAsString());
                q.setOption_d(questionObj.get("option_d").getAsString());
                q.setAnswer(questionObj.get("answer").getAsString());
                allQuestions.add(q);
            }
            reader.close();
            
        } catch (Exception e) {
            showAlert("Error", e +
                     "Please ensure the file is in the same directory as the application.");
            e.printStackTrace();
        }
    }
    
    private void loadHistory() {
        quizHistory = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    QuizRecord record = new QuizRecord(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    quizHistory.add(record);
                }
            }
        } catch (FileNotFoundException e) {
            // File doesn't exist yet, which is fine
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void saveHistory() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(HISTORY_FILE, true))) {
            QuizRecord lastRecord = quizHistory.get(quizHistory.size() - 1);
            writer.println(lastRecord.getPlayerName() + "," + 
                          lastRecord.getScore() + "," + 
                          lastRecord.getTotalQuestions() + "," + 
                          lastRecord.getDate() + "," + 
                          lastRecord.getTime());
        } catch (IOException e) {
        }
    }
    
    private void createStartScene() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        // Title
        Label titleLabel = new Label("Bible Quiz Challenge");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.DARKBLUE);
        
        // // Instructions
        // VBox instructionsBox = new VBox(10);
        // instructionsBox.setAlignment(Pos.CENTER);
        // instructionsBox.setMaxWidth(500);
        
        // Label instructionsTitle = new Label("Instructions:");
        // instructionsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        
        // Text instructions = new Text(
        //     "• Answer " + QUIZ_SIZE + " randomly selected Bible questions\n" +
        //     "• Each question has a 30-second time limit\n" +
        //     "• Select your answer by clicking on the options\n" +
        //     "• Green indicates correct, red indicates incorrect\n" +
        //     "• Quiz ends if time runs out on any question\n" +
        //     "• Try to get the highest score possible!"
        // );
        // instructions.setFont(Font.font("Arial", 14));
        // instructions.wrappingWidthProperty().bind(instructionsBox.widthProperty().subtract(20));
        
        // instructionsBox.getChildren().addAll(instructionsTitle, instructions);
        
        // Buttons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        startButton = new Button("Start Quiz");
        startButton.setStyle(START_BUTTON_STYLE);
        startButton.setOnAction(e -> primaryStage.setScene(nameScene));
        
        historyButton = new Button("View History");
        historyButton.setStyle(BUTTON_STYLE + " -fx-background-color: #FF9800; -fx-text-fill: white;");
        historyButton.setOnAction(e -> {
            refreshHistoryTable();
            primaryStage.setScene(historyScene);
        });
        
        buttonBox.getChildren().addAll(startButton, historyButton);
        
        root.getChildren().addAll(titleLabel, buttonBox);
        
        startScene = new Scene(root, 650, 550);
    }
    
    private void createNameScene() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: #F5F5F5;");
        
        // Title
        Label titleLabel = new Label("Enter Your Name");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.DARKBLUE);
        
        // Name input
        VBox nameBox = new VBox(15);
        nameBox.setAlignment(Pos.CENTER);
        nameBox.setMaxWidth(400);
        
        Label nameLabel = new Label("Player Name:");
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        nameField = new TextField();
        nameField.setPromptText("Enter your name here");
        nameField.setStyle("-fx-font-size: 16px; -fx-padding: 10;");
        nameField.setMaxWidth(300);
        
        nameBox.getChildren().addAll(nameLabel, nameField);
        
        // Buttons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button proceedButton = new Button("Start Quiz");
        proceedButton.setStyle(START_BUTTON_STYLE);
        proceedButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                showAlert("Name Required", "Please enter your name before starting the quiz.");
                return;
            }
            playerName = name;
            startQuiz();
        });
        
        Button backToMenuButton = new Button("Back to Menu");
        backToMenuButton.setStyle(BUTTON_STYLE + " -fx-background-color: #757575; -fx-text-fill: white;");
        backToMenuButton.setOnAction(e -> primaryStage.setScene(startScene));
        
        buttonBox.getChildren().addAll(backToMenuButton, proceedButton);
        
        // Handle Enter key
        nameField.setOnAction(e -> proceedButton.fire());
        
        root.getChildren().addAll(titleLabel, nameBox, buttonBox);
        
        nameScene = new Scene(root, 650, 400);
    }
    
    private void createQuizScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F8F8F8;");
        
        // Top section - Progress and Timer
        HBox topBox = new HBox(20);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(20));
        topBox.setStyle("-fx-background-color: #2196F3;");
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.setStyle("-fx-accent: #4CAF50;");
        
        timerLabel = new Label("Time: 30");
        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        timerLabel.setTextFill(Color.WHITE);
        
        scoreLabel = new Label("Score: 0/" + QUIZ_SIZE);
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        scoreLabel.setTextFill(Color.WHITE);
        
        topBox.getChildren().addAll(progressBar, timerLabel, scoreLabel);
        
        // Center section - Question and options
        VBox centerBox = new VBox(20);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(30));
        
        questionLabel = new Label();
        questionLabel.setWrapText(true);
        questionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        questionLabel.setMaxWidth(600);
        questionLabel.setAlignment(Pos.CENTER);
        
        // Options grid
        GridPane optionsGrid = new GridPane();
        optionsGrid.setAlignment(Pos.CENTER);
        optionsGrid.setHgap(20);
        optionsGrid.setVgap(15);
        
        optionA = createOptionButton();
        optionB = createOptionButton();
        optionC = createOptionButton();
        optionD = createOptionButton();
        
        optionA.setOnAction(e -> selectAnswer("option_a"));
        optionB.setOnAction(e -> selectAnswer("option_b"));
        optionC.setOnAction(e -> selectAnswer("option_c"));
        optionD.setOnAction(e -> selectAnswer("option_d"));
        
        optionsGrid.add(optionA, 0, 0);
        optionsGrid.add(optionB, 1, 0);
        optionsGrid.add(optionC, 0, 1);
        optionsGrid.add(optionD, 1, 1);
        
        centerBox.getChildren().addAll(questionLabel, optionsGrid);
        
        root.setTop(topBox);
        root.setCenter(centerBox);
        
        quizScene = new Scene(root, 750, 600);
    }
    
    private Button createOptionButton() {
        Button button = new Button();
        button.setStyle(OPTION_BUTTON_STYLE);
        button.setPrefWidth(250);
        button.setPrefHeight(60);
        button.setWrapText(true);
        button.setAlignment(Pos.CENTER);
        return button;
    }
    
    private void createEndScene() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: #F5F5F5;");
        
        Label titleLabel = new Label("Quiz Complete!");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.DARKGREEN);
        
        finalScoreLabel = new Label();
        finalScoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        finalScoreLabel.setTextFill(Color.DARKBLUE);
        
        Label playerLabel = new Label();
        playerLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        playerLabel.setTextFill(Color.DARKBLUE);
        playerLabel.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
            () -> "Player: " + playerName
        ));
        
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        restartButton = new Button("Play Again");
        restartButton.setStyle(START_BUTTON_STYLE);
        restartButton.setOnAction(e -> primaryStage.setScene(nameScene));
        
        Button viewHistoryButton = new Button("View History");
        viewHistoryButton.setStyle(BUTTON_STYLE + " -fx-background-color: #FF9800; -fx-text-fill: white;");
        viewHistoryButton.setOnAction(e -> {
            refreshHistoryTable();
            primaryStage.setScene(historyScene);
        });
        
        exitButton = new Button("Exit");
        exitButton.setStyle(BUTTON_STYLE + " -fx-background-color: #757575; -fx-text-fill: white;");
        exitButton.setOnAction(e -> Platform.exit());
        
        buttonBox.getChildren().addAll(restartButton, viewHistoryButton, exitButton);
        
        root.getChildren().addAll(titleLabel, new Label("Player: " + playerName), finalScoreLabel, buttonBox);
        
        endScene = new Scene(root, 600, 400);
    }
    
    private void createHistoryScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F5F5F5;");
        
        // Title
        VBox titleBox = new VBox(10);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Quiz History");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.DARKBLUE);
        
        titleBox.getChildren().add(titleLabel);
        
        // History table
        historyTable = new TableView<>();
        historyTable.setStyle("-fx-font-size: 14px;");
        
        TableColumn<QuizRecord, String> nameCol = new TableColumn<>("Player Name");
        nameCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPlayerName()));
        nameCol.setPrefWidth(150);
        
        TableColumn<QuizRecord, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getScore() + "/" + cellData.getValue().getTotalQuestions()));
        scoreCol.setPrefWidth(100);
        
        TableColumn<QuizRecord, String> percentageCol = new TableColumn<>("Percentage");
        percentageCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                String.format("%.1f%%", cellData.getValue().getPercentage())));
        percentageCol.setPrefWidth(120);
        
        TableColumn<QuizRecord, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDate()));
        dateCol.setPrefWidth(120);
        
        TableColumn<QuizRecord, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTime()));
        timeCol.setPrefWidth(100);
        
        historyTable.getColumns().addAll(nameCol, scoreCol, percentageCol, dateCol, timeCol);
        
        // Buttons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20));
        
        backButton = new Button("Back to Menu");
        backButton.setStyle(START_BUTTON_STYLE);
        backButton.setOnAction(e -> primaryStage.setScene(startScene));
        
        Button clearHistoryButton = new Button("Clear History");
        clearHistoryButton.setStyle(BUTTON_STYLE + " -fx-background-color: #F44336; -fx-text-fill: white;");
        clearHistoryButton.setOnAction(e -> clearHistory());
        
        buttonBox.getChildren().addAll(backButton, clearHistoryButton);
        
        root.setTop(titleBox);
        root.setCenter(historyTable);
        root.setBottom(buttonBox);
        
        historyScene = new Scene(root, 700, 500);
    }
    
    private void refreshHistoryTable() {
        historyTable.getItems().clear();
        // Sort by date/time (newest first)
        quizHistory.sort((a, b) -> {
            int dateCompare = b.getDate().compareTo(a.getDate());
            if (dateCompare != 0) return dateCompare;
            return b.getTime().compareTo(a.getTime());
        });
        historyTable.getItems().addAll(quizHistory);
    }
    
    private void clearHistory() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear History");
        alert.setHeaderText("Are you sure you want to clear all quiz history?");
        alert.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            quizHistory.clear();
            historyTable.getItems().clear();
            
            // Delete the history file
            try {
                File historyFile = new File(HISTORY_FILE);
                if (historyFile.exists()) {
                    historyFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void startQuiz() {
        // Reset quiz variables
        currentQuestionIndex = 0;
        score = 0;
        timeRemaining = 30;
        answerSelected = false;
        
        // Select random questions
        if (allQuestions.size() < QUIZ_SIZE) {
            showAlert("Error", "Not enough questions available. Need at least " + QUIZ_SIZE + " questions.");
            return;
        }
        
        Collections.shuffle(allQuestions);
        quizQuestions = new ArrayList<>(allQuestions.subList(0, QUIZ_SIZE));
        
        // Update UI
        updateQuizUI();
        primaryStage.setScene(quizScene);
        
        // Start timer
        startTimer();
    }
    
    private void updateQuizUI() {
        if (currentQuestionIndex >= quizQuestions.size()) {
            endQuiz();
            return;
        }
        
        Question currentQuestion = quizQuestions.get(currentQuestionIndex);
        
        // Update progress
        progressBar.setProgress((double) currentQuestionIndex / QUIZ_SIZE);
        
        // Update question and options
        questionLabel.setText((currentQuestionIndex + 1) + ". " + currentQuestion.getQuestion());
        optionA.setText("A) " + currentQuestion.getOption_a());
        optionB.setText("B) " + currentQuestion.getOption_b());
        optionC.setText("C) " + currentQuestion.getOption_c());
        optionD.setText("D) " + currentQuestion.getOption_d());
        
        // Reset button styles
        resetOptionButtons();
        
        // Update score
        scoreLabel.setText("Score: " + score + "/" + QUIZ_SIZE);
        
        // Reset answer selection
        answerSelected = false;
        timeRemaining = 30;
        
        // Enable buttons
        setOptionButtonsDisabled(false);
    }
    
    private void resetOptionButtons() {
        optionA.setStyle(OPTION_BUTTON_STYLE);
        optionB.setStyle(OPTION_BUTTON_STYLE);
        optionC.setStyle(OPTION_BUTTON_STYLE);
        optionD.setStyle(OPTION_BUTTON_STYLE);
    }
    
    private void setOptionButtonsDisabled(boolean disabled) {
        optionA.setDisable(disabled);
        optionB.setDisable(disabled);
        optionC.setDisable(disabled);
        optionD.setDisable(disabled);
    }
    
    private void selectAnswer(String selectedOption) {
        if (answerSelected) return;
        
        answerSelected = true;
        setOptionButtonsDisabled(true);
        
        Question currentQuestion = quizQuestions.get(currentQuestionIndex);
        String correctAnswer = currentQuestion.getAnswer();
        
        // Update button styles
        Button[] buttons = {optionA, optionB, optionC, optionD};
        String[] options = {"option_a", "option_b", "option_c", "option_d"};
        
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(correctAnswer)) {
                buttons[i].setStyle(CORRECT_STYLE);
            } else if (options[i].equals(selectedOption)) {
                buttons[i].setStyle(INCORRECT_STYLE);
            }
        }
        
        // Update score
        if (selectedOption.equals(correctAnswer)) {
            score++;
            scoreLabel.setText("Score: " + score + "/" + QUIZ_SIZE);
        }
        
        // Stop timer
        if (timer != null) {
            timer.stop();
        }
        
        // Move to next question after delay
        Timeline delay = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            currentQuestionIndex++;
            updateQuizUI();
            if (currentQuestionIndex < QUIZ_SIZE) {
                startTimer();
            }
        }));
        delay.play();
    }
    
    private void startTimer() {
        timeRemaining = 30;
        timerLabel.setText("Time: " + timeRemaining);
        
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeRemaining--;
            timerLabel.setText("Time: " + timeRemaining);
            
            // Change timer color when time is running out
            if (timeRemaining <= 10) {
                timerLabel.setTextFill(Color.RED);
            } else {
                timerLabel.setTextFill(Color.WHITE);
            }
            
            if (timeRemaining <= 0) {
                timeUp();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }
    
    private void timeUp() {
        if (timer != null) {
            timer.stop();
        }
        
        if (!answerSelected) {
            answerSelected = true;
            setOptionButtonsDisabled(true);
            
            // Show correct answer
            Question currentQuestion = quizQuestions.get(currentQuestionIndex);
            String correctAnswer = currentQuestion.getAnswer();
            
            Button[] buttons = {optionA, optionB, optionC, optionD};
            String[] options = {"option_a", "option_b", "option_c", "option_d"};
            
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(correctAnswer)) {
                    buttons[i].setStyle(CORRECT_STYLE);
                    break;
                }
            }
            
            // Show alert and end quiz
            Timeline delay = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
                showAlert("Time's Up!", "Time ran out! Quiz ended.\nFinal Score: " + score + "/" + QUIZ_SIZE);
                endQuiz();
            }));
            delay.play();
        }
    }

    private void endQuiz() {
        if (timer != null) {
            timer.stop();
        }
        
        // Save to history
        QuizRecord record = new QuizRecord(playerName, score, QUIZ_SIZE);
        quizHistory.add(record);
        saveHistory();
        
        // Update end scene
        finalScoreLabel.setText("Final Score: " + score + "/" + QUIZ_SIZE + 
                               " (" + String.format("%.1f%%", record.getPercentage()) + ")");
        
        // Update player label in end scene
        ((VBox) endScene.getRoot()).getChildren().set(1, new Label("Player: " + playerName) {{
            setFont(Font.font("Arial", FontWeight.NORMAL, 18));
            setTextFill(Color.DARKBLUE);
        }});
        
        primaryStage.setScene(endScene);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}