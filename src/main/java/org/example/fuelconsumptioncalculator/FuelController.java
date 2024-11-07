package org.example.fuelconsumptioncalculator;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

public class FuelController {
    @FXML
    private Label titleLabel;
    @FXML
    private Label distanceLabel;
    @FXML
    private Label fuelLabel;
    @FXML
    private Label resultLabel;
    @FXML
    private TextField distanceField;
    @FXML
    private TextField fuelField;
    @FXML
    private ComboBox<String> languageComboBox;
    @FXML
    private Button calculateButton;
    @FXML
    private Button resetButton;

    private ResourceBundle bundle;

    @FXML
    private void initialize() {
        languageComboBox.getItems().addAll("English", "French", "Japanese", "Farsi");
        languageComboBox.setValue("English");
        updateLanguage();

        languageComboBox.setOnAction(event -> updateLanguage());
    }

    private void updateLanguage() {
        String selectedLanguage = languageComboBox.getValue();
        Locale locale;
        switch (selectedLanguage) {
            case "French":
                locale = new Locale("fr", "FR");
                break;
            case "Japanese":
                locale = new Locale("ja", "JP");
                break;
            case "Farsi":
                locale = new Locale("fa", "IR");
                break;
            default:
                locale = new Locale("en", "UK");
                break;
        }

        bundle = ResourceBundle.getBundle("messages", locale);

        titleLabel.setText(bundle.getString("app.title"));
        distanceLabel.setText(bundle.getString("distance"));
        fuelLabel.setText(bundle.getString("fuel"));
        calculateButton.setText(bundle.getString("calculate"));
        resetButton.setText(bundle.getString("reset"));
        resultLabel.setText(""); // Clear the result label on language change
    }

    @FXML
    public void calculateFuelConsumption() {
        if (bundle == null) {
            updateLanguage();
        }

        try {
            double distance = Double.parseDouble(distanceField.getText());
            double fuel = Double.parseDouble(fuelField.getText());
            double consumption = (fuel / distance) * 100;
            String consumptionText = bundle.getString("result") + ": " + String.format("%.2f", consumption) + " L/100km";
            resultLabel.setText(consumptionText);

            // Store only the calculated result in the database
            storeCalculationResult(consumptionText);

        } catch (NumberFormatException e) {
            showAlert(bundle.getString("error"), bundle.getString("error.invalidNumber"));
        }
    }

    private void storeCalculationResult(String resultText) {
        String languageCode = "en"; // default to English
        String countryCode = "UK";  // default to UK

        // Set language and country codes based on selected language
        String selectedLanguage = languageComboBox.getValue();
        switch (selectedLanguage) {
            case "French":
                languageCode = "fr";
                countryCode = "FR";
                break;
            case "Japanese":
                languageCode = "ja";
                countryCode = "JP";
                break;
            case "Farsi":
                languageCode = "fa";
                countryCode = "IR";
                break;
        }

        // Query for inserting into fuel_calculations table
        String insertFuelCalculationQuery =
                "INSERT INTO fuel_calculations (distance, fuel, consumption, calculation_date) VALUES (?, ?, ?, NOW())";

        // Query for inserting into localized_messages table without updating existing entries
        String insertLocalizedMessageQuery =
                "INSERT IGNORE INTO localized_messages (language_code, country_code, message_key, message_text) " +
                        "VALUES (?, ?, 'calculation_result', ?)";

        try (Connection connection = DatabaseUtil.getConnection()) {
            // Insert into fuel_calculations
            try (PreparedStatement stmt = connection.prepareStatement(insertFuelCalculationQuery)) {
                double distance = Double.parseDouble(distanceField.getText());
                double fuel = Double.parseDouble(fuelField.getText());
                double consumption = (fuel / distance) * 100;

                stmt.setDouble(1, distance);
                stmt.setDouble(2, fuel);
                stmt.setDouble(3, consumption);
                stmt.executeUpdate();
            }

            // Insert into localized_messages if not already present
            try (PreparedStatement stmt = connection.prepareStatement(insertLocalizedMessageQuery)) {
                stmt.setString(1, languageCode);
                stmt.setString(2, countryCode);
                stmt.setString(3, resultText);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(bundle.getString("error"), "Failed to save calculation result to the database.");
        }
    }


    @FXML
    private void onResetButtonClick() {
        distanceField.clear();
        fuelField.clear();
        resultLabel.setText("");
        languageComboBox.setValue("English");
        updateLanguage();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
