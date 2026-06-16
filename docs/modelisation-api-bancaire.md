# Modélisation — API Bancaire (objectif : couverture de branches 100 %)

> Document de conception **complet**, antérieur à toute implémentation.
> Cible : Java + Spring Boot, architecture hexagonale, JaCoCo en couverture de **branches 100 %** sur le code métier que nous écrivons.
> Devise unique : **XAF** (Franc CFA, **0 décimale** → montants entiers).

---

## 1. Objectif & contraintes

### 1.1 Objectif
Modéliser puis implémenter une API web bancaire dont **chaque branche** du code métier que nous écrivons est exécutée par au moins un test (couverture de branches = 100 %, ce qui implique couverture d'instructions = 100 %).

### 1.2 Périmètre fonctionnel
- **Authentification** : login → JWT ; rôles `CLIENT` et `ADMIN` ; autorisation par propriété de compte.
- **Clients** : un client possède N comptes.
- **Comptes** : `CURRENT` (découvert autorisé jusqu'à un plafond) et `SAVINGS` (pas de découvert, taux d'intérêt) ; statuts `ACTIVE` / `FROZEN` / `CLOSED`.
- **Transactions** : dépôt, retrait, virement (atomique : validation avant mutation).
- **Prêts** : amortissement constant, échéancier, remboursement, capital restant, détection de retard.

### 1.3 Contraintes transverses
- **Montants** : type `Money` encapsulant un `long` (unités XAF). Strictement `> 0` pour toute opération d'argent ; un solde peut être négatif uniquement dans la limite du découvert d'un compte courant.
- **Atomicité** : le use case valide **toutes** les règles avant de muter les agrégats. `@Transactional` (annotation, **0 branche** dans notre code) ajoute la durabilité DB + rollback du framework comme filet de sécurité. **Interdiction** de tout `try/catch`/rollback manuel qui créerait des branches d'infrastructure.
- **Déterminisme** : aucune dépendance directe à `LocalDate.now()`, `UUID.randomUUID()`, ou la crypto JWT dans la logique métier. Ces sources non déterministes sont derrière des interfaces injectables (`Clock`, `IdGenerator`, `TokenService`) → 100 % testable.

---

## 2. Architecture (hexagonale)

```
com.bank
├── domain                 (cœur métier — POJO, aucune dépendance framework)
│   ├── model              (entités, agrégats, value objects)
│   ├── exception          (exceptions métier)
│   └── port               (interfaces : repositories, Clock, IdGenerator, TokenService, PasswordHasher)
├── application            (use cases / services applicatifs — orchestration)
│   └── service
├── web                    (adaptateur entrant — REST controllers, mapping erreurs)
│   ├── controller
│   ├── dto                (records — EXCLUS de JaCoCo)
│   └── error              (GlobalExceptionHandler)
├── infrastructure         (adaptateurs sortants — EXCLUS de JaCoCo)
│   ├── persistence        (entités JPA, impl repositories)
│   └── security           (impl JWT réelle, BCrypt)
└── config                 (configuration Spring — EXCLUS de JaCoCo)
```

### 2.1 Règles de dépendance
- `domain` ne dépend de **rien** (ni Spring, ni JPA).
- `application` dépend de `domain` uniquement.
- `web` et `infrastructure` dépendent de `application` + `domain`.
- Les ports (interfaces) sont dans `domain.port` ; leurs implémentations dans `infrastructure`.

### 2.2 Configuration JaCoCo (couverture 100 % ciblée)
Couverture **branches = 100 %** exigée sur :
- `com.bank.domain.model.*`
- `com.bank.domain.exception.*` (parties avec logique)
- `com.bank.application.service.*`
- `com.bank.web.controller.*`
- `com.bank.web.error.*`

**Exclusions** (code sans branche métier ou généré) :
- `com.bank.web.dto.*` (records)
- `com.bank.infrastructure.**`
- `com.bank.config.**`
- Classe `Application` (`main`)
- Méthodes générées : `equals`, `hashCode`, `toString` des records (exclues via configuration JaCoCo `excludes` ou via `@Generated`).

> Règle de discipline : tout `if`, `else`, `switch`, `? :`, `&&`, `||`, `catch`, et toute branche d'un `for`/`while` doit avoir un test l'exécutant dans chaque sens.

---

## 3. Conventions transverses

### 3.1 Value object `Money`
Encapsule un `long amount` (unités XAF entières, jamais de décimale).

| Méthode | Comportement | Branches |
|---|---|---|
| `of(long v)` | crée ; `v < 0` → `IllegalArgumentException` | `v < 0` vrai/faux |
| `ofPositive(long v)` | `v <= 0` → `InvalidAmountException` | `v <= 0` vrai/faux |
| `plus(Money o)` | addition | aucune |
| `minus(Money o)` | soustraction (peut être négatif) | aucune |
| `isNegative()` | `amount < 0` | retour booléen (1 expression, 2 valeurs → testé via 2 cas) |
| `isGreaterThan(Money o)` | `amount > o.amount` | idem |
| `isLessThan(Money o)` | `amount < o.amount` | idem |

> `Money` est immuable (record-like). `equals`/`hashCode` exclus.

### 3.2 Modèle d'erreurs (exceptions métier)
Toutes héritent de `BankException` (runtime). Chacune porte un `code` stable mappé à un statut HTTP.

| Exception | Code | HTTP | Sens |
|---|---|---|---|
| `InvalidAmountException` | `INVALID_AMOUNT` | 400 | montant ≤ 0 ou décimale |
| `AccountNotFoundException` | `ACCOUNT_NOT_FOUND` | 404 | compte inconnu |
| `ClientNotFoundException` | `CLIENT_NOT_FOUND` | 404 | client inconnu |
| `LoanNotFoundException` | `LOAN_NOT_FOUND` | 404 | prêt inconnu |
| `InsufficientFundsException` | `INSUFFICIENT_FUNDS` | 422 | solde/découvert dépassé |
| `AccountNotActiveException` | `ACCOUNT_NOT_ACTIVE` | 409 | compte FROZEN/CLOSED |
| `SameAccountTransferException` | `SAME_ACCOUNT_TRANSFER` | 422 | virement source = destination |
| `OverdraftNotAllowedException` | `OVERDRAFT_NOT_ALLOWED` | 422 | découvert sur épargne |
| `InvalidLoanTermsException` | `INVALID_LOAN_TERMS` | 400 | montant/taux/durée invalides |
| `LoanAlreadyClosedException` | `LOAN_ALREADY_CLOSED` | 409 | remboursement sur prêt soldé |
| `UnauthorizedException` | `UNAUTHORIZED` | 401 | token absent/invalide/expiré |
| `ForbiddenException` | `FORBIDDEN` | 403 | rôle insuffisant / accès compte d'autrui |

### 3.3 Ports (interfaces injectables)
- `Clock` → `LocalDate today()` (déterminisme dates).
- `IdGenerator` → `String newId()`.
- `PasswordHasher` → `String hash(String raw)`, `boolean matches(String raw, String hash)`.
- `TokenService` → `String issue(User u)`, `TokenClaims verify(String token)` (lève `UnauthorizedException`).
- `ClientRepository`, `AccountRepository`, `TransactionRepository`, `LoanRepository`, `UserRepository`.

---

## 4. Modèle de domaine

### 4.1 `Role`
`enum Role { CLIENT, ADMIN }`

### 4.2 `User` (identité d'authentification)
Champs : `id`, `username`, `passwordHash`, `Role role`, `clientId` (null si ADMIN).

| Méthode | Branches |
|---|---|
| `isAdmin()` | `role == ADMIN` v/f |
| `owns(String accountOwnerClientId)` | `clientId != null && clientId.equals(x)` → 2 conditions × 2 |

### 4.3 `Client`
Champs : `id`, `firstName`, `lastName`. Pas de logique branchante (validation des champs vides ci-dessous).

| Méthode | Branches |
|---|---|
| constructeur | `firstName` vide → exception ; `lastName` vide → exception (2 `if`) |

### 4.4 `AccountStatus`
`enum AccountStatus { ACTIVE, FROZEN, CLOSED }`

### 4.5 `AccountType`
`enum AccountType { CURRENT, SAVINGS }`

### 4.6 `Account` (agrégat — classe abstraite)
Champs : `id`, `clientId`, `AccountType type`, `Money balance`, `AccountStatus status`.

| Méthode | Règle | Branches |
|---|---|---|
| `ensureActive()` | `status != ACTIVE` → `AccountNotActiveException` | v/f |
| `deposit(Money amt)` | `ensureActive()` ; `balance = balance.plus(amt)` | (montant > 0 garanti en amont par `Money.ofPositive`) ; appelle `ensureActive` |
| `withdraw(Money amt)` | `ensureActive()` ; si `balance.minus(amt)` viole la règle de découvert → `InsufficientFundsException` ; sinon mut | délègue à `canWithdraw(amt)` |
| `canWithdraw(Money amt)` | **abstraite** — règle spécifique au sous-type | — |
| `freeze()` | `status = FROZEN` | (modélisé sans garde → aucune branche) |
| `close()` | `balance.isNegative() \|\| balance.amount>0` ? règle de fermeture | voir 4.6.1 |

#### 4.6.1 Règle de fermeture `close()`
- Si `balance` ≠ 0 → `IllegalStateException` (« compte non soldé »).
- Sinon `status = CLOSED`.
- Branches : `balance.amount != 0` vrai/faux.

#### `CurrentAccount`
- `long overdraftLimit` (≥ 0, plafond de découvert ; 0 par défaut).
- `canWithdraw(Money amt)` : autorisé si `balance - amt >= -overdraftLimit`.
  - Branches : résultat `>= -overdraftLimit` vrai (autorisé) / faux (refus → `InsufficientFundsException`).

#### `SavingsAccount`
- `double annualRate` (ex. 0.03).
- `canWithdraw(Money amt)` : autorisé si `balance - amt >= 0` (jamais de découvert).
  - Branches : vrai / faux.
- `applyInterest()` : `interest = floor(balance * annualRate)` ; si `interest > 0` → `deposit`.
  - Branches : `interest > 0` vrai/faux (taux 0 ou solde 0 → pas de dépôt).

### 4.7 `Transaction` (écriture de registre — historique)
`enum TransactionType { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, LOAN_DISBURSEMENT, LOAN_REPAYMENT }`
Champs : `id`, `accountId`, `TransactionType type`, `Money amount`, `LocalDate date`, `relatedAccountId` (nullable). Immuable. Pas de logique branchante.

### 4.8 `Loan` (agrégat prêt)
Champs : `id`, `clientId`, `accountId` (compte de décaissement/remboursement), `Money principal`, `double annualRate`, `int termMonths`, `LoanStatus status`, `Money outstandingPrincipal`, `List<Installment> schedule`, `LocalDate startDate`.

`enum LoanStatus { ACTIVE, PAID_OFF }`

#### 4.8.1 Validation à la création (`Loan.create(...)`)
- `principal <= 0` → `InvalidLoanTermsException`
- `termMonths <= 0` → `InvalidLoanTermsException`
- `annualRate < 0` → `InvalidLoanTermsException`
- Branches : 3 `if` indépendants (chacun v/f).

#### 4.8.2 Calcul de mensualité (amortissement constant)
Taux mensuel `t = annualRate / 12`.
- **Si `t == 0`** (taux nul) → mensualité `M = ceil(principal / termMonths)`.
- **Sinon** → `M = principal * t / (1 - (1+t)^(-termMonths))`, arrondi à l'entier XAF (`Math.round`).
- Branches : `t == 0` vrai/faux.

#### 4.8.3 Génération de l'échéancier `buildSchedule()`
Boucle sur `termMonths` :
- chaque `Installment` : `dueDate`, `amount` (mensualité ; **dernière échéance** ajustée pour solder exactement le capital + intérêts résiduels), `principalPart`, `interestPart`, `paid=false`.
- Branches : boucle `for` (corps exécuté / sortie) ; `i == termMonths - 1` (dernière échéance, ajustement) vrai/faux.

#### 4.8.4 Remboursement `repay(Money amt, LocalDate date)`
- Si `status == PAID_OFF` → `LoanAlreadyClosedException`.
- Réduit `outstandingPrincipal` du capital remboursé.
- Si `outstandingPrincipal <= 0` après remboursement → `status = PAID_OFF`.
- Branches : `status == PAID_OFF` v/f ; `outstandingPrincipal.amount <= 0` v/f.

#### 4.8.5 Retard `isLate(LocalDate today)`
- Vrai si une `Installment` non payée a `dueDate < today`.
- Branches : boucle ; condition `!inst.paid && inst.dueDate.isBefore(today)` (2 sous-conditions) ; retour anticipé `true` vs `false` final.

### 4.9 `Installment`
Champs : `index`, `dueDate`, `Money amount`, `Money principalPart`, `Money interestPart`, `boolean paid`. `markPaid()` → `paid = true` (aucune branche).

---

## 5. Couche application (use cases)

Chaque service est `@Transactional`. Les méthodes valident **puis** mutent. Les branches listées sont **toutes** à couvrir.

### 5.1 `AuthService`
- `login(username, rawPassword)` :
  1. `user = userRepository.findByUsername(username)` ; absent → `UnauthorizedException`.
  2. `passwordHasher.matches(raw, user.passwordHash)` faux → `UnauthorizedException`.
  3. sinon `tokenService.issue(user)`.
  - Branches : user présent v/f ; password match v/f.
- `authenticate(token)` → `TokenClaims` via `tokenService.verify` (lève `UnauthorizedException`). Pas de branche propre.

### 5.2 `AuthorizationGuard` (logique d'autorisation pure)
- `requireOwnerOrAdmin(TokenClaims claims, String ownerClientId)` :
  - si `claims.role == ADMIN` → OK.
  - sinon si `claims.clientId.equals(ownerClientId)` → OK.
  - sinon → `ForbiddenException`.
  - Branches : `role == ADMIN` v/f ; `clientId.equals(owner)` v/f.
- `requireAdmin(claims)` : `role != ADMIN` → `ForbiddenException`. (v/f)

### 5.3 `ClientService`
- `createClient(...)` (ADMIN) : crée `Client` + `User` CLIENT. Branches : validation déléguée au domaine.
- `getClient(id)` : absent → `ClientNotFoundException`. (v/f)

### 5.4 `AccountService`
- `openAccount(clientId, type, overdraftLimit/annualRate)` :
  - client absent → `ClientNotFoundException`.
  - `type == CURRENT` → instancie `CurrentAccount` (`overdraftLimit >= 0` sinon `IllegalArgumentException`).
  - `type == SAVINGS` → instancie `SavingsAccount` (`annualRate >= 0` sinon `IllegalArgumentException`).
  - Branches : client v/f ; `switch(type)` 2 cas ; gardes paramètres v/f.
- `getAccount(id)` : absent → `AccountNotFoundException`. (v/f)
- `deposit(accountId, amount)` :
  - `Money.ofPositive(amount)` (montant ≤ 0 → `InvalidAmountException`).
  - compte absent → `AccountNotFoundException`.
  - `account.deposit(money)` (peut lever `AccountNotActiveException`).
  - enregistre `Transaction(DEPOSIT)`.
  - Branches : montant valide v/f ; compte v/f ; actif v/f (dans `deposit`).
- `withdraw(accountId, amount)` :
  - `Money.ofPositive`, compte présent, `account.withdraw` (lève `AccountNotActiveException` ou `InsufficientFundsException`).
  - enregistre `Transaction(WITHDRAWAL)`.
- `getHistory(accountId)` : compte absent → `AccountNotFoundException`. (v/f)

### 5.5 `TransferService`
- `transfer(sourceId, destId, amount)` :
  1. `Money.ofPositive(amount)` → montant ≤ 0 → `InvalidAmountException`.
  2. `sourceId.equals(destId)` → `SameAccountTransferException`.
  3. `source` absent → `AccountNotFoundException`.
  4. `dest` absent → `AccountNotFoundException`.
  5. `source.ensureActive()` + `dest.ensureActive()` (FROZEN/CLOSED → `AccountNotActiveException`).
  6. `source.withdraw(amount)` (peut lever `InsufficientFundsException`) — **après** validations.
  7. `dest.deposit(amount)`.
  8. enregistre `Transaction(TRANSFER_OUT)` + `Transaction(TRANSFER_IN)`.
  - Branches : montant v/f ; même compte v/f ; source v/f ; dest v/f ; source active v/f ; dest active v/f ; fonds source v/f.

### 5.6 `LoanService`
- `requestLoan(clientId, accountId, principal, annualRate, termMonths)` :
  - client absent → `ClientNotFoundException` ; compte absent → `AccountNotFoundException`.
  - `Loan.create(...)` (validation 4.8.1) ; `buildSchedule()`.
  - décaissement : `account.deposit(principal)` + `Transaction(LOAN_DISBURSEMENT)`.
  - Branches : client v/f ; compte v/f ; (+ branches de `Loan.create`).
- `repayLoan(loanId, amount)` :
  - `Money.ofPositive` ; prêt absent → `LoanNotFoundException` ; `loan.repay(...)` (lève `LoanAlreadyClosedException`) ; `account.withdraw(amount)` + `Transaction(LOAN_REPAYMENT)`.
  - Branches : montant v/f ; prêt v/f ; statut prêt v/f ; solde après remboursement v/f.
- `getSchedule(loanId)` : absent → `LoanNotFoundException`. (v/f)

---

## 6. Couche web (REST)

### 6.1 Endpoints
| Méthode | Chemin | Rôle requis | Use case |
|---|---|---|---|
| POST | `/api/auth/login` | public | `AuthService.login` |
| POST | `/api/clients` | ADMIN | `ClientService.createClient` |
| GET | `/api/clients/{id}` | owner/ADMIN | `ClientService.getClient` |
| POST | `/api/clients/{id}/accounts` | owner/ADMIN | `AccountService.openAccount` |
| GET | `/api/accounts/{id}` | owner/ADMIN | `AccountService.getAccount` |
| POST | `/api/accounts/{id}/deposit` | owner/ADMIN | `AccountService.deposit` |
| POST | `/api/accounts/{id}/withdraw` | owner/ADMIN | `AccountService.withdraw` |
| GET | `/api/accounts/{id}/transactions` | owner/ADMIN | `AccountService.getHistory` |
| POST | `/api/transfers` | owner(source)/ADMIN | `TransferService.transfer` |
| POST | `/api/loans` | owner/ADMIN | `LoanService.requestLoan` |
| GET | `/api/loans/{id}/schedule` | owner/ADMIN | `LoanService.getSchedule` |
| POST | `/api/loans/{id}/repay` | owner/ADMIN | `LoanService.repayLoan` |

### 6.2 Filtre d'authentification (`JwtAuthFilter` — logique branchante à couvrir si écrite par nous)
> Décision : autorisation gérée **explicitement dans les controllers** via `AuthorizationGuard` (logique pure, 100 % testable), plutôt que dans un filtre Spring Security difficile à couvrir. Le controller :
1. extrait le header `Authorization` ; absent ou ne commençant pas par `Bearer ` → `UnauthorizedException`.
2. `claims = authService.authenticate(token)`.
3. `authorizationGuard.requireOwnerOrAdmin/​requireAdmin(...)`.
- Branches (par controller protégé) : header présent v/f ; préfixe `Bearer ` v/f.
> Pour éviter la duplication : un helper `RequestAuth.bearer(headerValue)` centralise l'extraction (2 branches, testées une fois).

### 6.3 `GlobalExceptionHandler` (`@RestControllerAdvice`)
Mappe chaque `BankException` → statut HTTP + corps `{code, message}`. Un `@ExceptionHandler` par famille. Chaque handler = 0 branche (mapping direct) mais **chaque handler doit être déclenché par un test** (sinon instruction non couverte).
- Handler générique `BankException` → lit `exception.httpStatus()`. Le `switch`/mapping de statut, s'il existe, voit chaque code testé.

> Simplification anti-branches : `BankException` porte directement `int httpStatus()` et `String code()`. Le handler retourne `ResponseEntity.status(ex.httpStatus()).body(new ErrorResponse(ex.code(), ex.getMessage()))` → **aucune branche** dans le handler. Les branches sont dans les exceptions elles-mêmes (constantes, sans `if`).

---

## 7. Sécurité

- Mots de passe : `PasswordHasher` (BCrypt en infra). Logique métier teste via fake déterministe.
- JWT : `TokenService` (impl infra signe/vérifie). Le domaine/app ne dépend que de l'interface ; tests utilisent un fake renvoyant des `TokenClaims` contrôlés → la crypto réelle n'entre pas dans la couverture métier.
- `TokenClaims` : `userId`, `clientId` (nullable), `Role role`.

---

## 8. Stratégie de test & couverture

### 8.1 Niveaux
1. **Tests unitaires domaine** (`domain.model`, `domain.exception`) — JUnit 5 pur, aucun mock framework. Couvrent la quasi-totalité des branches métier.
2. **Tests unitaires application** (`application.service`) — repositories et ports remplacés par **fakes in-memory** (pas Mockito imposé ; fakes déterministes). Couvrent les branches d'orchestration.
3. **Tests web** (`@WebMvcTest` + services mockés) — couvrent extraction token, autorisation, mapping HTTP, sérialisation.
4. **Test d'intégration** (`@SpringBootTest`, profil H2) — **hors quota de couverture** ; valide le câblage réel (non requis pour le 100 %, mais inclus pour la confiance).

### 8.2 Politique 100 % branches
- Build Gradle/Maven : `jacocoTestCoverageVerification` avec `branch = 1.0` (100 %) sur les packages de §2.2 ; échec du build sinon.
- Revue : chaque PR vérifie le rapport JaCoCo ; aucune ligne rouge/jaune dans les packages ciblés.

### 8.3 Principe de conception « anti-branches cachées »
- Pas d'opérateur `&&`/`||` non testé dans les deux sens.
- Préférer des gardes simples séquentielles (chaque `if` testé v/f) à des conditions composées difficiles à couvrir.
- Aucune branche défensive « impossible » (pas de `if (x == null) throw` non atteignable) : si non atteignable, on ne l'écrit pas.

---

## 9. Énumération exhaustive des branches → cas de tests

> Chaque ligne = **une branche** ⇒ **au moins un test**. La somme couvre 100 %.
> Nomenclature test : `Classe.methode_condition_attendu`.

### 9.1 `Money`
| # | Branche | Cas de test | Attendu |
|---|---|---|---|
| M1 | `of` v < 0 vrai | `of_negative_throws` | `IllegalArgumentException` |
| M2 | `of` v < 0 faux | `of_zeroOrPositive_ok` | objet créé |
| M3 | `ofPositive` v ≤ 0 vrai | `ofPositive_zero_throws` | `InvalidAmountException` |
| M4 | `ofPositive` v ≤ 0 faux | `ofPositive_positive_ok` | objet créé |
| M5 | `isNegative` vrai | `isNegative_negative_true` | `true` |
| M6 | `isNegative` faux | `isNegative_positive_false` | `false` |
| M7 | `isGreaterThan` vrai/faux | `isGreaterThan_x2` | `true` puis `false` |
| M8 | `isLessThan` vrai/faux | `isLessThan_x2` | `true` puis `false` |

### 9.2 `User`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| U1 | `isAdmin` vrai | `isAdmin_adminRole_true` | true |
| U2 | `isAdmin` faux | `isAdmin_clientRole_false` | false |
| U3 | `owns` clientId null | `owns_adminNullClientId_false` | false |
| U4 | `owns` egal | `owns_matching_true` | true |
| U5 | `owns` different | `owns_other_false` | false |

### 9.3 `Client` (constructeur)
| # | Branche | Cas | Attendu |
|---|---|---|---|
| C1 | firstName vide | `ctor_emptyFirstName_throws` | exception |
| C2 | firstName ok / lastName vide | `ctor_emptyLastName_throws` | exception |
| C3 | les deux ok | `ctor_valid_ok` | objet |

### 9.4 `Account` (commun)
| # | Branche | Cas | Attendu |
|---|---|---|---|
| A1 | `ensureActive` non actif | `ensureActive_frozen_throws` | `AccountNotActiveException` |
| A2 | `ensureActive` actif | `ensureActive_active_ok` | pas d'exception |
| A3 | `close` solde ≠ 0 | `close_nonZeroBalance_throws` | `IllegalStateException` |
| A4 | `close` solde = 0 | `close_zeroBalance_closed` | status CLOSED |
| A5 | `deposit` sur compte gelé | `deposit_frozen_throws` | `AccountNotActiveException` |
| A6 | `deposit` actif | `deposit_active_increasesBalance` | solde + montant |

### 9.5 `CurrentAccount.canWithdraw`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| CA1 | dans limite découvert (vrai) | `withdraw_withinOverdraft_ok` | solde diminué (peut être négatif) |
| CA2 | dépasse découvert (faux) | `withdraw_beyondOverdraft_throws` | `InsufficientFundsException` |
| CA3 | limite découvert exacte (frontière) | `withdraw_exactlyAtOverdraftLimit_ok` | ok (`>=`) |

### 9.6 `SavingsAccount`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| SA1 | `canWithdraw` solde suffisant | `withdraw_enough_ok` | ok |
| SA2 | `canWithdraw` solde insuffisant | `withdraw_insufficient_throws` | `InsufficientFundsException` |
| SA3 | `applyInterest` interest > 0 | `applyInterest_positive_credits` | solde augmenté |
| SA4 | `applyInterest` interest = 0 (taux 0 ou solde 0) | `applyInterest_zero_noChange` | solde inchangé |

### 9.7 `Loan.create`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| L1 | principal ≤ 0 | `create_nonPositivePrincipal_throws` | `InvalidLoanTermsException` |
| L2 | termMonths ≤ 0 | `create_nonPositiveTerm_throws` | idem |
| L3 | annualRate < 0 | `create_negativeRate_throws` | idem |
| L4 | tout valide | `create_valid_ok` | objet |

### 9.8 `Loan.monthlyPayment`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| L5 | t == 0 (taux nul) | `payment_zeroRate_principalOverTerm` | `ceil(P/n)` |
| L6 | t > 0 | `payment_positiveRate_amortized` | formule amortissement |

### 9.9 `Loan.buildSchedule`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| L7 | corps de boucle exécuté | `schedule_size_equalsTerm` | n échéances |
| L8 | dernière échéance ajustée | `schedule_lastInstallment_balancesPrincipal` | somme capital = principal |

### 9.10 `Loan.repay`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| L9 | déjà PAID_OFF | `repay_alreadyPaidOff_throws` | `LoanAlreadyClosedException` |
| L10 | remboursement partiel (reste > 0) | `repay_partial_stillActive` | ACTIVE, outstanding diminué |
| L11 | remboursement soldant (reste ≤ 0) | `repay_full_paidOff` | PAID_OFF |

### 9.11 `Loan.isLate`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| L12 | échéance impayée & en retard | `isLate_overdueUnpaid_true` | true |
| L13 | échéance payée (sous-cond `!paid` faux) | `isLate_paidInstallment_false` | false |
| L14 | échéance non échue (dueDate future) | `isLate_futureDue_false` | false |
| L15 | aucune échéance en retard → fin de boucle | `isLate_allCurrent_false` | false |

### 9.12 `AuthService.login`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| AS1 | user absent | `login_unknownUser_unauthorized` | `UnauthorizedException` |
| AS2 | password ne correspond pas | `login_wrongPassword_unauthorized` | `UnauthorizedException` |
| AS3 | succès | `login_valid_returnsToken` | token |

### 9.13 `AuthorizationGuard`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| AG1 | ADMIN | `ownerOrAdmin_admin_ok` | ok |
| AG2 | owner correspond | `ownerOrAdmin_owner_ok` | ok |
| AG3 | ni admin ni owner | `ownerOrAdmin_other_forbidden` | `ForbiddenException` |
| AG4 | `requireAdmin` non-admin | `requireAdmin_client_forbidden` | `ForbiddenException` |
| AG5 | `requireAdmin` admin | `requireAdmin_admin_ok` | ok |

### 9.14 `ClientService`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| CS1 | `getClient` absent | `getClient_unknown_throws` | `ClientNotFoundException` |
| CS2 | `getClient` présent | `getClient_existing_returns` | client |
| CS3 | `createClient` ok | `createClient_valid_persists` | client + user créés |

### 9.15 `AccountService`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| AC1 | `openAccount` client absent | `open_unknownClient_throws` | `ClientNotFoundException` |
| AC2 | `openAccount` CURRENT | `open_current_ok` | CurrentAccount |
| AC3 | `openAccount` SAVINGS | `open_savings_ok` | SavingsAccount |
| AC4 | `openAccount` overdraft < 0 | `open_negativeOverdraft_throws` | `IllegalArgumentException` |
| AC5 | `openAccount` rate < 0 | `open_negativeRate_throws` | `IllegalArgumentException` |
| AC6 | `getAccount` absent | `get_unknown_throws` | `AccountNotFoundException` |
| AC7 | `getAccount` présent | `get_existing_returns` | compte |
| AC8 | `deposit` montant ≤ 0 | `deposit_invalidAmount_throws` | `InvalidAmountException` |
| AC9 | `deposit` compte absent | `deposit_unknownAccount_throws` | `AccountNotFoundException` |
| AC10 | `deposit` ok | `deposit_valid_recordsTxn` | solde + txn |
| AC11 | `withdraw` montant ≤ 0 | `withdraw_invalidAmount_throws` | `InvalidAmountException` |
| AC12 | `withdraw` compte absent | `withdraw_unknownAccount_throws` | `AccountNotFoundException` |
| AC13 | `withdraw` ok | `withdraw_valid_recordsTxn` | solde - + txn |
| AC14 | `getHistory` absent | `history_unknown_throws` | `AccountNotFoundException` |
| AC15 | `getHistory` présent | `history_existing_returnsList` | liste txn |

### 9.16 `TransferService.transfer`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| T1 | montant ≤ 0 | `transfer_invalidAmount_throws` | `InvalidAmountException` |
| T2 | source = dest | `transfer_sameAccount_throws` | `SameAccountTransferException` |
| T3 | source absente | `transfer_unknownSource_throws` | `AccountNotFoundException` |
| T4 | dest absente | `transfer_unknownDest_throws` | `AccountNotFoundException` |
| T5 | source non active | `transfer_frozenSource_throws` | `AccountNotActiveException` |
| T6 | dest non active | `transfer_frozenDest_throws` | `AccountNotActiveException` |
| T7 | fonds source insuffisants | `transfer_insufficientFunds_throws` | `InsufficientFundsException` |
| T8 | succès complet | `transfer_valid_movesFundsAndRecords2Txn` | soldes ajustés + 2 txns |

### 9.17 `LoanService`
| # | Branche | Cas | Attendu |
|---|---|---|---|
| LS1 | `requestLoan` client absent | `request_unknownClient_throws` | `ClientNotFoundException` |
| LS2 | `requestLoan` compte absent | `request_unknownAccount_throws` | `AccountNotFoundException` |
| LS3 | `requestLoan` ok | `request_valid_disbursesAndSchedules` | prêt + crédit compte + txn |
| LS4 | `repayLoan` montant ≤ 0 | `repay_invalidAmount_throws` | `InvalidAmountException` |
| LS5 | `repayLoan` prêt absent | `repay_unknownLoan_throws` | `LoanNotFoundException` |
| LS6 | `repayLoan` prêt soldé | `repay_paidOffLoan_throws` | `LoanAlreadyClosedException` |
| LS7 | `repayLoan` ok | `repay_valid_reducesAndRecords` | outstanding - + txn |
| LS8 | `getSchedule` absent | `schedule_unknown_throws` | `LoanNotFoundException` |
| LS9 | `getSchedule` présent | `schedule_existing_returns` | échéancier |

### 9.18 Couche web — extraction token & autorisation (`RequestAuth.bearer` + controllers)
| # | Branche | Cas | Attendu |
|---|---|---|---|
| W1 | header absent | `bearer_missingHeader_unauthorized` | 401 |
| W2 | préfixe `Bearer ` absent | `bearer_badPrefix_unauthorized` | 401 |
| W3 | token bien formé | `bearer_valid_returnsToken` | token extrait |

### 9.19 Couche web — déclenchement de chaque mapping d'erreur (`GlobalExceptionHandler`)
> Un test d'intégration web (`@WebMvcTest`) par famille pour exécuter l'instruction de chaque handler.
| # | Cas | Statut attendu |
|---|---|---|
| H1 | `InvalidAmountException` | 400 + `{code:INVALID_AMOUNT}` |
| H2 | `AccountNotFoundException` | 404 |
| H3 | `InsufficientFundsException` | 422 |
| H4 | `AccountNotActiveException` | 409 |
| H5 | `SameAccountTransferException` | 422 |
| H6 | `UnauthorizedException` | 401 |
| H7 | `ForbiddenException` | 403 |
| H8 | `InvalidLoanTermsException` | 400 |
| H9 | `LoanAlreadyClosedException` | 409 |
| H10 | `ClientNotFoundException` / `LoanNotFoundException` | 404 |

### 9.20 Endpoints — chemin nominal (sérialisation + statut succès)
| # | Cas | Attendu |
|---|---|---|
| E1 | POST login | 200 + token |
| E2 | POST client (ADMIN) | 201 |
| E3 | GET client (owner) | 200 |
| E4 | POST account | 201 |
| E5 | POST deposit | 200 + solde |
| E6 | POST withdraw | 200 |
| E7 | GET transactions | 200 + liste |
| E8 | POST transfer | 200 |
| E9 | POST loan | 201 + échéancier |
| E10 | GET schedule | 200 |
| E11 | POST repay | 200 |
| E12 | endpoint protégé sans token | 401 |
| E13 | endpoint ADMIN appelé par CLIENT | 403 |

---

## 10. Récapitulatif de couverture

| Module | Méthodes branchantes | Branches | Tests dédiés |
|---|---|---|---|
| `Money` | 5 | ~10 | M1–M8 |
| `User` | 2 | 5 | U1–U5 |
| `Client` | 1 | 3 | C1–C3 |
| `Account` + sous-types | 6 | ~14 | A1–A6, CA1–CA3, SA1–SA4 |
| `Loan` | 5 | ~16 | L1–L15 |
| `AuthService` / `AuthorizationGuard` | 3 | 8 | AS1–AS3, AG1–AG5 |
| `ClientService` | 2 | 3 | CS1–CS3 |
| `AccountService` | 5 | ~15 | AC1–AC15 |
| `TransferService` | 1 | 8 | T1–T8 |
| `LoanService` | 3 | 9 | LS1–LS9 |
| Web (auth + erreurs + endpoints) | — | — | W1–W3, H1–H10, E1–E13 |

**Garantie** : chaque branche identifiée possède un test la franchissant dans chaque sens ⇒ couverture de branches = 100 % ⇒ couverture d'instructions = 100 % sur les packages ciblés.

---

## 11. Stack & outillage prévu (pour la phase d'implémentation)
- **Java 21**, **Spring Boot 3.x**.
- **Build** : Maven ou Gradle (au choix), plugin **JaCoCo** avec `branch = 1.0`.
- **Tests** : JUnit 5, AssertJ, Spring `@WebMvcTest` / `@SpringBootTest`, H2 (intégration).
- **Sécurité** : JWT (impl infra), BCrypt.
- **Persistance** : JPA + H2 (dev/test), interface repository en domaine (in-memory fakes pour tests unitaires).

---

*Fin du modèle. Dès validation, passage à l'implémentation complète (domaine → application → web → infrastructure → tests).*
