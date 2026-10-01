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

    // Adresse de la dernière release publiée sur GitHub
    private static final String GITHUB_RELEASES_URL =
        "https://api.github.com/repos/userLinpy/Frontend-todolist/releases/latest";

    private static final String BASE_URL = "https://backend-todolist-pi3p.onrender.com/api";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void reveillerServeur() {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/health")).GET().build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    // RÉCUPÉRER LA DERNIÈRE VERSION DISPONIBLE
    // Renvoie {"version": "1.5", "url": "lien du .exe ou du .dmg"}, ou null en cas de problème
    public Map<String, String> getDerniereVersion() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_RELEASES_URL))
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response.body());
            String version = root.get("tag_name").asText().replaceFirst("^v", ""); // "v1.5" -> "1.5"

            // On cherche le fichier adapté au système : .dmg sur Mac, .exe sur Windows
            String extension = System.getProperty("os.name").toLowerCase().contains("mac") ? ".dmg" : ".exe";

            for (com.fasterxml.jackson.databind.JsonNode asset : root.get("assets")) {
                if (asset.get("name").asText().endsWith(extension)) {
                    Map<String, String> info = new HashMap<>();
                    info.put("version", version);
                    info.put("url", asset.get("browser_download_url").asText());
                    return info;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // TÉLÉCHARGER LE FICHIER DE MISE À JOUR DANS LE DOSSIER TEMPORAIRE
    // Renvoie le chemin du fichier téléchargé, ou null en cas d'échec
    public java.nio.file.Path telechargerMiseAJour(String url) {
        try {
            String nomFichier = url.substring(url.lastIndexOf('/') + 1);
            java.nio.file.Path destination = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), nomFichier);
    
            // GitHub redirige les téléchargements : il faut un client qui suit les redirections
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
    
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
    
            HttpResponse<java.nio.file.Path> response = client.send(request,
                    HttpResponse.BodyHandlers.ofFile(destination,
                            java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.WRITE,
                            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING));
    
            return response.statusCode() == 200 ? destination : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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