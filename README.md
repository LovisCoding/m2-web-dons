# Donner - plateforme de dons

Application Spring Boot avec rendu Thymeleaf, API JSON/HAL, JPA et base H2 en développement.

## Lancer

```sh
./run.sh run
./run.sh test
./run.sh build
```

L’application est accessible sur <http://localhost:8080/annonces>. La console H2 est sur `/h2-console`.
La base locale persistante se trouve dans `./data/donner`. Pour configurer une base externe, fournir
`DATABASE_URL`, `DATABASE_USER` et `DATABASE_PASSWORD`. PostgreSQL est inclus comme pilote de production.
La configuration `server.forward-headers-strategy=framework` prend en charge les en-têtes `X-Forwarded-*`
transmis par un reverse proxy.

## Modèle

- `Person` publie des `Listing`.
- Une annonce porte son état, sa date, sa zone, son mode de remise et ses mots-clés.
- `Favorite` relie un profil à une annonce.
- `SavedSearch` mémorise les critères et le réglage des notifications.
- `ItemLot` regroupe les annonces d’un même propriétaire.
- `Message` relie expéditeur, destinataire et éventuellement une annonce.

## Routes

| Ressource | URL | Méthodes |
|---|---|---|
| Annonces HTML | `/annonces`, `/annonces/{id}` | GET |
| Création HTML | `/annonces/nouvelle`, `/annonces` | GET, POST |
| Actions annonce HTML | `/annonces/{id}/favoris`, `/messages`, `/modifier`, `/supprimer` | POST |
| Annonces API HAL/JSON | `/api/annonces`, `/api/annonces/{id}` | GET, POST, PUT, DELETE |
| Profils | `/api/personnes` | GET, POST |
| Favoris | `/api/personnes/{id}/favoris`, `/api/personnes/{id}/favoris/{annonceId}` | GET, POST, DELETE |
| Recherches sauvegardées | `/api/personnes/{id}/recherches`, `/api/recherches/{id}` | GET, POST, PUT, PATCH, DELETE |
| Lots | `/api/personnes/{id}/lots`, `/api/lots/{id}` | GET, POST, DELETE |
| Annonces d’un lot | `/api/lots/{lotId}/annonces/{annonceId}` | POST, DELETE |
| Messages | `/api/personnes/{id}/messages`, `/api/messages` | GET, POST |

Les recherches HTML acceptent `q`, `zone`, `etat` et `motCle`. L’API utilise `ETag`, `If-None-Match`
et une durée `Cache-Control` pour ses réponses GET. Les notifications sont simulées dans les journaux.

### Exemples

Créer une annonce :

```sh
curl -X POST http://localhost:8080/api/annonces \
  -H 'Content-Type: application/json' \
  -d '{"title":"Chaise en bois","description":"En bon état","state":"BON","area":"Lyon","deliveryMode":"MAIN_PROPRE","ownerId":1,"keywords":["meuble"]}'
```

Ajouter une annonce aux favoris d’un profil :

```sh
curl -X POST http://localhost:8080/api/personnes/2/favoris/1
```

Envoyer un message :

```sh
curl -X POST http://localhost:8080/api/messages \
  -H 'Content-Type: application/json' \
  -d '{"senderId":2,"recipientId":1,"listingId":1,"body":"Est-elle toujours disponible ?"}'
```

## Profils de démonstration et limites

Les données initiales créent Camille (ID 1) et Yanis (ID 2). Les formulaires et exemples utilisent ces profils
pour le TP. Il n’y a pas encore d’authentification : les identifiants `ownerId`/`personId` fournis par un client
ne prouvent pas son identité. Ne pas exposer cette version telle quelle en production.

## TODO restant

- [ ] Ajouter une authentification réelle et lier les autorisations à l’utilisateur connecté.
- [ ] Compléter les liens HAL et la négociation de contenu pour favoris, recherches, lots et messages.
- [ ] Ajouter pagination/tri et migrations de schéma versionnées.
- [ ] Étendre les tests d’intégration aux opérations d’écriture des annonces, favoris, recherches, lots et messages.
- [ ] Ajouter un Maven Wrapper si l’environnement de remise ne fournit pas Maven.
- [ ] Préparer l’archive ZIP avec sources, tests et historique Git.

Le PDF de synthèse est volontairement exclu à la demande de l’utilisateur.


# Sujet 

n se propose dans ce TP de réaliser une plateforme permettant à des personnes de donner des objets.
Spécification fonctionnelle

Une personne peut proposer des objets de différentes natures à donner, sous forme d'annonces. Une annonce coOmporte un titre, une description, l'état de l'objet, une date de publication, une zone géographique, si le don est en main propre ou peut être envoyé, ainsi qu'une liste de mots-clés.

On peut ensuite effectuer des recherches parmi les annonces, et les filtrer selon différents critères (zone, mots-clés, état, ...). Une recherche peut être sauvegardée si la personne utilisatrice est enregistrée, et une notification sera envoyée lors de l'apparition de nouvelles annonces correspondantes (cet aspect peut être désactivé).

Une annonce peut être mise en favoris. Il est également possible de créer des lots d'objets de la même personne pour une transaction groupée. Une messagerie interne permet de mettre au point le don.
Spécification technique

Nous allons réaliser ce système sous la forme d'une application autonome, c'est-à-dire qu'elle embarquera le serveur web. Ceci facilitera sa mise en place. Dans l'environnement de développement, la base de données sera aussi embarquée. Le système doit cependant être conçu de manière à permettre l'utilisation d'un serveur de données une fois en production si nécessaire. Le passage à l'échelle devra être pris en compte. Vous vous assurerez donc de gérer correctement les requêtes conditionnelles et les en-têtes de cache notamment, et de faciliter la répartition de charge. L'application devra donc facilement pouvoir être configurée pour être exposée via un reverse proxy.

L'application devra fournir une interface Web simple (HTML, rendu coté serveur).

On envisage cependant de pouvoir utiliser différentes applications en frontal (interface web, SPA/PWA, application mobile native, outil en ligne de commande, etc.) Vous veillerez donc à faciliter l'intégration de nouveaux client spécifiques (couplage faible par négociation de contenu et hypermédia par exemple).
Travail

Vous réaliserez ce projet en binôme, en utilisant le framework Spring Boot. Vous remettrez le code source de votre travail (application, tests unitaires et fonctionnels, etc.), ainsi que l'historique de version (le dossier .git). Ce code doit en outre contenir un script de compilation/exécution (maven, gradle, bash, ...) permettant de lancer facilement le logiciel et les tests.