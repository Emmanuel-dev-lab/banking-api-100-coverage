# Comptoir — Frontend

Interface web de l'API bancaire (XAF). React + Vite + TypeScript, TanStack
Query, React Router, Tailwind CSS v4.

## Démarrer

```bash
npm install
npm run dev      # http://localhost:5173 (proxy /api -> http://localhost:8080)
```

Le backend Spring doit tourner sur le port 8080. Le proxy Vite renvoie les
appels `/api/**` vers ce backend : aucune configuration CORS n'est nécessaire
en développement.

### Comptes de démonstration (si `SEED_ENABLED=true` côté backend)

- `admin` / `admin123` — console d'administration
- `client1` / `client123` — espace client

## Scripts

| Commande | Effet |
|---|---|
| `npm run dev` | Serveur de dev avec HMR |
| `npm run build` | Vérification de types + build de production (`dist/`) |
| `npm run preview` | Sert le build de production |
| `npm test` | Tests unitaires (Vitest) |

## Architecture

```
src/
  api/        client fetch + endpoints typés + hooks TanStack Query
  auth/       contexte d'auth, décodage JWT, gardes de routes
  components/ primitives UI, modales, composants partagés
  layout/     coquille applicative (rail latéral selon le rôle)
  lib/        formatage (XAF, dates), erreurs, utilitaires
  pages/      écrans (client/, admin/, partagés)
```

### Choix clés

- **Contrat typé** (`src/api/types.ts`) : miroir des DTO du backend, source
  unique de vérité.
- **Auth JWT** : jeton stocké en mémoire + `localStorage`, rôle/`clientId`
  décodés côté client. Tout `401` purge la session et redirige vers `/login` ;
  l'expiration du jeton est anticipée.
- **Deux espaces** selon le rôle du jeton : espace **client** (self-service via
  `/api/me/*`) et **console admin** (clients, comptes, prêts, gel/fermeture).
- **Montants** : entiers XAF, affichés en chasse fixe à figures tabulaires.
- **Pagination** : reprend `page`/`size` du contrat ; l'historique des
  transactions est paginé et trié du plus récent au plus ancien.
