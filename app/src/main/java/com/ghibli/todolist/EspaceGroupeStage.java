package com.ghibli.todolist;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class EspaceGroupeStage extends Stage {

    private final ApiService apiService;
    private final String emailUtilisateur;
    private final java.util.function.Consumer<Long> onOuvrirGroupe; // Pour envoyer l'ID à l'App principale
    private ComboBox<String> comboMembres = new ComboBox<>();

    // Petite classe interne pour afficher joliment le groupe dans la liste
    private static class GroupeItem {
        Long id; String nom; String code;
        GroupeItem(Long id, String nom, String code) { this.id = id; this.nom = nom; this.code = code; }
        @Override public String toString() { return nom + " (Code Secret : " + code + ")"; }
    }

    private ListView<GroupeItem> listeGroupes;

    public EspaceGroupeStage(ApiService apiService, String emailUtilisateur, java.util.function.Consumer<Long> onOuvrirGroupe) {
        this.apiService = apiService;
        this.emailUtilisateur = emailUtilisateur;
        this.onOuvrirGroupe = onOuvrirGroupe;

        this.setTitle("Mes Espaces Collectifs");
        this.initModality(Modality.APPLICATION_MODAL); 

        comboMembres.setPromptText("Membres du groupe");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        Text titre = new Text("Mes Groupes");
        titre.setStyle("-font-size: 18px; -fx-font-weight: bold;");

        listeGroupes = new ListView<>();
        listeGroupes.setPrefHeight(200);

        listeGroupes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                chargerMembres(newVal.id); // On charge les membres du groupe sélectionné
            }
        });

        // Bouton pour ouvrir le groupe sélectionné
        Button btnOuvrir = new Button("Ouvrir l'espace sélectionné");
        btnOuvrir.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-font-weight: bold;");
        btnOuvrir.setOnAction(e -> {
            GroupeItem selection = listeGroupes.getSelectionModel().getSelectedItem();
            if (selection != null) {
                onOuvrirGroupe.accept(selection.id); // Transmet l'ID à App.java
                this.close(); // Ferme la popup
            }
        });

        HBox actionBox = new HBox(15, btnOuvrir);
        actionBox.setAlignment(Pos.CENTER);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button btnCreer = new Button("Créer un groupe");
        btnCreer.setStyle("-fx-background-color: #2d6a4f; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCreer.setOnAction(e -> ouvrirPopupCreation());

        Button btnRejoindre = new Button("Rejoindre un groupe");
        btnRejoindre.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white; -fx-font-weight: bold;");
        btnRejoindre.setOnAction(e -> ouvrirPopupJointure());

        buttonBox.getChildren().addAll(btnCreer, btnRejoindre);

        layout.getChildren().addAll(titre, listeGroupes, actionBox, buttonBox, comboMembres);

        Scene scene = new Scene(layout, 400, 400);
        this.setScene(scene);

        chargerListeGroupes(); // Charge les groupes à l'ouverture
    }

    private void chargerMembres(Long groupeId) {
        // Appelle le service pour récupérer la liste des membres
        List<String> membres = apiService.getMembresGroupe(groupeId);
        comboMembres.getItems().setAll(membres);
    }

    private void chargerListeGroupes() {
        listeGroupes.getItems().clear();
        List<Map<String, String>> groupes = apiService.getMesGroupes(emailUtilisateur);
        for (Map<String, String> g : groupes) {
            listeGroupes.getItems().add(new GroupeItem(
                    Long.parseLong(g.get("id")), g.get("nom"), g.get("code")
            ));
        }
    }

    private void ouvrirPopupCreation() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nouveau Groupe");
        dialog.setHeaderText("Créer un espace collectif");
        dialog.setContentText("Nom du groupe :");

        dialog.showAndWait().ifPresent(nom -> {
            if (!nom.trim().isEmpty()) {
                if (apiService.creerGroupe(nom, emailUtilisateur)) {
                    new Alert(Alert.AlertType.INFORMATION, "Groupe créé avec succès !").show();
                    chargerListeGroupes(); // Rafraîchit la liste
                } else {
                    new Alert(Alert.AlertType.ERROR, "Erreur lors de la création du groupe.").show();
                }
            }
        });
    }

    private void ouvrirPopupJointure() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rejoindre un Groupe");
        dialog.setHeaderText("Rejoindre un espace collectif existant");
        dialog.setContentText("Entrez le code secret à 6 lettres :");

        dialog.showAndWait().ifPresent(code -> {
            if (!code.trim().isEmpty()) {
                if (apiService.rejoindreGroupe(code, emailUtilisateur)) {
                    new Alert(Alert.AlertType.INFORMATION, "Félicitations, vous avez rejoint le groupe !").show();
                    chargerListeGroupes(); // Rafraîchit la liste
                } else {
                    new Alert(Alert.AlertType.ERROR, "Code invalide ou erreur de connexion.").show();
                }
            }
        });
    }
}