# Tests MySQL jetables — preparation uniquement

Aucun test MySQL ni conteneur n'a ete execute pendant la preparation.
Ne pas lancer ces commandes sans autorisation explicite.

## Conditions avant la future execution

- Docker operationnel avec conteneurs Linux ; aucune installation automatique.
- Image MySQL 8.4.7 disponible (version a comparer avec le serveur cible sans contacter la base personnelle).
- Branche integration/backend-eventcampus ; Java 25 et Maven.
- Ne jamais utiliser WAMP pour ces tests. Aucun volume ni fichier personnel n'est monte.
- Le profil mysql-it active uniquement les trois classes restantes et MysqlPersistenceIT.
- Une execution sans ce profil echoue avant Docker/JDBC ; elle n'utilise jamais une base de secours.

## Future commande autorisee separement

Depuis backend, apres validation uniquement :

    mvn -Pmysql-it -Dtest=BackendApplicationTests,AdminDashboardTests,EventApiTests,MysqlPersistenceIT test

Il ne faut pas ajouter de parametres datasource, imports Spring ou configuration locale.
Ne jamais executer database/schema.sql : il cible eventcampus.
Les deux scripts db/mysql-it-*.sql sont charges uniquement via la datasource du conteneur.

## Isolation

La configuration charge exclusivement application-mysql-it.properties ; tout import ou emplacement
supplementaire et toute source application.properties sont refuses par l'initialiseur avant Docker/JDBC.
L'URL par defaut est non reseau et son pilote inexistant. Elle n'est jamais utilisee pour une connexion.

Le conteneur GenericContainer est neuf, non reutilise, sans montage, avec port dynamique (3306 cote hote refuse),
base eventcampus_it, utilisateur restreint a cette base et mots de passe aleatoires d'execution.
Il utilise une attente TCP, sans sonde JDBC Testcontainers. Les connexions SQL passent par GuardedDataSource.
Avant CHAQUE connexion : controle d'activation, identite du conteneur vivant, hote, port et URL construite
uniquement pour cette instance. Aucun identifiant fourni par l'appelant n'est accepte.
La meme datasource est injectee dans JPA, JdbcTemplate et les trois verrous GET_LOCK.
La configuration Spring ne peut pas rediriger cette datasource.

Le bean QrTokenCodec de production est remplace avant instanciation par @TestBean, avec une cle aleatoire
32 octets dans un repertoire temporaire. Aucune lecture de la cle personnelle ou de ses permissions.
Conteneur et cle sont nettoyes a l'arret normal de la JVM ; un arret force peut laisser des ressources
jetables a nettoyer manuellement, apres verification de leur identite.

## Couverture preparee

- BackendApplicationTests : contexte complet avec vrais services/repositories et datasource jetable.
- AdminDashboardTests : donnees fictives, permissions et trois verrous MySQL concurrents.
- EventApiTests : catalogue public et statistiques sur donnees fictives.
- MysqlPersistenceIT (5 scenarios) : inscription BCrypt/connexion/created_at/unicite email ;
  annulation/reinscription/revocation QR ; QR demain consultable mais presence refusee ;
  derniere place concurrente ; doubles scans et unicites inscriptions/QR/presences.

Les 9 tests existants ne sont pas affaiblis. Les 5 nouveaux tests n'utilisent pas @Transactional :
leurs threads concurrents doivent voir des transactions effectivement validees.
Les adresses sont fictives en example.invalid. Les comptes de fixtures servent aux scenarios ;
les comptes de connexion et mots de passe sont generes par test. Aucun compte personnel n'est copie.

## Limites avant publication

Compilation seulement : le demarrage Docker, l'initialisation SQL, les gardes a l'execution et les 14 tests
MySQL restent a verifier. Les 84 tests sans MySQL n'ont pas ete relances pendant cette preparation.
Le schema jetable utilise InnoDB : aucune conclusion sur MyISAM sans campagne separee sur conteneur.
Les trois tests historiques de catalogue/tableau de bord simulent la fraicheur des sessions ; les tests
complets de cookies, deconnexion, changement de role et comptes desactives restent a completer.
Les connexions SQL et transactions des services existent reellement dans ces tests.
Les limites autour de minuit et du changement d'heure de Paris restent a couvrir avec horloges controlees.
Aucun serveur HTTP n'est lance : SpringBootTest utilise son contexte web simule, sans port 8080.

Aucun commit ou push n'est effectue par la preparation ou par les tests.