# Donner — plateforme de dons

Application Spring Boot autonome avec rendu Thymeleaf, API JSON HAL, JPA et H2.

## Lancer

```sh
./run.sh run
```

Puis ouvrir http://localhost:8080. L’API des annonces est disponible sur `/api/annonces`.
Les commandes disponibles sont `./run.sh run`, `./run.sh test` et `./run.sh build`.

La base locale est stockée dans `./data/donner`. Pour une base de production, définir `DATABASE_URL`, `DATABASE_USER` et `DATABASE_PASSWORD`. `server.forward-headers-strategy=framework` permet l’exposition derrière un reverse proxy configuré pour transmettre `X-Forwarded-*`.

## Modèle

`Person` possède des `Listing`. Chaque annonce a son état, sa date, sa zone, son mode de remise et ses mots-clés. `Favorite` associe personne et annonce ; `SavedSearch` stocke les critères et le choix de notifications ; `ItemLot` regroupe des annonces d’un même propriétaire ; `Message` relie expéditeur, destinataire et éventuellement annonce.

## Ressources

| URL | Représentation | Méthodes |
|---|---|---|
| `/annonces` | HTML filtrable (`q`, `zone`, `etat`, `motCle`) | GET |
| `/annonces/{id}` | HTML annonce | GET |
| `/annonces/nouvelle`, `/annonces` | formulaire HTML | GET, POST |
| `/api/annonces` | JSON HAL et liens hypermédia | GET |

Les listes HTML et les données démo sont volontairement simples pour un TP : les formulaires utilisent les deux profils de démonstration et les actions favorites, recherches sauvegardées, lots et messages sont représentées par leurs entités JPA, sans interface de gestion encore exposée.

## Vérifications

```sh
mvn test
mvn spring-boot:run
```

## TODO 

### Fonctionnalités

- [ ] Ajouter l’authentification et l’inscription 
- [ ] Permettre aux utilisaters de modifier et supprimer leurs annonces, avec contrôle des droits.
- [ ] Ajouter la recherche par état au formulaire HTML et permettre de remettre les critères à zéro.
- [ ] Exposition CRUD des annonces via l’API.
- [ ] Ajouter la gestion des favoris 
- [ ] Ajouter la création, la consultation et la gestion des lots groupés, en appliquant la règle du propriétaire commun.
- [ ] Ajouter les conversations et l’envoi/consultation de messages entre utilisateurs.
- [ ] Ajouter la sauvegarde, la consultation, la modification et la suppression de recherches.
- [ ] Comparer les nouvelles annonces aux recherches sauvegardées et notifier les personnes concernées.
- [ ] Permettre d’activer et de désactiver les notifications par recherche.

### API, cache et exploitation

- [ ] Ajouter des représentations HAL (navigation entre les ressources) et des liens hypermédia pour les autres ressources,  la négociation de contenu pour les clients API.
- [ ] Ajouter pagination et tri aux listes d’annonces.
- [ ]  migrations de schéma de bdd.

### Qualité et remise du TP

- [ ] Ajouter une validation complète de l'app et des réponses d’erreur côté HTML et API.
- [ ] Rédiger le PDF demandé 
