# EventCampus : base fictive de test

Ce jeu de données est construit depuis `schema.sql` et les entités Java, sans
export de base réelle. Il crée **eventcampus_test**, avec 13 tables neuves en
InnoDB, 3 rôles, 5 catégories, 3 comptes actifs et 4 événements fictifs.
Aucune conversion de moteur, suppression ou modification de table existante.
Les autres tables restent vides, notamment les inscriptions et les QR codes.

## Importer dans phpMyAdmin

1. Démarrer Apache et MySQL dans WAMP, puis ouvrir phpMyAdmin.
2. Vérifier que **eventcampus_test n'existe pas**, même vide. Ne pas la créer
   manuellement et ne pas sélectionner la base habituelle `eventcampus`.
3. Depuis l'accueil du serveur, ouvrir **Importer**, sélectionner
   `eventcampus_test.sql`, conserver le format SQL et l'encodage UTF-8,
   puis lancer l'import complet. Le compte MySQL local doit pouvoir créer une base.
4. Vérifier la présence de `eventcampus_test`, de ses 13 tables et des trois
   comptes dans `users`. Aucun mot de passe n'y est stocké en clair.

Le fichier n'utilise aucun `USE` et qualifie toutes ses tables et références par
`eventcampus_test`. La création de base ne comporte pas `IF NOT EXISTS` : si
elle existe déjà, une erreur est attendue. Chaque création de table et insertion
est aussi conditionnée à la création réussie de la base par la même session
(absence initiale et résultat de `CREATE DATABASE`). Si l'import poursuit après
cette erreur, ces opérations sont remplacées par un message sans écriture.
Importer le fichier entier dans une seule session ; ne pas exécuter des extraits.

En cas d'erreur, arrêter et lire le message. Ne pas relancer sur une base
partiellement importée : le script refuse également ce cas. Il ne réinitialise
jamais une base. Les commandes de création MySQL ne sont pas annulables par une
simple transaction ; un import interrompu peut laisser une base de test incomplète.

## Connecter le backend local à la base de test

Prérequis : Java 25 et les dépendances Maven du projet. Arrêter tout backend
déjà lancé sur le port 8080 avant de le relancer. Dans un terminal PowerShell
local, définir les variables **avant** de démarrer Spring Boot :

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/eventcampus_test?useSSL=false&serverTimezone=Europe/Paris&allowPublicKeyRetrieval=true'
$env:SPRING_DATASOURCE_USERNAME = '<utilisateur MySQL local>'
$env:SPRING_DATASOURCE_PASSWORD = '<mot de passe MySQL local>'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'none'
Set-Location backend
.\mvnw.cmd spring-boot:run
```

Remplacer les deux valeurs entre chevrons par sa propre configuration MySQL
(chaîne vide si son mot de passe local est vide). Ce ne sont pas les comptes
applicatifs du tableau ci-dessous. Ces variables restent locales au terminal ;
ne pas les enregistrer dans Git. Si Maven est déjà installé, `mvn spring-boot:run`
peut remplacer le wrapper. Vérifier dans les logs que l'URL JDBC cible bien
**eventcampus_test** avant toute connexion depuis le frontend.

Ne pas modifier `application.properties` pour partager ses identifiants locaux.
Après les essais, arrêter le backend et fermer ce terminal pour abandonner ces
variables temporaires. Le fichier de configuration du projet n'est pas modifié.

## Comptes de démonstration publics

| Rôle | Adresse fictive | Mot de passe de test |
| --- | --- | --- |
| ADMIN | admin@eventcampus.test | CampusTest-Admin-2026! |
| MODERATOR | moderator@eventcampus.test | CampusTest-Moderator-2026! |
| USER | user@eventcampus.test | CampusTest-User-2026! |

Ces identifiants sont volontairement publics et réservés aux tests locaux.
Ne pas les réutiliser sur un service réel ni exposer cette installation sur Internet.
Les hachages ont été générés avec `BCryptPasswordEncoder` du projet (coût 10),
avec un sel distinct par compte, et vérifiés hors base. Le statut `ACTIVE` est
nécessaire à la connexion. Les rôles sont liés par `users.id_role` à `roles`.

## Vérifier ADMIN et MODERATOR

1. Ouvrir le frontend via Apache, par exemple
   `http://localhost/EventCampus/frontend/pages/connexion.html` si le dossier
   du projet s'appelle EventCampus dans le répertoire web de WAMP.
   Ne pas ouvrir le fichier directement en `file://`. Utiliser `localhost`
   de façon cohérente pour le frontend et le backend.
2. Se connecter avec ADMIN : redirection vers `espace-admin.html`.
   Le tableau de bord doit afficher 3 comptes, 5 catégories et 4 événements,
   dont 2 en attente et 1 publié. Vérifier la gestion des comptes et catégories.
   Pour tester une désactivation, utiliser USER, puis le réactiver.
   La protection du dernier administrateur actif est normale.
3. Se déconnecter, puis se connecter avec MODERATOR : redirection vers
   `espace-moderator.html`. Approuver « Demo - Atelier a approuver », puis
   refuser « Demo - Rencontre a refuser ». La liste d'attente doit se vider
   et les compteurs se mettre à jour. Ces essais modifient uniquement la base
   de test à laquelle le backend a été connecté.
4. Facultatif : se connecter avec USER, consulter les événements publiés et
   tester une inscription ou une proposition. Les événements sont datés à
   30, 31, 45 et 46 jours après la date d'import.

Les rôles ont des accès distincts : ADMIN n'accède pas automatiquement aux API
MODERATOR, et inversement. Utiliser la déconnexion ou des profils de navigateur
distincts pour changer de compte : l'authentification utilise une session HTTP.
Une connexion refusée avec un compte désactivé ou un mauvais mot de passe est normale.

Le test des QR codes exige une clé locale de 32 octets configurée séparément
via `EVENTCAMPUS_QR_KEY_FILE` ou l'emplacement local prévu par `QrTokenCodec`.
Aucune clé ni aucun jeton QR n'est fourni ici. Une clé absente peut empêcher
la génération des QR codes sans empêcher les connexions ADMIN et MODERATOR.

## Vérifications avant partage

Partager uniquement ce README et `eventcampus_test.sql`, après validation.
Ils contiennent des données fictives, des adresses sous `.test` et les seuls
identifiants de démonstration ci-dessus : aucun export de comptes réels,
identifiant MySQL réel, fichier de clé ou secret de session.

La vérification locale porte sur le schéma, les références, les gardes d'écriture
et les hachages BCrypt. **Aucun import ni test de connexion MySQL n'a été exécuté
lors de la préparation** ; l'import et les essais applicatifs restent à effectuer
sur l'installation de test de chaque coéquipier.
