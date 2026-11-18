package fr.donner;

import fr.donner.domain.DeliveryMode;
import fr.donner.domain.ItemState;
import fr.donner.domain.Listing;
import fr.donner.domain.Person;
import fr.donner.repo.ListingRepository;
import fr.donner.repo.PersonRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
class SeedData {
    private static final String PHOTO_LAMP = "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_BOOK = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_BOOKS = "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_STOOL = "https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_TABLE = "https://images.unsplash.com/photo-1533090481720-856c6e3c1fdc?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_PLANT = "https://images.unsplash.com/photo-1485955900006-10f4d324d411?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_POTS = "https://images.unsplash.com/photo-1463320726281-696a485928c7?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_GARDEN = "https://images.unsplash.com/photo-1416879595882-3373a0480b5b?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_BIKE = "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_COFFEE = "https://images.unsplash.com/photo-1517705008128-361805f42e86?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_GUITAR = "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_MIC = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_PIANO = "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_CAMERA = "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_SOFA = "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_ARMCHAIR = "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_CHAIR = "https://images.unsplash.com/photo-1598300042247-d088f8ab3a91?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_MUGS = "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_BAG = "https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_BACKPACK = "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_WATCH = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_HEADPHONES = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_SNEAKERS = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_SHELF = "https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_FRAME = "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_MODEL_CAR = "https://images.unsplash.com/photo-1581235720704-06d3acfcb36f?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_NIGHTSTAND = "https://images.unsplash.com/photo-1519710164239-da123dc03ef4?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_DESK = "https://images.unsplash.com/photo-1524758631624-e2822e304c36?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_CUSHIONS = "https://images.unsplash.com/photo-1493663284031-b7e3aefcae8e?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_MOUSE = "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_PHONE = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_LAPTOP = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_SPEAKER = "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_DECOR = "https://images.unsplash.com/photo-1583847268964-b28dc8f51f92?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_CONTROLLER = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_KITCHEN = "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_MIXER = "https://images.unsplash.com/photo-1585515320310-259814833e62?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_APPLIANCE = "https://images.unsplash.com/photo-1574269909862-7e1d70bb8078?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_SWEATER = "https://images.unsplash.com/photo-1434389677669-e08b4cac3105?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_JACKET = "https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_TSHIRT = "https://images.unsplash.com/photo-1576566588028-4147f3842f27?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_TOOLS = "https://images.unsplash.com/photo-1530124566582-a618bc2615dc?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_DRILL = "https://images.unsplash.com/photo-1572981779307-38b8cabb2407?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_SPORT = "https://images.unsplash.com/photo-1517649763962-0c623066013b?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_YOGA = "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_TOYS = "https://images.unsplash.com/photo-1566576912321-d58ddd7a6088?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_BLOCKS = "https://images.unsplash.com/photo-1596461404969-9ae70f2830c1?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_BABY = "https://images.unsplash.com/photo-1515488042361-ee00e0ddd4e4?auto=format&fit=crop&w=800&q=80";
    private static final String PHOTO_CARE = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?auto=format&fit=crop&w=800&q=80";

    private record SeedListing(
            String title,
            String description,
            ItemState state,
            String area,
            DeliveryMode deliveryMode,
            Set<String> keywords,
            String imageUrl) {
    }

    @Bean
    CommandLineRunner seed(PersonRepository people, ListingRepository listings, PasswordEncoder passwordEncoder) {
        return args -> {
            String defaultHash = passwordEncoder.encode("password");

            Person camille = ensurePerson(people, "Camille", "camille@example.fr", defaultHash);
            Person yanis = ensurePerson(people, "Yanis", "yanis@example.fr", defaultHash);

            people.findAll().forEach(person -> {
                if (person.getPasswordHash() == null || person.getPasswordHash().isBlank()) {
                    person.setPasswordHash(defaultHash);
                    people.save(person);
                }
            });

            Person ines = ensurePerson(people, "Inès", "ines@example.fr", defaultHash);
            Person lucas = ensurePerson(people, "Lucas", "lucas@example.fr", defaultHash);
            Person chloe = ensurePerson(people, "Chloé", "chloe@example.fr", defaultHash);
            Person mehdi = ensurePerson(people, "Mehdi", "mehdi@example.fr", defaultHash);
            Person lea = ensurePerson(people, "Léa", "lea@example.fr", defaultHash);
            Person thomas = ensurePerson(people, "Thomas", "thomas@example.fr", defaultHash);
            List<Person> donors = List.of(camille, yanis, ines, lucas, chloe, mehdi, lea, thomas);

            if (listings.count() == 0) {
                Listing first = new Listing(
                        "Lampe de bureau vintage",
                        "Lampe fonctionnelle, ampoule incluse.",
                        ItemState.BON,
                        "Lyon",
                        DeliveryMode.MAIN_PROPRE,
                        camille,
                        Set.of("maison", "lumière"),
                        PHOTO_LAMP);
                Listing second = new Listing(
                        "Roman — Le Petit Prince",
                        "Livre en très bon état.",
                        ItemState.TRES_BON,
                        "Villeurbanne",
                        DeliveryMode.LES_DEUX,
                        yanis,
                        Set.of("livre", "culture"),
                        PHOTO_BOOK);
                listings.save(first);
                listings.save(second);
            }

            List<Listing> existing = listings.findAll();
            Set<String> existingTitles = new HashSet<>();
            for (Listing item : existing) {
                existingTitles.add(item.getTitle());
                if (item.getImageUrl() == null || item.getImageUrl().isBlank()) {
                    item.setImageUrl(defaultPhotoFor(item));
                    listings.save(item);
                }
            }

            List<SeedListing> extra = extraListings();
            Instant baseTime = Instant.now();
            for (int i = 0; i < extra.size(); i++) {
                SeedListing spec = extra.get(i);
                if (!existingTitles.contains(spec.title())) {
                    Person owner = donors.get(i % donors.size());
                    Listing created = new Listing(
                            spec.title(),
                            spec.description(),
                            spec.state(),
                            spec.area(),
                            spec.deliveryMode(),
                            owner,
                            spec.keywords(),
                            spec.imageUrl());
                    created.setPublishedAt(baseTime.minus(i + 1L, ChronoUnit.HOURS));
                    listings.save(created);
                }
            }
        };
    }

    private static Person ensurePerson(PersonRepository people, String name, String email, String defaultHash) {
        return people.findByEmailIgnoreCase(email)
                .orElseGet(() -> people.save(new Person(name, email, defaultHash)));
    }

    private static String defaultPhotoFor(Listing listing) {
        String title = listing.getTitle() == null ? "" : listing.getTitle().toLowerCase();
        if (title.contains("lampe")) {
            return PHOTO_LAMP;
        }
        if (title.contains("petit prince") || title.contains("roman") || title.contains("livre")) {
            return PHOTO_BOOK;
        }
        return PHOTO_DECOR;
    }

    private static List<SeedListing> extraListings() {
        return List.of(
                // 1-10 : Mobilier & Décoration
                new SeedListing("Fauteuil scandinave tissu sauge", "Assise confortable et pieds en hêtre massif, idéal pour un coin lecture.", ItemState.TRES_BON, "Lyon", DeliveryMode.MAIN_PROPRE, Set.of("maison", "meuble", "salon"), PHOTO_ARMCHAIR),
                new SeedListing("Canapé 3 places velours vert", "Canapé propre sans tache ni déchirure, coussins déhoussables.", ItemState.BON, "Villeurbanne", DeliveryMode.MAIN_PROPRE, Set.of("maison", "salon", "canapé"), PHOTO_SOFA),
                new SeedListing("Table basse ronde en chêne", "Diamètre 80 cm, quelques micro-rayures d’usage sur le plateau.", ItemState.BON, "Lyon", DeliveryMode.MAIN_PROPRE, Set.of("maison", "meuble", "bois"), PHOTO_TABLE),
                new SeedListing("Tabouret de bar en bois brut", "Hauteur 65 cm, très stable, parfait pour un îlot de cuisine.", ItemState.TRES_BON, "Nantes", DeliveryMode.MAIN_PROPRE, Set.of("maison", "cuisine", "bois"), PHOTO_STOOL),
                new SeedListing("Chaise design coque blanche", "Chaise légère avec piètement métal et bois, peu utilisée.", ItemState.NEUF, "Paris", DeliveryMode.MAIN_PROPRE, Set.of("maison", "bureau", "chaise"), PHOTO_CHAIR),
                new SeedListing("Étagère murale 4 niveaux", "Bibliothèque ouverte facile à remonter, visserie fournie.", ItemState.TRES_BON, "Bordeaux", DeliveryMode.MAIN_PROPRE, Set.of("maison", "rangement", "meuble"), PHOTO_SHELF),
                new SeedListing("Table de chevet minimaliste", "Petit meuble à tiroir coulissant, couleur bois clair.", ItemState.BON, "Toulouse", DeliveryMode.MAIN_PROPRE, Set.of("maison", "chambre", "bois"), PHOTO_NIGHTSTAND),
                new SeedListing("Bureau télétravail plateau clair", "Largeur 120 cm avec passe-câbles intégré, démontable.", ItemState.TRES_BON, "Lyon", DeliveryMode.MAIN_PROPRE, Set.of("maison", "bureau", "télétravail"), PHOTO_DESK),
                new SeedListing("Lot de 3 cadres photo en bois", "Formats A4 et 30x40 cm avec passe-partout blancs.", ItemState.NEUF, "Lille", DeliveryMode.LES_DEUX, Set.of("maison", "décoration", "cadre"), PHOTO_FRAME),
                new SeedListing("Lot de 4 coussins déco lin", "Housses lavables en machine, tons naturels et terracotta.", ItemState.TRES_BON, "Strasbourg", DeliveryMode.ENVOI, Set.of("maison", "décoration", "salon"), PHOTO_CUSHIONS),

                // 11-20 : Livres, BD & Culture
                new SeedListing("Coffret trilogie Le Seigneur des Anneaux", "Édition brochée complète, pages propres sans annotation.", ItemState.TRES_BON, "Villeurbanne", DeliveryMode.LES_DEUX, Set.of("livre", "culture", "roman"), PHOTO_BOOKS),
                new SeedListing("Roman — L’Étranger d’Albert Camus", "Format poche en excellent état, lu une seule fois.", ItemState.TRES_BON, "Lyon", DeliveryMode.ENVOI, Set.of("livre", "culture", "classique"), PHOTO_BOOK),
                new SeedListing("Lot de 5 romans policiers français", "Sélection de thrillers récents pour les vacances.", ItemState.BON, "Marseille", DeliveryMode.LES_DEUX, Set.of("livre", "polar", "lecture"), PHOTO_BOOKS),
                new SeedListing("Atlas géographique du monde illustré", "Grand format relié avec cartes détaillées et photos.", ItemState.TRES_BON, "Rennes", DeliveryMode.LES_DEUX, Set.of("livre", "culture", "géographie"), PHOTO_BOOK),
                new SeedListing("Intégrale BD Astérix — 4 albums", "Albums cartonnés bien conservés, coins intacts.", ItemState.BON, "Lyon", DeliveryMode.MAIN_PROPRE, Set.of("livre", "bd", "culture"), PHOTO_BOOKS),
                new SeedListing("Manuel d’apprentissage Java & Spring", "Ouvrage technique complet avec exercices corrigés.", ItemState.NEUF, "Villeurbanne", DeliveryMode.LES_DEUX, Set.of("livre", "informatique", "études"), PHOTO_BOOK),
                new SeedListing("Recueil de recettes végétariennes de saison", "120 recettes illustrées pas à pas, couverture rigide.", ItemState.TRES_BON, "Grenoble", DeliveryMode.ENVOI, Set.of("livre", "cuisine", "recette"), PHOTO_BOOKS),
                new SeedListing("Lot de 6 magazines design & architecture", "Numéros récents en parfait état pour inspiration ou collage.", ItemState.BON, "Paris", DeliveryMode.LES_DEUX, Set.of("livre", "design", "culture"), PHOTO_BOOK),
                new SeedListing("Dictionnaire encyclopédique Larousse", "Édition récente peu manipulée, idéal collégien ou lycéen.", ItemState.TRES_BON, "Montpellier", DeliveryMode.MAIN_PROPRE, Set.of("livre", "études", "culture"), PHOTO_BOOKS),
                new SeedListing("Essai d’histoire contemporaine", "Grand format relié, aucune page cornée.", ItemState.BON, "Le Havre", DeliveryMode.ENVOI, Set.of("livre", "histoire", "culture"), PHOTO_BOOK),

                // 21-30 : Cuisine & Électroménager
                new SeedListing("Cafetière piston verre borosilicate", "Capacité 1 litre (8 tasses), filtre inox nettoyé.", ItemState.TRES_BON, "Lyon", DeliveryMode.LES_DEUX, Set.of("cuisine", "maison", "café"), PHOTO_COFFEE),
                new SeedListing("Service de 6 tasses en grès artisanal", "Coloris crème et terre cuite, compatibles lave-vaisselle.", ItemState.NEUF, "Nantes", DeliveryMode.MAIN_PROPRE, Set.of("cuisine", "vaisselle", "maison"), PHOTO_MUGS),
                new SeedListing("Robot pâtissier multifonction", "Bol inox 4,5 L avec fouet, crochet et feuille, fonctionne parfaitement.", ItemState.BON, "Bordeaux", DeliveryMode.MAIN_PROPRE, Set.of("cuisine", "électroménager", "pâtisserie"), PHOTO_MIXER),
                new SeedListing("Grille-pain 2 fentes inox brossé", "7 niveaux de brunissage et tiroir ramasse-miettes.", ItemState.TRES_BON, "Lyon", DeliveryMode.LES_DEUX, Set.of("cuisine", "électroménager"), PHOTO_APPLIANCE),
                new SeedListing("Lot d’ustensiles de cuisine en bois", "Spatules, cuillères et planche à découper en bambou.", ItemState.TRES_BON, "Villeurbanne", DeliveryMode.LES_DEUX, Set.of("cuisine", "maison", "bois"), PHOTO_KITCHEN),
                new SeedListing("Bouilloire électrique température réglable", "Capacité 1,7 L, arrêt automatique, détartrée récemment.", ItemState.BON, "Lille", DeliveryMode.MAIN_PROPRE, Set.of("cuisine", "thé", "électroménager"), PHOTO_COFFEE),
                new SeedListing("Blender smoothie 600 W avec bol verre", "Idéal soupes et jus, joint de rechange neuf inclus.", ItemState.TRES_BON, "Toulouse", DeliveryMode.MAIN_PROPRE, Set.of("cuisine", "électroménager"), PHOTO_MIXER),
                new SeedListing("Lot de 4 mugs en céramique mate", "Contenance 350 ml, aucun éclat.", ItemState.NEUF, "Strasbourg", DeliveryMode.LES_DEUX, Set.of("cuisine", "vaisselle"), PHOTO_MUGS),
                new SeedListing("Batterie de 3 casseroles induction", "Poignées amovibles, fond épais compatible tous feux.", ItemState.BON, "Paris", DeliveryMode.MAIN_PROPRE, Set.of("cuisine", "maison"), PHOTO_KITCHEN),
                new SeedListing("Machine espresso à réparer (pompe)", "S’allume mais manque de pression, idéale bricoleur ou pièces.", ItemState.A_REPARER, "Marseille", DeliveryMode.MAIN_PROPRE, Set.of("cuisine", "café", "bricolage"), PHOTO_APPLIANCE),

                // 31-40 : Informatique & High-Tech
                new SeedListing("Souris sans fil ergonomique USB-C", "Batterie longue durée, capteur précis multi-surfaces.", ItemState.TRES_BON, "Lyon", DeliveryMode.ENVOI, Set.of("informatique", "bureau", "tech"), PHOTO_MOUSE),
                new SeedListing("Ordinateur portable 14 pouces Linux", "8 Go RAM, SSD 256 Go, batterie tient environ 4 h, chargeur inclus.", ItemState.BON, "Villeurbanne", DeliveryMode.MAIN_PROPRE, Set.of("informatique", "tech", "études"), PHOTO_LAPTOP),
                new SeedListing("Smartphone reconditionné 64 Go débloqué", "Écran protégé par verre trempé, idéal téléphone d’appoint.", ItemState.BON, "Paris", DeliveryMode.LES_DEUX, Set.of("tech", "téléphone", "mobile"), PHOTO_PHONE),
                new SeedListing("Appareil photo argentique vintage avec objectif", "Boîtier mécanique révisé avec sangle cuir.", ItemState.BON, "Bordeaux", DeliveryMode.LES_DEUX, Set.of("photo", "vintage", "tech"), PHOTO_CAMERA),
                new SeedListing("Enceinte connectée compacte Wi-Fi / Bluetooth", "Son clair pour une chambre ou un bureau, câble fourni.", ItemState.TRES_BON, "Lyon", DeliveryMode.LES_DEUX, Set.of("audio", "tech", "maison"), PHOTO_SPEAKER),
                new SeedListing("Manette sans fil multi-plateforme", "Gâchettes réactives, fonctionne sur PC et console.", ItemState.TRES_BON, "Rennes", DeliveryMode.ENVOI, Set.of("jeux", "tech", "console"), PHOTO_CONTROLLER),
                new SeedListing("Casque audio Bluetooth réduction de bruit", "Coussinets propres, câble jack et étui inclus.", ItemState.TRES_BON, "Nantes", DeliveryMode.LES_DEUX, Set.of("audio", "tech", "musique"), PHOTO_HEADPHONES),
                new SeedListing("Support ventilé réglable pour ordinateur", "Aluminium pliable à 6 hauteurs pour soulager la nuque.", ItemState.NEUF, "Toulouse", DeliveryMode.ENVOI, Set.of("informatique", "bureau"), PHOTO_LAPTOP),
                new SeedListing("Appareil photo compact numérique 20 MP", "Livré avec carte SD 32 Go et batterie supplémentaire.", ItemState.BON, "Grenoble", DeliveryMode.LES_DEUX, Set.of("photo", "tech"), PHOTO_CAMERA),
                new SeedListing("Vieille souris filaire + tapis ergonomique", "Matériel propre et fonctionnel pour poste fixe.", ItemState.BON, "Le Havre", DeliveryMode.ENVOI, Set.of("informatique", "bureau"), PHOTO_MOUSE),

                // 41-50 : Musique & Audio
                new SeedListing("Guitare acoustique folk pour débutant", "Cordes récentes, housse souple et médiators fournis.", ItemState.BON, "Lyon", DeliveryMode.MAIN_PROPRE, Set.of("musique", "guitare", "loisirs"), PHOTO_GUITAR),
                new SeedListing("Clavier numérique 61 touches sensibles", "Avec pupitre et alimentation secteur, parfait pour débuter.", ItemState.TRES_BON, "Villeurbanne", DeliveryMode.MAIN_PROPRE, Set.of("musique", "piano", "instrument"), PHOTO_PIANO),
                new SeedListing("Microphone statique USB pour podcast", "Avec trépied de table et filtre anti-pop.", ItemState.NEUF, "Paris", DeliveryMode.LES_DEUX, Set.of("musique", "audio", "podcast"), PHOTO_MIC),
                new SeedListing("Casque studio fermé filaire 3m", "Adaptateur 6,35 mm inclus, son neutre pour montage.", ItemState.BON, "Lille", DeliveryMode.ENVOI, Set.of("musique", "audio"), PHOTO_HEADPHONES),
                new SeedListing("Enceinte nomade étanche 12 h d’autonomie", "Pratique pour pique-niques et sorties en plein air.", ItemState.TRES_BON, "Montpellier", DeliveryMode.LES_DEUX, Set.of("musique", "audio", "plein-air"), PHOTO_SPEAKER),
                new SeedListing("Guitare classique 3/4 enfant", "Manche droit, idéale pour élève de conservatoire 8-12 ans.", ItemState.BON, "Strasbourg", DeliveryMode.MAIN_PROPRE, Set.of("musique", "guitare", "enfant"), PHOTO_GUITAR),
                new SeedListing("Pied de micro perche réglable", "Acier stable avec pince universelle.", ItemState.TRES_BON, "Bordeaux", DeliveryMode.MAIN_PROPRE, Set.of("musique", "audio"), PHOTO_MIC),
                new SeedListing("Banquette de piano réglable en hauteur", "Assise simili cuir noir sans accroc.", ItemState.BON, "Lyon", DeliveryMode.MAIN_PROPRE, Set.of("musique", "piano", "meuble"), PHOTO_PIANO),
                new SeedListing("Accordeur à pince + capodastre métal", "Accessoires indispensables pour guitare ou ukulélé.", ItemState.NEUF, "Nantes", DeliveryMode.ENVOI, Set.of("musique", "guitare"), PHOTO_GUITAR),
                new SeedListing("Amplificateur guitare 15 W à réviser", "Faux contact sur le potentiomètre de volume, facile à souder.", ItemState.A_REPARER, "Marseille", DeliveryMode.MAIN_PROPRE, Set.of("musique", "bricolage"), PHOTO_SPEAKER),

                // 51-60 : Sport & Mobilité
                new SeedListing("Vélo de ville vintage taille M", "Pneus neufs, dynamo et porte-bagages arrière fonctionnels.", ItemState.BON, "Lyon", DeliveryMode.MAIN_PROPRE, Set.of("sport", "vélo", "mobilité"), PHOTO_BIKE),
                new SeedListing("Tapis de yoga antidérapant 6 mm + sangle", "Nettoyé après chaque séance, matière liège et caoutchouc.", ItemState.TRES_BON, "Villeurbanne", DeliveryMode.LES_DEUX, Set.of("sport", "yoga", "bien-être"), PHOTO_YOGA),
                new SeedListing("Paire de baskets de running pointure 42", "Portées 3 fois sur piste, semelle impeccable.", ItemState.NEUF, "Paris", DeliveryMode.LES_DEUX, Set.of("sport", "chaussures", "course"), PHOTO_SNEAKERS),
                new SeedListing("Sac à dos de randonnée 35 L", "Dos ventilé avec housse de pluie intégrée, toutes boucles intactes.", ItemState.TRES_BON, "Grenoble", DeliveryMode.LES_DEUX, Set.of("sport", "randonnée", "voyage"), PHOTO_BACKPACK),
                new SeedListing("Kit haltères modulables + corde à sauter", "2 x 5 kg avec poignées ergonomiques.", ItemState.BON, "Toulouse", DeliveryMode.MAIN_PROPRE, Set.of("sport", "fitness"), PHOTO_SPORT),
                new SeedListing("Vélo route cadre aluminium à réviser", "Dérailleur arrière à régler, roue avant voilée légère.", ItemState.A_REPARER, "Lyon", DeliveryMode.MAIN_PROPRE, Set.of("sport", "vélo", "bricolage"), PHOTO_BIKE),
                new SeedListing("Montre connectée suivi cardio & course", "Bracelet silicone neuf, chargeur aimanté inclus.", ItemState.TRES_BON, "Nantes", DeliveryMode.ENVOI, Set.of("sport", "tech", "montre"), PHOTO_WATCH),
                new SeedListing("Bloc de yoga en liège + élastiques", "Lot complet pour étirements et renforcement à la maison.", ItemState.NEUF, "Rennes", DeliveryMode.LES_DEUX, Set.of("sport", "yoga"), PHOTO_YOGA),
                new SeedListing("Casque vélo urbain taille M/L", "Avec feu arrière LED intégré, jamais chuté.", ItemState.TRES_BON, "Bordeaux", DeliveryMode.LES_DEUX, Set.of("sport", "vélo", "sécurité"), PHOTO_SPORT),
                new SeedListing("Baskets trail pointure 39", "Bon amorti, encore beaucoup de kilomètres devant elles.", ItemState.BON, "Lille", DeliveryMode.ENVOI, Set.of("sport", "chaussures"), PHOTO_SNEAKERS),

                // 61-70 : Jardin & Plantes
                new SeedListing("Monstera deliciosa en pot terre cuite", "Plante vigoureuse de 70 cm, feuillage sain sans parasite.", ItemState.TRES_BON, "Lyon", DeliveryMode.MAIN_PROPRE, Set.of("plante", "maison", "jardin"), PHOTO_PLANT),
                new SeedListing("Trio de cactus et succulentes en pots", "Peu d’entretien, idéal pour rebord de fenêtre lumineux.", ItemState.NEUF, "Villeurbanne", DeliveryMode.MAIN_PROPRE, Set.of("plante", "décoration", "maison"), PHOTO_POTS),
                new SeedListing("Arrosoir métal galvanisé 5 L + sécateur", "Matériel robuste pour balcon ou petit jardin.", ItemState.BON, "Nantes", DeliveryMode.MAIN_PROPRE, Set.of("jardin", "outils", "maison"), PHOTO_GARDEN),
                new SeedListing("Ficus elastica hauteur 90 cm", "Avec son cache-pot tressé, à récupérer sur place.", ItemState.TRES_BON, "Bordeaux", DeliveryMode.MAIN_PROPRE, Set.of("plante", "maison"), PHOTO_PLANT),
                new SeedListing("Lot de 6 pots en céramique percés", "Diamètres variés de 12 à 20 cm avec soucoupes.", ItemState.TRES_BON, "Toulouse", DeliveryMode.MAIN_PROPRE, Set.of("plante", "jardin", "décoration"), PHOTO_POTS),
                new SeedListing("Kit outils de rempotage 4 pièces", "Transplantoir, griffe, gants taille M et tapis de rempotage.", ItemState.NEUF, "Strasbourg", DeliveryMode.LES_DEUX, Set.of("jardin", "plante"), PHOTO_GARDEN),
                new SeedListing("Boutures de pothos doré enracinées", "3 belles tiges prêtes à planter en pot ou suspension.", ItemState.NEUF, "Lyon", DeliveryMode.MAIN_PROPRE, Set.of("plante", "maison"), PHOTO_PLANT),
                new SeedListing("Jardinière de balcon 60 cm avec crochets", "Résistante au gel, couleur vert sauge.", ItemState.BON, "Marseille", DeliveryMode.MAIN_PROPRE, Set.of("jardin", "balcon"), PHOTO_POTS),
                new SeedListing("Graines potagères bio (tomates, basilic, radis)", "Sachets non entamés valables jusqu’en 2028.", ItemState.NEUF, "Rennes", DeliveryMode.ENVOI, Set.of("jardin", "potager"), PHOTO_GARDEN),
                new SeedListing("Grand pot d’extérieur en terre cuite 40 cm", "Fêlure superficielle sans fuite, très décoratif.", ItemState.BON, "Le Havre", DeliveryMode.MAIN_PROPRE, Set.of("jardin", "plante"), PHOTO_POTS),

                // 71-80 : Bricolage & Outillage
                new SeedListing("Caisse à outils garnie 25 pièces", "Marteau, tournevis, pince multiprise et mètre ruban.", ItemState.BON, "Lyon", DeliveryMode.MAIN_PROPRE, Set.of("bricolage", "outils", "maison"), PHOTO_TOOLS),
                new SeedListing("Perceuse-visseuse sans fil 18 V + 2 batteries", "Mandrin 13 mm et mallette de rangement avec forets.", ItemState.TRES_BON, "Villeurbanne", DeliveryMode.MAIN_PROPRE, Set.of("bricolage", "outils"), PHOTO_DRILL),
                new SeedListing("Niveau à bulle 60 cm + équerre menuisier", "Outils précis pour pose d’étagères et cadres.", ItemState.NEUF, "Lille", DeliveryMode.LES_DEUX, Set.of("bricolage", "maison"), PHOTO_TOOLS),
                new SeedListing("Ponceuse vibrante avec feuilles abrasives", "Sac à poussière propre, fonctionne sur secteur 230 V.", ItemState.BON, "Grenoble", DeliveryMode.MAIN_PROPRE, Set.of("bricolage", "outils"), PHOTO_DRILL),
                new SeedListing("Assortiment chevilles et vis bois/placo", "Coffret compartimenté presque complet (plus de 300 pièces).", ItemState.NEUF, "Paris", DeliveryMode.LES_DEUX, Set.of("bricolage", "quincaillerie"), PHOTO_TOOLS),
                new SeedListing("Scie sauteuse à réparer (interrupteur)", "Moteur tourne par intermittence, lame incluse.", ItemState.A_REPARER, "Montpellier", DeliveryMode.MAIN_PROPRE, Set.of("bricolage", "outils"), PHOTO_DRILL),
                new SeedListing("Pince à dénuder et testeur de tension", "Indispensable pour petits travaux électriques.", ItemState.TRES_BON, "Nantes", DeliveryMode.ENVOI, Set.of("bricolage", "électricité"), PHOTO_TOOLS),
                new SeedListing("Lot de 4 serre-joints rapides 30 cm", "Mâchoires caoutchouc pour ne pas marquer le bois.", ItemState.TRES_BON, "Bordeaux", DeliveryMode.LES_DEUX, Set.of("bricolage", "menuiserie"), PHOTO_TOOLS),
                new SeedListing("Lampe frontale rechargeable chantier", "Faisceau large LED, pratique en cave ou grenier.", ItemState.NEUF, "Lyon", DeliveryMode.ENVOI, Set.of("bricolage", "lumière"), PHOTO_LAMP),
                new SeedListing("Visseuse compacte USB-C pour meubles en kit", "Embout aimanté et jeu de 10 embouts fourni.", ItemState.TRES_BON, "Toulouse", DeliveryMode.ENVOI, Set.of("bricolage", "maison"), PHOTO_DRILL),

                // 81-90 : Vêtements & Accessoires
                new SeedListing("Pull en laine mérinos beige taille M", "Doux et chaud, lavé à froid avec soin, aucune bouloche.", ItemState.TRES_BON, "Lyon", DeliveryMode.LES_DEUX, Set.of("vêtement", "mode", "hiver"), PHOTO_SWEATER),
                new SeedListing("Veste mi-saison coupe perfecto taille L", "Doublure impeccable, fermetures éclair fluides.", ItemState.TRES_BON, "Paris", DeliveryMode.LES_DEUX, Set.of("vêtement", "mode", "veste"), PHOTO_JACKET),
                new SeedListing("Lot de 3 t-shirts coton bio taille M", "Blanc, marine et olive, très peu portés.", ItemState.NEUF, "Villeurbanne", DeliveryMode.ENVOI, Set.of("vêtement", "mode"), PHOTO_TSHIRT),
                new SeedListing("Sac cabas en cuir camel spacieux", "Contient facilement un ordinateur 15 pouces et des dossiers.", ItemState.BON, "Bordeaux", DeliveryMode.LES_DEUX, Set.of("accessoire", "sac", "mode"), PHOTO_BAG),
                new SeedListing("Sac à dos urbain imperméable 20 L", "Compartiment rembourré pour PC portable.", ItemState.TRES_BON, "Lille", DeliveryMode.LES_DEUX, Set.of("accessoire", "sac", "bureau"), PHOTO_BACKPACK),
                new SeedListing("Montre analogique bracelet cuir noir", "Pile neuve posée ce mois-ci, cadran sans rayure.", ItemState.TRES_BON, "Nantes", DeliveryMode.ENVOI, Set.of("accessoire", "montre", "mode"), PHOTO_WATCH),
                new SeedListing("Trousse de toilette voyage compartimentée", "Multiples poches zippées et crochet de suspension.", ItemState.NEUF, "Strasbourg", DeliveryMode.ENVOI, Set.of("accessoire", "voyage"), PHOTO_CARE),
                new SeedListing("Gilet maille côtelée vert forêt taille S", "Boutons en bois, très confortable à la mi-saison.", ItemState.BON, "Rennes", DeliveryMode.ENVOI, Set.of("vêtement", "mode"), PHOTO_SWEATER),
                new SeedListing("Blouson coupe-vent déperlant taille M", "Capuche repliable dans le col, idéal vélo urbain.", ItemState.BON, "Lyon", DeliveryMode.LES_DEUX, Set.of("vêtement", "vélo", "mode"), PHOTO_JACKET),
                new SeedListing("Chemise en lin lavé blanc taille L", "Respirante pour l’été, propre et repassée.", ItemState.TRES_BON, "Marseille", DeliveryMode.ENVOI, Set.of("vêtement", "mode"), PHOTO_TSHIRT),

                // 91-100 : Jeux, Enfants & Loisirs
                new SeedListing("Jeu de construction bois 100 pièces", "Boîte de rangement incluse, peinture non toxique.", ItemState.TRES_BON, "Lyon", DeliveryMode.LES_DEUX, Set.of("jouet", "enfant", "bois"), PHOTO_BLOCKS),
                new SeedListing("Coffret voitures miniatures métal (6 pièces)", "Bon état général, idéal dès 3 ans.", ItemState.BON, "Villeurbanne", DeliveryMode.LES_DEUX, Set.of("jouet", "enfant"), PHOTO_MODEL_CAR),
                new SeedListing("Lot de jouets d’éveil bébé en bois", "Hochets, anneaux empilables et cube d’activités nettoyés.", ItemState.TRES_BON, "Nantes", DeliveryMode.LES_DEUX, Set.of("jouet", "bébé", "bois"), PHOTO_BABY),
                new SeedListing("Jeux de société familiaux — lot de 3 boîtes", "Jeux complets avec toutes les cartes, pions et notices.", ItemState.TRES_BON, "Toulouse", DeliveryMode.MAIN_PROPRE, Set.of("jeu", "loisirs", "famille"), PHOTO_TOYS),
                new SeedListing("Briques de construction créatives (vrac 2 kg)", "Sac nettoyé contenant briques classiques, roues et personnages.", ItemState.BON, "Paris", DeliveryMode.LES_DEUX, Set.of("jouet", "enfant", "construction"), PHOTO_BLOCKS),
                new SeedListing("Puzzle 1000 pièces paysage alpin", "Fait une seule fois, complet dans sa boîte d’origine.", ItemState.NEUF, "Grenoble", DeliveryMode.ENVOI, Set.of("jeu", "loisirs", "puzzle"), PHOTO_TOYS),
                new SeedListing("Voiture télécommandée avec batterie USB", "Portée 20 m, fonctionne en intérieur et sur terrasse.", ItemState.BON, "Bordeaux", DeliveryMode.LES_DEUX, Set.of("jouet", "enfant"), PHOTO_MODEL_CAR),
                new SeedListing("Mobile musical en bois pour lit bébé", "Mélodie douce mécanique sans pile, fixation universelle.", ItemState.TRES_BON, "Lille", DeliveryMode.MAIN_PROPRE, Set.of("bébé", "chambre", "bois"), PHOTO_BABY),
                new SeedListing("Lampe veilleuse LED forme nuage", "Lumière chaude réglable, câble USB inclus.", ItemState.NEUF, "Lyon", DeliveryMode.LES_DEUX, Set.of("enfant", "lumière", "maison"), PHOTO_LAMP),
                new SeedListing("Lot de 4 livres cartonnés première lecture", "Histoires illustrées pour enfants de 2 à 5 ans.", ItemState.TRES_BON, "Le Havre", DeliveryMode.ENVOI, Set.of("livre", "enfant", "culture"), PHOTO_BOOKS)
        );
    }
}
