# Rapport d'audit de sécurité frontend

## Périmètre

Audit statique du frontend `frontend/` d'EventCampus, selon les points OWASP
indiqués dans la demande. Aucun fichier frontend ni backend n'a été modifié
pendant l'audit. Les observations ci-dessous ne constituent pas des
vulnérabilités confirmées.

## Résumé

Aucune vulnérabilité exploitable n'a été confirmée dans le frontend examiné.
Les risques de protection CSRF et de validation/anti-rejeu des QR codes
dépendent de contrôles côté serveur et restent à vérifier avec le développeur
backend.

La confiance indique la certitude sur le comportement observé dans le code,
pas sur l'exploitabilité.

| # | Severity | File | Lines | Vulnerability | Confidence |
|---|----------|------|-------|---------------|------------|
| 1 | 🟡 MEDIUM | [frontend/js/inscription.js](./frontend/js/inscription.js) | 55 | Observation non confirmée : le POST d'inscription cible `http://localhost:8080`. En déploiement, cela vise la machine du visiteur; si un service local répond, le mot de passe pourrait être transmis en HTTP. | 10/10 |
| 2 | 🟡 MEDIUM | [frontend/js/event-registrations.js](./frontend/js/event-registrations.js) | 25 | Observation non confirmée : les requêtes incluent les cookies sans jeton CSRF explicite côté frontend. La protection dépend des règles serveur et des attributs des cookies. | 10/10 |
| 3 | ⚪ LOW | [frontend/js/qr-scanner.js](./frontend/js/qr-scanner.js) | 65, 115 | Observation : le client contrôle le format du jeton et empêche les soumissions concurrentes locales; validité, autorisation et rejeu doivent être contrôlés atomiquement par le backend. | 10/10 |
| 4 | ⚪ LOW | [frontend/js/main.js](./frontend/js/main.js) | 65 | Problème de compatibilité : l'appel au catalogue utilise HTTP même si la page est en HTTPS, et peut être bloqué comme contenu mixte. | 10/10 |

## Limites et coordination backend

- Audit statique frontend uniquement; aucun test navigateur ou runtime exécuté.
- `node` n'étant pas disponible dans l'environnement, les tests CJS existants et la vérification syntaxique JavaScript n'ont pas pu être exécutés.
- Les protections CSRF/CORS, attributs `SameSite`/`Secure` des cookies,
  autorisations, validations des jetons QR et contrôles anti-rejeu n'ont pas
  été vérifiés côté serveur.
- Les URL d'API et protections côté serveur sont à confirmer avec le
  développeur backend; aucun contrat API n'a été modifié.
