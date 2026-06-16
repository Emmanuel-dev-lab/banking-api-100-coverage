# API Bancaire — couverture de branches 100 %

API web bancaire en Java / Spring Boot, conçue puis implémentée avec une
**couverture de branches de 100 %** (donc une couverture d'instructions de
100 %) sur le code métier, vérifiée automatiquement par JaCoCo au build.

Projet réalisé dans le cadre du cours **ICT304 — Software Testing**.

## Périmètre fonctionnel

- **Authentification** : login → JWT, rôles `CLIENT` et `ADMIN`, autorisation par propriété de compte.
- **Clients** : un client possède N comptes.
- **Comptes** : `CURRENT` (découvert autorisé jusqu'à un plafond) et `SAVINGS` (pas de découvert, taux d'intérêt). Statuts `ACTIVE` / `FROZEN` / `CLOSED`.
- **Transactions** : dépôt, retrait, virement atomique, historique.
- **Prêts** : amortissement constant, échéancier, remboursement, capital restant, détection de retard.
- **Devise** : XAF (Franc CFA, 0 décimale → montants entiers).

## Stack

- Java 21, Spring Boot 3.3
- Spring Data JPA + H2 (en mémoire)
- jjwt (JWT), spring-security-crypto (BCrypt)
- JUnit 5, AssertJ
- JaCoCo (seuil branch = 100 %, instruction = 100 %)

## Architecture (hexagonale)

```
com.bank
├── domain          coeur metier pur (aucune dependance framework)
│   ├── model       Money, Account, Loan, Client, User, Transaction...
│   ├── exception   BankException + sous-types (code + statut HTTP)
│   └── port        interfaces : repositories, Clock, IdGenerator,
│                   TokenService, PasswordHasher
├── application     use cases (services), orchestration + @Transactional
├── web             controllers REST, DTO, mapping d'erreurs
└── infrastructure  adaptateurs : JPA/H2, JWT, BCrypt (exclus de la couverture)
```

Principe clé : la logique métier valide **avant** de muter (atomicité logique).
`@Transactional` ajoute la durabilité/rollback DB sans introduire de branche
dans notre code. Les sources non déterministes (date, id, crypto) sont derrière
des ports injectables, ce qui rend le métier testable à 100 % sans framework.

## Build & exécution

Prérequis : JDK 21, Maven 3.9+.

```bash
# compiler + tester + verifier la couverture 100% (echoue si < 100%)
mvn verify

# lancer l'API (http://localhost:8080)
mvn spring-boot:run
```

Le rapport de couverture est généré dans `target/site/jacoco/index.html`.

## Documentation interactive (Swagger / OpenAPI)

Une fois l'API lancée :

- **Swagger UI** : http://localhost:8080/swagger-ui.html
- **Contrat OpenAPI (JSON)** : http://localhost:8080/v3/api-docs

Chaque endpoint y est décrit (résumé, paramètres, codes de réponse). Pour les
endpoints protégés : appeler `POST /api/auth/login`, copier le jeton, cliquer
sur **Authorize** (cadenas) et le coller — il sera ajouté automatiquement en
`Authorization: Bearer <token>` sur les appels suivants.

## Tests & couverture

- **130 tests** : unitaires domaine (JUnit pur), unitaires application (fakes
  in-memory déterministes), web (controllers + handler), et un test
  d'intégration `@SpringBootTest` validant le câblage réel sur H2.
- Le plugin JaCoCo impose `BRANCH = 100 %` **et** `INSTRUCTION = 100 %`. Le
  build échoue en cas de régression.
- Exclusions de la couverture (code framework / généré / sans branche métier) :
  `Application`, `config`, `infrastructure`, `web.dto`.

La conception détaillée — y compris la table exhaustive « branche → cas de
test » qui garantit le 100 % — est dans
[`docs/modelisation-api-bancaire.md`](docs/modelisation-api-bancaire.md).

## Endpoints REST

| Méthode | Chemin                              | Rôle requis      |
|---------|-------------------------------------|------------------|
| POST    | `/api/auth/login`                   | public           |
| POST    | `/api/clients`                      | ADMIN            |
| GET     | `/api/clients/{id}`                 | owner / ADMIN    |
| POST    | `/api/clients/{id}/accounts`        | owner / ADMIN    |
| GET     | `/api/accounts/{id}`                | owner / ADMIN    |
| POST    | `/api/accounts/{id}/deposit`        | owner / ADMIN    |
| POST    | `/api/accounts/{id}/withdraw`       | owner / ADMIN    |
| GET     | `/api/accounts/{id}/transactions`   | owner / ADMIN    |
| POST    | `/api/transfers`                    | owner / ADMIN    |
| POST    | `/api/loans`                        | owner / ADMIN    |
| GET     | `/api/loans/{id}/schedule`          | owner / ADMIN    |
| POST    | `/api/loans/{id}/repay`             | owner / ADMIN    |

Les endpoints protégés attendent l'en-tête `Authorization: Bearer <token>`.

### Exemple

```bash
# login
curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"..."}'

# depot (avec le jeton obtenu)
curl -s -X POST localhost:8080/api/accounts/{id}/deposit \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{"amount":10000}'
```

## Modèle d'erreurs

Chaque exception métier porte un `code` stable et un statut HTTP, renvoyés en
JSON `{ "code": ..., "message": ... }` :

| Code                    | HTTP |
|-------------------------|------|
| `INVALID_AMOUNT`        | 400  |
| `INVALID_LOAN_TERMS`    | 400  |
| `UNAUTHORIZED`          | 401  |
| `FORBIDDEN`             | 403  |
| `ACCOUNT_NOT_FOUND`     | 404  |
| `CLIENT_NOT_FOUND`      | 404  |
| `LOAN_NOT_FOUND`        | 404  |
| `ACCOUNT_NOT_ACTIVE`    | 409  |
| `LOAN_ALREADY_CLOSED`   | 409  |
| `INSUFFICIENT_FUNDS`    | 422  |
| `SAME_ACCOUNT_TRANSFER` | 422  |
