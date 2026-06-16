module com.ghibli.todolist {
    // On ajoute 'transitive' pour que les types de JavaFX (comme Stage) 
    // soient accessibles partout dans l'application.
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires javafx.base;

    // On ajoute l'accès à la bibliothèque HttpClient native de Java
    requires java.net.http;

    // On indique qu'on a besoin de Jackson pour le JSON
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    // Nécessaire pour que JavaFX ET Jackson puissent lire et remplir les attributs de la classe Tache
    opens com.ghibli.todolist to javafx.fxml, javafx.base, javafx.graphics, com.fasterxml.jackson.databind;

    exports com.ghibli.todolist;
}