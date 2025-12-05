# Bucket Adapter Repository

## Run l'application

### Maven

À partir de la racine du projet, exécutez `./mvnw spring-boot:run` pour démarrer le wrapper Maven et lancer l'application.

### Docker

À partir de la racine du projet, exécutez `docker compose up --build` pour build et démarrer le container de l'application.

## Tests

Exécutez `./mvnw test` pour exécuter les tests unitaires et d'intégration disponibles


### Fonctionnement de l'application

Vous trouverez des curl samples dans le [Controller](/src/main/java/com/example/bucketadapter/controllers/BucketController.java)

## Documentation Doxygen

### Générer la documentation Doxygen

```bash
doxygen Doxyfile
```

Cela génère un dossier `html` dans `/docs` .

### Visualiser la documentation Doxygen

Lancer un serveur PHP pour voir la documentation Doxygen :

```bash
php -S localhost:8000 -t docs/html
```

Ouvrir dans le navigateur : http://localhost:8000