package com.ghibli.todolist;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiService {

    private static final String BASE_URL = "https://backend-todolist-pi3p.onrender.com/api";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // MÉTHODE POUR LA CONNEXION (LOGIN) 
    
    public HttpResponse<String> connecter(String username, String password) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("password", password);
            
            // Jackson génère un JSON propre ({"username":"...","password":"..."})
            String jsonPayload = objectMapper.writeValueAsString(payload);
        
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/connexion"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
        
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // MÉTHODE POUR L'INSCRIPTION 
    public HttpResponse<String> inscrire(String username, String password, String email) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("password", password);
            payload.put("email", email);

            String jsonPayload = objectMapper.writeValueAsString(payload); // Jackson génère un JSON propre sans aucun '\'

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/inscription"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // DEMANDE DE RÉINITIALISATION (MOT DE PASSE OUBLIÉ) - CORRIGÉE
    public boolean demanderReinitialisation(String email) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("email", email);
            
            String jsonPayload = objectMapper.writeValueAsString(payload);
        
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/mot-de-passe-oublie"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
        
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // VALIDER LE CHANGEMENT DE MOT DE PASSE AVEC JETON - CORRIGÉE
    public boolean validerReinitialisation(String token, String nouveauMotDePasse) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("token", token);
            payload.put("nouveauMotDePasse", nouveauMotDePasse);
            
            String jsonPayload = objectMapper.writeValueAsString(payload);
        
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/reinitialiser-mot-de-passe"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
        
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // RÉCUPÉRER LES TÂCHES D'UN TABLEAU (Reste inchangée et propre)
    public List<Tache> getTachesParTableau(Long tableauId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/tableau/" + tableauId + "/taches"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<Tache>>(){});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public boolean validerCompte(String email, String code) {
        try {
            // Le serveur attend probablement un JSON avec le code
            Map<String, String> payload = new HashMap<>();
            payload.put("email", email); 
            payload.put("code", code);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/valider-compte"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Retourne true si le serveur renvoie 200 (OK)
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Tache creerTache(Long tableauId, Tache tache) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(tache);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/tableau/" + tableauId + "/tache"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), Tache.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // CRÉER UN GROUPE COLLECTIF
    public boolean creerGroupe(String nomGroupe, String emailCreateur) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("nom", nomGroupe);
            payload.put("emailCreateur", emailCreateur);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/tableaux/creer"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // REJOINDRE UN GROUPE EXISTANT
    public boolean rejoindreGroupe(String codeGroupe, String emailUtilisateur) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("code", codeGroupe);
            payload.put("email", emailUtilisateur);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/tableaux/rejoindre"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // RÉCUPÉRER MES GROUPES COLLECTIFS
    public List<Map<String, String>> getMesGroupes(String email) {
        List<Map<String, String>> groupes = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/tableaux/mes-groupes/" + email))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
                    
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response.body());
                if (root.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode node : root) {
                        Map<String, String> map = new HashMap<>();
                        map.put("id", node.get("id").asText());
                        map.put("nom", node.get("nom").asText());
                        // On sécurise l'extraction du codeGroupe s'il est null
                        map.put("code", node.has("codeGroupe") && !node.get("codeGroupe").isNull() ? node.get("codeGroupe").asText() : "");
                        groupes.add(map);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return groupes;
    }

    // CHANGER LE PSEUDO
    public boolean changerPseudo(String email, String nouveauPseudo) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("email", email);
            payload.put("pseudo", nouveauPseudo);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/pseudo"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload)) // Attention, c'est un PUT !
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // MODIFIER UNE TÂCHE EXISTANTE
    public boolean modifierTache(Long tacheId, Tache tache) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(tache);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/tache/" + tacheId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // SUPPRIMER UNE TÂCHE
    public boolean supprimerTache(Long tacheId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/tache/" + tacheId))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // RÉCUPÉRER LES MEMBRES D'UN GROUPE
    public List<String> getMembresGroupe(Long tableauId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/tableaux/" + tableauId + "/membres"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<String>>(){});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}