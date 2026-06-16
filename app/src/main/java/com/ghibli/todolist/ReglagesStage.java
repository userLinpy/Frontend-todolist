package com.ghibli.todolist;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ReglagesStage extends Stage {

    public ReglagesStage(ApiService apiService, Runnable onThemeChanged) {
        this.setTitle("⚙️ Réglages de l'application");
        this.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.CENTER);

        Text titre = new Text("Personnalisation");
        titre.setStyle("-font-size: 20px; -fx-font-weight: bold;");

        // --- SECTION PSEUDO ---
        Label lblPseudo = new Label("Changer mon pseudo :");
        TextField fieldPseudo = new TextField();
        fieldPseudo.setPromptText("Nouveau pseudo");
        
        Button btnSavePseudo = new Button("Mettre à jour le pseudo");
        btnSavePseudo.setOnAction(e -> {
            if (!fieldPseudo.getText().trim().isEmpty()) {
                boolean ok = apiService.changerPseudo(App.emailUtilisateurConnecte, fieldPseudo.getText().trim());
                if (ok) {
                    new Alert(Alert.AlertType.INFORMATION, "Pseudo mis à jour !").show();
                    fieldPseudo.clear();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Erreur de mise à jour.").show();
                }
            }
        });

        // SECTION THÈME 
        Label lblTheme = new Label("Changer de Thème :");
        ComboBox<App.ThemeDef> comboThemes = new ComboBox<>();
        comboThemes.getItems().addAll(App.THEMES_DISPOS);
        comboThemes.setValue(App.themeActif); // Thème actuel par défaut

        Button btnSaveTheme = new Button("Appliquer le thème");
        btnSaveTheme.setOnAction(e -> {
            App.themeActif = comboThemes.getValue();
            onThemeChanged.run(); // Déclenche le changement visuel dans App.java
            this.close();
        });

        layout.getChildren().addAll(titre, new Separator(), lblPseudo, fieldPseudo, btnSavePseudo, new Separator(), lblTheme, comboThemes, btnSaveTheme);

        Scene scene = new Scene(layout, 350, 400);
        this.setScene(scene);
    }
}