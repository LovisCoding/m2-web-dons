package fr.donner.web;

import fr.donner.domain.DeliveryMode;
import fr.donner.domain.Favorite;
import fr.donner.domain.ItemLot;
import fr.donner.domain.ItemState;
import fr.donner.domain.Listing;
import fr.donner.domain.Message;
import fr.donner.domain.Person;
import fr.donner.domain.SavedSearch;
import fr.donner.repo.FavoriteRepository;
import fr.donner.repo.ItemLotRepository;
import fr.donner.repo.ListingRepository;
import fr.donner.repo.MessageRepository;
import fr.donner.repo.PersonRepository;
import fr.donner.repo.SavedSearchRepository;
import fr.donner.security.AuthService;
import fr.donner.service.ListingService;
import fr.donner.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@RestController
@RequestMapping(path = "/api", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
public class ApiController {
    private final PersonRepository people;
    private final ListingRepository listings;
    private final FavoriteRepository favorites;
    private final SavedSearchRepository searches;
    private final ItemLotRepository lots;
    private final MessageRepository messages;
    private final NotificationService notifications;
    private final ListingService listingService;
    private final AuthService authService;

    public ApiController(PersonRepository people, ListingRepository listings,
            FavoriteRepository favorites, SavedSearchRepository searches,
            ItemLotRepository lots, MessageRepository messages, NotificationService notifications,
            ListingService listingService, AuthService authService) {
        this.people = people;
        this.listings = listings;
        this.favorites = favorites;
        this.searches = searches;
        this.lots = lots;
        this.messages = messages;
        this.notifications = notifications;
        this.listingService = listingService;
        this.authService = authService;
    }

    @GetMapping("/annonces")
    public ResponseEntity<PagedModel<EntityModel<Listing>>> allListings(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) ItemState etat,
            @RequestParam(required = false) String motCle,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "publishedAt,desc") String sort) {
        var pageable = ListingQuerySupport.pageRequest(page, size, sort, 100);
        Page<Listing> result = listings.search(
                ListingQuerySupport.blank(q),
                ListingQuerySupport.blank(zone),
                etat,
                ListingQuerySupport.blank(motCle),
                pageable);

        List<EntityModel<Listing>> resources = result.getContent().stream()
                .map(this::toListingModel)
                .toList();

        List<Link> links = new ArrayList<>();
        links.add(Link.of(listingsPageUri("/api/annonces", q, zone, etat, motCle,
                result.getNumber(), result.getSize(), sort), "self"));
        links.add(Link.of(listingsPageUri("/annonces", q, zone, etat, motCle,
                result.getNumber(), result.getSize(), sort), "html"));
        links.add(Link.of(listingsPageUri("/api/annonces", q, zone, etat, motCle,
                0, result.getSize(), sort), "first"));
        if (result.hasPrevious()) {
            links.add(Link.of(listingsPageUri("/api/annonces", q, zone, etat, motCle,
                    result.getNumber() - 1, result.getSize(), sort), "prev"));
        }
        if (result.hasNext()) {
            links.add(Link.of(listingsPageUri("/api/annonces", q, zone, etat, motCle,
                    result.getNumber() + 1, result.getSize(), sort), "next"));
        }
        if (result.getTotalPages() > 0) {
            links.add(Link.of(listingsPageUri("/api/annonces", q, zone, etat, motCle,
                    result.getTotalPages() - 1, result.getSize(), sort), "last"));
        }

        PagedModel<EntityModel<Listing>> model = PagedModel.of(
                resources,
                new PagedModel.PageMetadata(
                        result.getSize(),
                        result.getNumber(),
                        result.getTotalElements(),
                        result.getTotalPages()),
                links);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
                .body(model);
    }

    @GetMapping("/annonces/{id}")
    public ResponseEntity<EntityModel<Listing>> listing(@PathVariable Long id) {
        Listing listing = findListing(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
                .body(toListingModel(listing));
    }

    @PostMapping(path = "/annonces", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<Listing>> createListing(
            @Valid @RequestBody ListingRequest request,
            Authentication authentication) {
        Person owner = authService.requireSamePerson(authentication, request.ownerId());
        Listing listing = listings.save(request.toListing(owner));
        notifications.notifyMatchingSearches(listing);
        return ResponseEntity.created(URI.create("/api/annonces/" + listing.getId()))
                .body(toListingModel(listing));
    }

    @PutMapping(path = "/annonces/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public EntityModel<Listing> updateListing(@PathVariable Long id,
            @RequestParam(required = false) Long ownerId,
            @Valid @RequestBody ListingRequest request,
            Authentication authentication) {
        Person connected = authService.requireSamePerson(authentication, ownerId);
        Listing listing = findListing(id);
        requireOwner(listing, connected.getId());
        if (request.ownerId() != null && !listing.getOwner().getId().equals(request.ownerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le propriétaire ne peut pas être modifié");
        }
        listing.update(request.title(), request.description(), request.state(), request.area(),
                request.deliveryMode(), request.keywords());
        return toListingModel(listing);
    }

    @DeleteMapping("/annonces/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id,
            @RequestParam(required = false) Long ownerId,
            Authentication authentication) {
        Person connected = authService.requireSamePerson(authentication, ownerId);
        Listing listing = findListing(id);
        requireOwner(listing, connected.getId());
        listingService.delete(listing);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/personnes")
    public CollectionModel<EntityModel<Person>> people() {
        List<EntityModel<Person>> resources = people.findAll().stream()
                .map(this::toPersonModel)
                .toList();
        return CollectionModel.of(resources,
                linkTo(ApiController.class).slash("personnes").withSelfRel());
    }

    @GetMapping("/personnes/{id}")
    public EntityModel<Person> person(@PathVariable Long id) {
        return toPersonModel(findPerson(id));
    }

    @PostMapping(path = "/personnes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<Person>> register(@Valid @RequestBody PersonRequest request) {
        Person person = authService.register(request.name(), request.email(), request.password());
        return ResponseEntity.created(URI.create("/api/personnes/" + person.getId()))
                .body(toPersonModel(person));
    }

    @GetMapping("/personnes/{personId}/favoris")
    public CollectionModel<EntityModel<Listing>> favorites(@PathVariable Long personId,
            Authentication authentication) {
        authService.requireSamePerson(authentication, personId);
        findPerson(personId);
        List<EntityModel<Listing>> resources = favorites.findByPersonId(personId).stream()
                .map(fav -> toFavoriteModel(personId, fav.getListing()))
                .toList();
        return CollectionModel.of(resources,
                linkTo(ApiController.class).slash("personnes").slash(personId).slash("favoris").withSelfRel(),
                linkTo(ApiController.class).slash("personnes").slash(personId).withRel("personne"));
    }

    @PostMapping("/personnes/{personId}/favoris/{listingId}")
    public ResponseEntity<EntityModel<Listing>> addFavorite(@PathVariable Long personId,
            @PathVariable Long listingId, Authentication authentication) {
        Person person = authService.requireSamePerson(authentication, personId);
        Listing listing = findListing(listingId);
        if (!favorites.existsByPersonIdAndListingId(personId, listingId)) {
            favorites.save(new Favorite(person, listing));
        }
        return ResponseEntity.ok(toFavoriteModel(personId, listing));
    }

    @DeleteMapping("/personnes/{personId}/favoris/{listingId}")
    @Transactional
    public ResponseEntity<Void> removeFavorite(@PathVariable Long personId,
            @PathVariable Long listingId, Authentication authentication) {
        authService.requireSamePerson(authentication, personId);
        findPerson(personId);
        favorites.deleteByPersonIdAndListingId(personId, listingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/personnes/{personId}/recherches")
    public CollectionModel<EntityModel<SavedSearch>> searches(@PathVariable Long personId,
            Authentication authentication) {
        authService.requireSamePerson(authentication, personId);
        findPerson(personId);
        List<EntityModel<SavedSearch>> resources = searches.findByPersonId(personId).stream()
                .map(this::toSearchModel)
                .toList();
        return CollectionModel.of(resources,
                linkTo(ApiController.class).slash("personnes").slash(personId).slash("recherches").withSelfRel(),
                linkTo(ApiController.class).slash("personnes").slash(personId).withRel("personne"));
    }

    @GetMapping("/recherches/{id}")
    public EntityModel<SavedSearch> search(@PathVariable Long id, Authentication authentication) {
        Person connected = authService.requirePerson(authentication);
        SavedSearch search = findSearch(id);
        requireSearchOwner(search, connected);
        return toSearchModel(search);
    }

    @PostMapping(path = "/personnes/{personId}/recherches", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<SavedSearch>> saveSearch(@PathVariable Long personId,
            @Valid @RequestBody SearchRequest request, Authentication authentication) {
        Person person = authService.requireSamePerson(authentication, personId);
        SavedSearch search = searches.save(new SavedSearch(person,
                ListingQuerySupport.blank(request.query()),
                ListingQuerySupport.blank(request.area()),
                request.state() == null ? null : request.state().name(),
                ListingQuerySupport.blank(request.keyword())));
        return ResponseEntity.created(URI.create("/api/recherches/" + search.getId()))
                .body(toSearchModel(search));
    }

    @PatchMapping("/recherches/{id}/notifications")
    @Transactional
    public EntityModel<SavedSearch> notifications(@PathVariable Long id,
            @RequestParam boolean enabled, Authentication authentication) {
        Person connected = authService.requirePerson(authentication);
        SavedSearch search = findSearch(id);
        requireSearchOwner(search, connected);
        search.setNotifications(enabled);
        return toSearchModel(search);
    }

    @PutMapping(path = "/recherches/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public EntityModel<SavedSearch> updateSearch(@PathVariable Long id,
            @RequestParam(required = false) Long personId,
            @Valid @RequestBody SearchRequest request, Authentication authentication) {
        Person connected = authService.requireSamePerson(authentication, personId);
        SavedSearch search = findSearch(id);
        requireSearchOwner(search, connected);
        search.update(
                ListingQuerySupport.blank(request.query()),
                ListingQuerySupport.blank(request.area()),
                request.state() == null ? null : request.state().name(),
                ListingQuerySupport.blank(request.keyword()));
        return toSearchModel(search);
    }

    @DeleteMapping("/recherches/{id}")
    public ResponseEntity<Void> deleteSearch(@PathVariable Long id,
            @RequestParam(required = false) Long personId, Authentication authentication) {
        Person connected = authService.requireSamePerson(authentication, personId);
        SavedSearch search = findSearch(id);
        requireSearchOwner(search, connected);
        searches.delete(search);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/personnes/{personId}/lots")
    public CollectionModel<EntityModel<ItemLot>> lots(@PathVariable Long personId) {
        findPerson(personId);
        List<EntityModel<ItemLot>> resources = lots.findByOwnerId(personId).stream()
                .map(this::toLotModel)
                .toList();
        return CollectionModel.of(resources,
                linkTo(ApiController.class).slash("personnes").slash(personId).slash("lots").withSelfRel(),
                linkTo(ApiController.class).slash("personnes").slash(personId).withRel("proprietaire"));
    }

    @GetMapping("/lots/{id}")
    public EntityModel<ItemLot> lot(@PathVariable Long id) {
        return toLotModel(findLot(id));
    }

    @PostMapping(path = "/personnes/{personId}/lots", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<ItemLot>> createLot(@PathVariable Long personId,
            @Valid @RequestBody LotRequest request, Authentication authentication) {
        Person owner = authService.requireSamePerson(authentication, personId);
        ItemLot lot = lots.save(new ItemLot(owner, request.name().trim()));
        return ResponseEntity.created(URI.create("/api/lots/" + lot.getId()))
                .body(toLotModel(lot));
    }

    @PostMapping("/lots/{lotId}/annonces/{listingId}")
    @Transactional
    public EntityModel<ItemLot> addToLot(@PathVariable Long lotId, @PathVariable Long listingId,
            @RequestParam(required = false) Long ownerId, Authentication authentication) {
        Person connected = authService.requireSamePerson(authentication, ownerId);
        ItemLot lot = findLot(lotId);
        requireLotOwner(lot, connected);
        try {
            lot.add(findListing(listingId));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return toLotModel(lots.save(lot));
    }

    @DeleteMapping("/lots/{lotId}/annonces/{listingId}")
    @Transactional
    public ResponseEntity<Void> removeFromLot(@PathVariable Long lotId, @PathVariable Long listingId,
            @RequestParam(required = false) Long ownerId, Authentication authentication) {
        Person connected = authService.requireSamePerson(authentication, ownerId);
        ItemLot lot = findLot(lotId);
        requireLotOwner(lot, connected);
        lot.getListings().removeIf(item -> item.getId().equals(listingId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/lots/{id}")
    public ResponseEntity<Void> deleteLot(@PathVariable Long id,
            @RequestParam(required = false) Long ownerId, Authentication authentication) {
        Person connected = authService.requireSamePerson(authentication, ownerId);
        ItemLot lot = findLot(id);
        requireLotOwner(lot, connected);
        lots.delete(lot);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/personnes/{personId}/messages")
    public CollectionModel<EntityModel<Message>> messages(@PathVariable Long personId,
            Authentication authentication) {
        authService.requireSamePerson(authentication, personId);
        findPerson(personId);
        List<EntityModel<Message>> resources = messages
                .findBySenderIdOrRecipientIdOrderBySentAtDesc(personId, personId).stream()
                .map(this::toMessageModel)
                .toList();
        return CollectionModel.of(resources,
                linkTo(ApiController.class).slash("personnes").slash(personId).slash("messages").withSelfRel(),
                linkTo(ApiController.class).slash("personnes").slash(personId).withRel("personne"));
    }

    @GetMapping("/messages/{id}")
    public EntityModel<Message> message(@PathVariable Long id, Authentication authentication) {
        Person connected = authService.requirePerson(authentication);
        Message message = findMessage(id);
        if (!message.getSender().getId().equals(connected.getId())
                && !message.getRecipient().getId().equals(connected.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return toMessageModel(message);
    }

    @PostMapping(path = "/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<Message>> sendMessage(@Valid @RequestBody MessageRequest request,
            Authentication authentication) {
        Person sender = authService.requireSamePerson(authentication, request.senderId());
        Person recipient = findPerson(request.recipientId());
        Listing listing = request.listingId() == null ? null : findListing(request.listingId());
        Message message = messages.save(new Message(sender, recipient, listing, request.body()));
        return ResponseEntity.created(URI.create("/api/messages/" + message.getId()))
                .body(toMessageModel(message));
    }

    private EntityModel<Listing> toListingModel(Listing listing) {
        Long id = listing.getId();
        Long ownerId = listing.getOwner().getId();
        return EntityModel.of(listing,
                linkTo(ApiController.class).slash("annonces").slash(id).withSelfRel(),
                linkTo(ListingController.class).slash("annonces").slash(id).withRel("html"),
                linkTo(ApiController.class).slash("annonces").withRel("collection"),
                linkTo(ApiController.class).slash("personnes").slash(ownerId).withRel("proprietaire"),
                linkTo(ApiController.class).slash("personnes").slash(ownerId)
                        .slash("favoris").slash(id).withRel("favoris"),
                linkTo(ApiController.class).slash("messages").withRel("messages"));
    }

    private EntityModel<Person> toPersonModel(Person person) {
        Long id = person.getId();
        return EntityModel.of(person,
                linkTo(ApiController.class).slash("personnes").slash(id).withSelfRel(),
                linkTo(ApiController.class).slash("personnes").slash(id).slash("favoris").withRel("favoris"),
                linkTo(ApiController.class).slash("personnes").slash(id).slash("recherches").withRel("recherches"),
                linkTo(ApiController.class).slash("personnes").slash(id).slash("lots").withRel("lots"),
                linkTo(ApiController.class).slash("personnes").slash(id).slash("messages").withRel("messages"),
                linkTo(ApiController.class).slash("annonces").withRel("annonces"));
    }

    private EntityModel<Listing> toFavoriteModel(Long personId, Listing listing) {
        Long listingId = listing.getId();
        return EntityModel.of(listing,
                linkTo(ApiController.class).slash("annonces").slash(listingId).withSelfRel(),
                linkTo(ListingController.class).slash("annonces").slash(listingId).withRel("html"),
                linkTo(ApiController.class).slash("personnes").slash(personId).withRel("personne"),
                linkTo(ApiController.class).slash("personnes").slash(personId)
                        .slash("favoris").slash(listingId).withRel("favori"));
    }

    private EntityModel<SavedSearch> toSearchModel(SavedSearch search) {
        Long id = search.getId();
        Long personId = search.getPerson().getId();
        String notifHref = linkTo(ApiController.class).slash("recherches").slash(id)
                .slash("notifications").toUriComponentsBuilder()
                .queryParam("enabled", !search.isNotifications())
                .toUriString();
        return EntityModel.of(search,
                linkTo(ApiController.class).slash("recherches").slash(id).withSelfRel(),
                linkTo(ApiController.class).slash("personnes").slash(personId).withRel("personne"),
                linkTo(ApiController.class).slash("personnes").slash(personId)
                        .slash("recherches").withRel("recherches"),
                Link.of(notifHref, "notifications"),
                linkTo(ApiController.class).slash("annonces").withRel("resultats"),
                linkTo(ListingController.class).slash("annonces").withRel("html"));
    }

    private EntityModel<ItemLot> toLotModel(ItemLot lot) {
        Long id = lot.getId();
        Long ownerId = lot.getOwner().getId();
        List<Link> links = new ArrayList<>();
        links.add(linkTo(ApiController.class).slash("lots").slash(id).withSelfRel());
        links.add(linkTo(ApiController.class).slash("personnes").slash(ownerId).withRel("proprietaire"));
        links.add(linkTo(ApiController.class).slash("personnes").slash(ownerId).slash("lots").withRel("lots"));
        for (Listing item : lot.getListings()) {
            links.add(linkTo(ApiController.class).slash("annonces").slash(item.getId()).withRel("annonces"));
        }
        return EntityModel.of(lot, links);
    }

    private EntityModel<Message> toMessageModel(Message message) {
        Long id = message.getId();
        Long senderId = message.getSender().getId();
        Long recipientId = message.getRecipient().getId();
        List<Link> links = new ArrayList<>();
        links.add(linkTo(ApiController.class).slash("messages").slash(id).withSelfRel());
        links.add(linkTo(ApiController.class).slash("personnes").slash(senderId).withRel("expediteur"));
        links.add(linkTo(ApiController.class).slash("personnes").slash(recipientId).withRel("destinataire"));
        links.add(linkTo(ApiController.class).slash("personnes").slash(senderId).slash("messages").withRel("messages"));
        if (message.getListing() != null) {
            Long listingId = message.getListing().getId();
            links.add(linkTo(ApiController.class).slash("annonces").slash(listingId).withRel("annonce"));
            links.add(linkTo(ListingController.class).slash("annonces").slash(listingId).withRel("html"));
        }
        return EntityModel.of(message, links);
    }

    private String listingsPageUri(String basePath, String q, String zone, ItemState etat,
            String motCle, int page, int size, String sort) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(
                linkTo(ListingController.class).toUri()).path(basePath);
        if (ListingQuerySupport.blank(q) != null) builder.queryParam("q", q.trim());
        if (ListingQuerySupport.blank(zone) != null) builder.queryParam("zone", zone.trim());
        if (etat != null) builder.queryParam("etat", etat.name());
        if (ListingQuerySupport.blank(motCle) != null) builder.queryParam("motCle", motCle.trim());
        builder.queryParam("page", page);
        builder.queryParam("size", size);
        builder.queryParam("sort", sort);
        return builder.build().toUriString();
    }

    private Listing findListing(Long id) {
        return listings.findById(id).orElseThrow(() -> notFound("Annonce", id));
    }

    private Person findPerson(Long id) {
        return people.findById(id).orElseThrow(() -> notFound("Personne", id));
    }

    private SavedSearch findSearch(Long id) {
        return searches.findById(id).orElseThrow(() -> notFound("Recherche", id));
    }

    private ItemLot findLot(Long id) {
        return lots.findById(id).orElseThrow(() -> notFound("Lot", id));
    }

    private Message findMessage(Long id) {
        return messages.findById(id).orElseThrow(() -> notFound("Message", id));
    }

    private void requireOwner(Listing listing, Long ownerId) {
        if (!listing.ownedBy(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void requireSearchOwner(SavedSearch search, Person connected) {
        if (!search.getPerson().getId().equals(connected.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void requireLotOwner(ItemLot lot, Person connected) {
        if (!lot.getOwner().getId().equals(connected.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private ResponseStatusException notFound(String type, Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " introuvable : " + id);
    }

    public record ListingRequest(@NotBlank String title, String description,
            @NotNull ItemState state, @NotBlank String area,
            @NotNull DeliveryMode deliveryMode, Long ownerId, Set<String> keywords) {
        Listing toListing(Person owner) {
            return new Listing(title, description, state, area, deliveryMode, owner,
                    keywords == null ? Set.of() : new LinkedHashSet<>(keywords));
        }
    }

    public record PersonRequest(@NotBlank String name, @Email @NotBlank String email, String password) { }
    public record SearchRequest(String query, String area, ItemState state, String keyword) { }
    public record LotRequest(@NotBlank String name) { }
    public record MessageRequest(Long senderId, @NotNull Long recipientId,
            Long listingId, @NotBlank String body) { }
}
