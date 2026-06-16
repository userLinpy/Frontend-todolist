package com.ghibli.todolist;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.net.http.HttpResponse;

public class LoginStage extends Stage {

    private final ApiService apiService;
    private final Runnable onSuccess;
    private String userEmail;

    public LoginStage(ApiService apiService, Runnable onSuccess) {
        this.apiService = apiService;
        this.onSuccess = onSuccess;

        this.setTitle("Totoro ToDoList - Connexion");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("login-tab-pane");

        Tab tabConnexion = new Tab("Se Connecter", creerEcranConnexion());
        Tab tabInscription = new Tab("S'Inscrire", creerEcranInscription());

        tabConnexion.setClosable(false);
        tabInscription.setClosable(false);

        tabPane.getTabs().addAll(tabConnexion, tabInscription);

        Scene scene = new Scene(tabPane, 440, 440);
        
        if (getClass().getResource("/login.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/login.css").toExternalForm());
        }
        
        this.setScene(scene);
    }

    // ÉCRAN DE CONNEXION (Par Email)
    private VBox creerEcranConnexion() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Text titre = new Text("Bienvenue dans l'aventure !");
        titre.setStyle("-font-size: 20px; -fx-font-weight: bold; -fx-fill: #2e4a62;");

        // Utilisation de l'email comme identifiant
        TextField emailField = new TextField();
        emailField.setPromptText("Adresse Email");
        emailField.setMaxWidth(280);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.setMaxWidth(280);

        Button loginBtn = new Button("Entrer dans notre univers");
        loginBtn.getStyleClass().add("login-btn");
        loginBtn.setMaxWidth(280);

        Hyperlink mdpOublieLink = new Hyperlink("Mot de passe oublié ?");
        mdpOublieLink.setOnAction(e -> ouvrirFenetreMotDePasseOublie());

        loginBtn.setOnAction(event -> {
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                afficherAlerte(Alert.AlertType.WARNING, "Champs vides", "S'il te plaît, écris ton e-mail et ton mot de passe !");
                return;
            }

            HttpResponse<String> reponse = apiService.connecter(email, password);
            if (reponse.statusCode() == 200) {
                try {
                    // On utilise l'ObjectMapper de Jackson pour lire le texte JSON reçu
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(reponse.body());

                    App.emailUtilisateurConnecte = root.get("email").asText(); // On sauvegarde l'email des utilisateur pour les groupes

                    // On extrait le tableau de données "tableaux" de l'utilisateur
                    com.fasterxml.jackson.databind.JsonNode tableauxNode = root.get("tableaux");
                    if (tableauxNode != null && tableauxNode.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode tab : tableauxNode) {
                            // Si on trouve le tableau dont le type est "PERSONNEL"
                            if ("PERSONNEL".equals(tab.get("type").asText())) {
                                // On retient son ID dans la variable globale de votre App
                                App.idTableauPersonnel = tab.get("id").asLong();
                                App.idTableauActif = App.idTableauPersonnel; // On définit le tableau actif par défaut !
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erreur d'extraction de l'ID du tableau : " + e.getMessage());
                }

                Platform.runLater(onSuccess);
                this.close();

            } else if (reponse != null && reponse.statusCode() == 403) {
                // Gestion du compte non activé
                afficherAlerte(Alert.AlertType.ERROR, "Compte non activé", "Votre compte n'est pas encore activé. Veuillez vérifier vos e-mails pour entrer le code de validation.");
                ouvrirFenetreValidationCompte();
            } else {
                String messageErreur = (reponse != null) ? reponse.body() : "Le serveur ne répond pas.";
                afficherAlerte(Alert.AlertType.ERROR, "Accès refusé", messageErreur);
            }
        });

        layout.getChildren().addAll(titre, emailField, passwordField, loginBtn, mdpOublieLink);
        return layout;
    }

    // ÉCRAN D'INSCRIPTION
    private VBox creerEcranInscription() {
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.CENTER);

        Text titre = new Text("Créer un nouveau compte");
        titre.setStyle("-font-size: 18px; -fx-font-weight: bold; -fx-fill: #2e4a62;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Choisissez un Pseudo");
        usernameField.setMaxWidth(280);

        TextField emailField = new TextField();
        emailField.setPromptText("Votre adresse Email (Unique)");
        emailField.setMaxWidth(280);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe (Min. 5 caractères)");
        passwordField.setMaxWidth(280);

        Button registerBtn = new Button("Créer mon compte");
        registerBtn.getStyleClass().add("register-btn");
        registerBtn.setMaxWidth(280);

        registerBtn.setOnAction(event -> {
            String username = usernameField.getText().trim();
            this.userEmail = emailField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || userEmail.isEmpty() || password.isEmpty()) {
                afficherAlerte(Alert.AlertType.WARNING, "Champs vides", "Remplissez tous les champs pour vous inscrire !");
                return;
            }

            // Validation de la contrainte locale des 5 caractères minimum
            if (password.length() < 5) {
                afficherAlerte(Alert.AlertType.WARNING, "Mot de passe trop court", "Le mot de passe doit contenir au moins 5 caractères !");
                return;
            }

            HttpResponse<String> reponse = apiService.inscrire(username, password, userEmail);
            if (reponse != null && reponse.statusCode() == 200) {
                afficherAlerte(Alert.AlertType.INFORMATION, "Inscription Réussie", 
                        "Votre compte a été pré-créé ! Un code de vérification a été envoyé à l'adresse : " + userEmail);
                
                // On ouvre directement la fenêtre pour taper le code reçu par e-mail
                ouvrirFenetreValidationCompte();
            } else {
                String messageErreur = (reponse != null) ? reponse.body() : "Le serveur ne répond pas.";
                afficherAlerte(Alert.AlertType.ERROR, "Échec de l'inscription", messageErreur);
            }
        });

        layout.getChildren().addAll(titre, usernameField, emailField, passwordField, registerBtn);
        return layout;
    }

    // FENÊTRE POPUP : Saisir le code de validation reçu après inscription
    private void ouvrirFenetreValidationCompte() {
        Stage validationStage = new Stage();
        validationStage.setTitle("Validation du compte");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.CENTER);

        Text instructions = new Text("Entrez le code d'activation reçu par mail :");
        instructions.setStyle("-fx-font-weight: bold;");

        TextField codeField = new TextField();
        codeField.setPromptText("Ex: A1B2C3");
        codeField.setMaxWidth(200);
        codeField.setAlignment(Pos.CENTER);

        Button validerBtn = new Button("Activer mon compte");
        validerBtn.setMaxWidth(200);

        validerBtn.setOnAction(e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                afficherAlerte(Alert.AlertType.WARNING, "Erreur", "Veuillez entrer le code.");
                return;
            }

            boolean success = apiService.validerCompte(userEmail, code);
            if (success) {
                afficherAlerte(Alert.AlertType.INFORMATION, "Compte activé !", "Félicitations, ton compte est actif. Tu peux maintenant te connecter !");
                validationStage.close();
            } else {
                afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Code incorrect ou expiré.");
            }
        });

        layout.getChildren().addAll(instructions, codeField, validerBtn);
        Scene scene = new Scene(layout, 320, 220);
        if (getClass().getResource("/login.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/login.css").toExternalForm());
        }
        validationStage.setScene(scene);
        validationStage.show();
    }

    // FENÊTRE POPUP : Mot de passe oublié (Reste fonctionnelle et inchangée)
    private void ouvrirFenetreMotDePasseOublie() {
        Stage resetStage = new Stage();
        resetStage.setTitle("Réinitialiser le mot de passe");

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        Text instructions = new Text("1. Entrez votre e-mail pour recevoir un code :");
        TextField emailField = new TextField();
        emailField.setPromptText("Votre adresse e-mail");
        emailField.setMaxWidth(250);

        Button envoyerBtn = new Button("Demander un code");
        envoyerBtn.setMaxWidth(250);

        Text labelToken = new Text("2. Saisissez le code et votre nouveau mot de passe :");
        TextField tokenField = new TextField();
        tokenField.setPromptText("Code secret reçu");
        tokenField.setMaxWidth(250);

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nouveau mot de passe (Min. 5 caractères)");
        newPasswordField.setMaxWidth(250);

        Button validerBtn = new Button("Changer le mot de passe");
        validerBtn.setMaxWidth(250);

        envoyerBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            if (email.isEmpty()) {
                afficherAlerte(Alert.AlertType.WARNING, "Erreur", "Entre ton adresse e-mail.");
                return;
            }
            boolean envoye = apiService.demanderReinitialisation(email);
            if (envoye) {
                afficherAlerte(Alert.AlertType.INFORMATION, "Code envoyé", "Regarde tes e-mails (ou la console du serveur) pour obtenir ton code !");
            } else {
                afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Aucun compte n'existe avec cet e-mail.");
            }
        });

        validerBtn.setOnAction(e -> {
            String token = tokenField.getText().trim();
            String newPass = newPasswordField.getText().trim();

            if (token.isEmpty() || newPass.isEmpty()) {
                afficherAlerte(Alert.AlertType.WARNING, "Erreur", "Tous les champs doivent être remplis.");
                return;
            }

            if (newPass.length() < 5) {
                afficherAlerte(Alert.AlertType.WARNING, "Mot de passe trop court", "Le nouveau mot de passe doit faire au moins 5 caractères !");
                return;
            }

            boolean reussite = apiService.validerReinitialisation(token, newPass);
            if (reussite) {
                afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "Ton mot de passe a été modifié avec succès ! Tu peux maintenant te connecter.");
                resetStage.close();
            } else {
                afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Code incorrect, expiré ou invalide.");
            }
        });

        layout.getChildren().addAll(instructions, emailField, envoyerBtn, labelToken, tokenField, newPasswordField, validerBtn);

        Scene scene = new Scene(layout, 360, 460);
        if (getClass().getResource("/login.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/login.css").toExternalForm());
        }
        resetStage.setScene(scene);
        resetStage.show();
    }

    private void afficherAlerte(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}