# CipherTalk — Backend (API + WebSocket + PostgreSQL)

Backend Node.js / Express qui remplace la démo Alice & Bob de la page web par un
vrai service de messagerie multi-utilisateurs : comptes, conversations, historique
persistant en base PostgreSQL, et diffusion temps réel par WebSocket.

## 1. Prérequis

- Node.js 18+
- PostgreSQL 13+ installé et démarré (en local, en conteneur, ou hébergé)

## 2. Installation

```bash
cd backend
npm install
cp .env.example .env
```

Ouvrez `.env` et renseignez vos identifiants PostgreSQL (`PGHOST`, `PGUSER`,
`PGPASSWORD`, `PGDATABASE`...) ainsi qu'un `JWT_SECRET` robuste.

## 3. Création de la base de données

Créez la base puis chargez le schéma :

```bash
createdb ciphertalk
psql -U postgres -d ciphertalk -f sql/schema.sql
```

(Adaptez `-U postgres` à votre utilisateur PostgreSQL.)

## 4. Lancement du serveur

```bash
npm run dev      # avec rechargement automatique (nodemon)
# ou
npm start
```

Le serveur écoute par défaut sur `http://localhost:4000` :
- API REST : `http://localhost:4000/api/...`
- WebSocket temps réel : `ws://localhost:4000/ws?token=VOTRE_JWT`

## 5. Tester rapidement avec curl

```bash
# Inscription
curl -X POST http://localhost:4000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"motdepasse123"}'

# Connexion
curl -X POST http://localhost:4000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"motdepasse123"}'
```

## 6. Rendre le serveur accessible depuis votre téléphone / l'émulateur Android

- **Émulateur Android Studio** : utilisez `http://10.0.2.2:4000/` comme `BASE_URL`
  côté application (10.0.2.2 pointe vers le `localhost` de votre PC depuis l'émulateur).
- **Téléphone physique sur le même Wi-Fi** : utilisez l'adresse IP locale de votre PC,
  ex. `http://192.168.1.42:4000/`, et assurez-vous que le pare-feu autorise le port 4000.
- **Déploiement en ligne** (recommandé pour un usage "production" entre plusieurs
  personnes réellement distantes) : déployez ce dossier `backend/` sur un service comme
  Render, Railway, Fly.io ou une VM avec une base PostgreSQL managée, puis utilisez
  l'URL HTTPS publique comme `BASE_URL`.

## Structure du projet

```
backend/
├── sql/schema.sql            Schéma PostgreSQL (tables users, conversations, messages...)
├── src/
│   ├── server.js             Point d'entrée HTTP + WebSocket
│   ├── config.js             Lecture des variables d'environnement
│   ├── db.js                 Pool de connexions PostgreSQL
│   ├── controllers/          Logique métier de chaque ressource
│   ├── routes/                Définition des routes REST
│   ├── middleware/            Authentification JWT, gestion d'erreurs
│   ├── utils/                 Chiffrement affine, hash mots de passe, JWT
│   └── ws/                    Serveur WebSocket (présence, diffusion des messages)
└── package.json
```
