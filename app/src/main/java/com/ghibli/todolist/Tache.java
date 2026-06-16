package com.ghibli.todolist;

import java.time.LocalDate;
// l'annotation de sécurité de Jackson
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) 
public class Tache {
    private Long id; 
    private String titre; 
    private String description;
    private LocalDate dateCreation;
    private LocalDate dateFinTache;
    private String priorite;    
    private double avancement;
    private boolean terminee; 

    // Constructeur vide obligatoire pour la traduction JSON
    public Tache() {
        this.dateCreation = LocalDate.now();
    }

    public Tache(String titre, String description, LocalDate dateFinTache, String priorite) {
        this.titre = titre;
        this.description = description;
        this.dateCreation = LocalDate.now();
        this.dateFinTache = dateFinTache;
        this.priorite = priorite;
        this.avancement = 0.0;
        this.terminee = false;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitre(){ return titre;}
    public void setTitre(String titre) { this.titre = titre;}

    public String getDescription(){ return description;}
    public void setDescription(String description) { this.description = description;}

    public LocalDate getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDate date) { this.dateCreation = date; }

    public LocalDate getDateFinTache() { return dateFinTache;}
    public void setDateFinTache(LocalDate date2) { this.dateFinTache = date2;}

    public String getPriorite() { return priorite;}
    public void setPriorite(String priorite_new) { this.priorite = priorite_new;}
    
    public double getAvancement() { return avancement;}
    public void setAvancement(double avanc) { this.avancement = avanc;} 

    public boolean getTerminee() { 
        return avancement == 100.0;
    }
    public void setTerminee(boolean bool) { this.terminee = bool;}
}