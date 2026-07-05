# CipherTalk — Messagerie mobile à chiffrement affine

Version mobile Android de votre démo web "CipherTalk", transformée en véritable
application de messagerie **multi-utilisateurs** : n'importe quel nombre de
personnes peut créer un compte, discuter en temps réel, et chaque message est
**stocké dans une vraie base PostgreSQL** sur votre serveur.

```
┌─────────────────────┐        REST + WebSocket        ┌──────────────────────┐
│   Application        │ ◄─────────────────────────────► │   Backend Node.js     │
│   Android (Kotlin)    │        (JSON / JWT)             │   Express + ws        │
│   android/             │                                 │   backend/             │
└─────────────────────┘                                 └──────────┬───────────┘
                                                                     │ SQL
                                                          ┌──────────▼───────────┐
                                                          │   PostgreSQL           │
                                                          │   (vos messages)       │
                                                          └────────────────────────┘
```

## Contenu du projet

```
CipherTalkProject/
├── backend/     Serveur Node.js / Express / PostgreSQL / WebSocket (API + temps réel)
└── android/     Application Android (Kotlin) à ouvrir dans Android Studio
```

Chaque dossier a son propre `README.md` avec les instructions détaillées :
- [`backend/README.md`](backend/README.md) — installation et lancement du serveur
- Instructions Android ci-dessous

## Démarrage rapide

### 1. Backend (à faire en premier)

```bash
cd backend
npm install
cp .env.example .env        # puis renseignez vos identifiants PostgreSQL dans .env
psql -U postgres -c "CREATE DATABASE ciphertalk;"
psql -U postgres -d ciphertalk -f sql/schema.sql
npm run dev
```

Le serveur tourne alors sur `http://localhost:4000`.

### 2. Application Android

1. Ouvrez **Android Studio** → `File > Open...` → sélectionnez le dossier `android/`.
2. Laissez Android Studio synchroniser Gradle (la première fois, il télécharge les
   dépendances : Internet requis).
3. Ouvrez `app/build.gradle.kts` et adaptez `BASE_URL` selon votre cas :
   - Émulateur Android Studio → déjà configuré sur `http://10.0.2.2:4000/`
   - Téléphone physique sur le même Wi-Fi → mettez l'IP locale de votre PC,
     ex. `http://192.168.1.42:4000/`
   - Serveur déployé en ligne → votre URL HTTPS publique
4. Cliquez sur ▶ **Run** pour lancer l'application sur un émulateur ou un téléphone
   connecté en USB (mode développeur + débogage USB activés).

Deux personnes (ou plus) peuvent alors créer chacune un compte depuis leur propre
téléphone (ou un émulateur différent) et discuter ensemble, à condition que les deux
appareils puissent atteindre votre serveur backend (même réseau, ou serveur déployé
en ligne pour un usage à distance réel).

## Fonctionnalités

Reprises de la démo web d'origine :
- Chiffrement affine `E(x) = (a·x + b) mod 26`, clé partagée modifiable, table de
  substitution, génération de clé aléatoire valide.
- Interface sombre, bulles "texte en clair" + "texte chiffré" pour chaque message.

Ajoutées pour en faire une vraie application multi-utilisateurs et "plus pratique" :
- **Comptes utilisateurs réels** (inscription / connexion sécurisée, mots de passe
  hachés avec bcrypt, sessions par token JWT) — au lieu des deux comptes figés
  Alice/Bob.
- **Stockage persistant dans PostgreSQL** : historique des conversations conservé
  côté serveur, accessible depuis n'importe quel appareil connecté au compte.
- **Recherche d'utilisateurs** pour démarrer une nouvelle conversation avec
  n'importe quelle personne inscrite.
- **Messagerie en temps réel** via WebSocket (les messages arrivent instantanément,
  sans avoir à rafraîchir).
- **Indicateur "en ligne / hors ligne"** et **horodatage du dernier message vu**.
- **Indicateur de frappe** ("... est en train d'écrire").
- **Accusés de lecture** (✓ envoyé / ✓✓ lu).
- **Compteur de messages non lus** par conversation.
- **Suppression de message** (par son auteur).
- **Clé par message** : changer la clé partagée en cours de conversation n'empêche
  plus de relire les anciens messages — chaque message garde la clé utilisée au
  moment de son envoi.
- Architecture client/serveur professionnelle (REST + WebSocket, JWT, ViewBinding,
  coroutines Kotlin, Retrofit/OkHttp) au lieu de tout exécuter dans une page HTML
  locale.

## Notes de sécurité

Le chiffrement affine est conservé pour son intérêt pédagogique (il provient de
votre démo d'origine), mais ce **n'est pas un chiffrement sécurisé** au sens
moderne (26 lettres seulement, cassable en quelques secondes par analyse de
fréquence). Pour un usage réellement confidentiel en production, il faudrait :
- Servir le backend en HTTPS (ex. via un certificat Let's Encrypt) plutôt qu'en
  HTTP en clair ;
- Remplacer/compléter le chiffrement affine par un chiffrement de bout en bout
  moderne (AES-256, libsignal, etc.).

Le projet est entièrement fonctionnel tel quel pour un usage entre amis, en
réseau local ou via un petit serveur déployé en ligne — la partie "backend +
PostgreSQL + temps réel" a été testée de bout en bout pendant le développement
(inscription, connexion, conversations, envoi/réception, présence, changement de
clé, suppression).
