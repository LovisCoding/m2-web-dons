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
import fr.donner.service.NotificationService;
import fr.donner.service.ListingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiController {
    private final PersonRepository people;
    private final ListingRepository listings;
    private final FavoriteRepository favorites;
    private final SavedSearchRepository searches;
    private final ItemLotRepository lots;
    private final MessageRepository messages;
    private final NotificationService notifications;
    private final ListingService listingService;

    public ApiController(PersonRepository people, ListingRepository listings,
            FavoriteRepository favorites, SavedSearchRepository searches,
            ItemLotRepository lots, MessageRepository messages, NotificationService notifications,
            ListingService listingService) {
        this.people = people;
        this.listings = listings;
        this.favorites = favorites;
        this.searches = searches;
        this.lots = lots;
        this.messages = messages;
        this.notifications = notifications;
        this.listingService = listingService;
    }

    @GetMapping("/annonces")
    public ResponseEntity<CollectionModel<EntityModel<Listing>>> allListings() {
        List<EntityModel<Listing>> resources = listings.findAll().stream()
                .map(listing -> EntityModel.of(listing,
                        linkTo(ApiController.class).slash("annonces").slash(listing.getId()).withSelfRel()))
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
                .body(CollectionModel.of(resources,
                        linkTo(ApiController.class).slash("annonces").withSelfRel()));
    }

    @GetMapping("/annonces/{id}")
    public EntityModel<Listing> listing(@PathVariable Long id) {
        Listing listing = findListing(id);
        return EntityModel.of(listing,
                linkTo(ApiController.class).slash("annonces").slash(id).withSelfRel(),
                linkTo(ApiController.class).slash("personnes").slash(listing.getOwner().getId())
                        .slash("favoris").withRel("favoris"));
    }

    @PostMapping(path = "/annonces", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<Listing>> createListing(@Valid @RequestBody ListingRequest request) {
        Person owner = findPerson(request.ownerId());
        Listing listing = listings.save(request.toListing(owner));
        notifications.notifyMatchingSearches(listing);
        return ResponseEntity.created(URI.create("/api/annonces/" + listing.getId()))
                .body(listing(listing.getId()));
    }

    @PutMapping(path = "/annonces/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public EntityModel<Listing> updateListing(@PathVariable Long id,
            @RequestParam Long ownerId, @Valid @RequestBody ListingRequest request) {
        Listing listing = findListing(id);
        requireOwner(listing, ownerId);
        if (!listing.getOwner().getId().equals(request.ownerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le propriétaire ne peut pas être modifié");
        }
        listing.update(request.title(), request.description(), request.state(), request.area(),
                request.deliveryMode(), request.keywords());
        return listing(id);
    }

    @DeleteMapping("/annonces/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id, @RequestParam Long ownerId) {
        Listing listing = findListing(id);
        requireOwner(listing, ownerId);
        listingService.delete(listing);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/personnes")
    public List<Person> people() { return people.findAll(); }

    @PostMapping(path = "/personnes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Person> register(@Valid @RequestBody PersonRequest request) {
        Person person = people.save(new Person(request.name(), request.email()));
        return ResponseEntity.created(URI.create("/api/personnes/" + person.getId())).body(person);
    }

    @GetMapping("/personnes/{personId}/favoris")
    public List<Listing> favorites(@PathVariable Long personId) {
        findPerson(personId);
        return favorites.findByPersonId(personId).stream().map(Favorite::getListing).toList();
    }

    @PostMapping("/personnes/{personId}/favoris/{listingId}")
    public ResponseEntity<Void> addFavorite(@PathVariable Long personId, @PathVariable Long listingId) {
        Person person = findPerson(personId);
        Listing listing = findListing(listingId);
        if (!favorites.existsByPersonIdAndListingId(personId, listingId)) {
            favorites.save(new Favorite(person, listing));
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/personnes/{personId}/favoris/{listingId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long personId, @PathVariable Long listingId) {
        findPerson(personId);
        favorites.deleteByPersonIdAndListingId(personId, listingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/personnes/{personId}/recherches")
    public List<SavedSearch> searches(@PathVariable Long personId) {
        findPerson(personId);
        return searches.findByPersonId(personId);
    }

    @PostMapping(path = "/personnes/{personId}/recherches", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SavedSearch> saveSearch(@PathVariable Long personId,
            @Valid @RequestBody SearchRequest request) {
        SavedSearch search = searches.save(new SavedSearch(findPerson(personId), request.query(),
                request.area(), request.state() == null ? null : request.state().name(), request.keyword()));
        return ResponseEntity.created(URI.create("/api/recherches/" + search.getId())).body(search);
    }

    @PatchMapping("/recherches/{id}/notifications")
    public SavedSearch notifications(@PathVariable Long id, @RequestParam boolean enabled) {
        SavedSearch search = searches.findById(id).orElseThrow(() -> notFound("Recherche", id));
        search.setNotifications(enabled);
        return search;
    }

    @PutMapping(path = "/recherches/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public SavedSearch updateSearch(@PathVariable Long id, @RequestParam Long personId,
            @Valid @RequestBody SearchRequest request) {
        SavedSearch search = searches.findById(id).orElseThrow(() -> notFound("Recherche", id));
        if (!search.getPerson().getId().equals(personId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        search.update(request.query(), request.area(),
                request.state() == null ? null : request.state().name(), request.keyword());
        return search;
    }

    @DeleteMapping("/lots/{id}")
    public ResponseEntity<Void> deleteLot(@PathVariable Long id, @RequestParam Long ownerId) {
        ItemLot lot = lots.findById(id).orElseThrow(() -> notFound("Lot", id));
        if (!lot.getOwner().getId().equals(ownerId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        lots.delete(lot);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/recherches/{id}")
    public ResponseEntity<Void> deleteSearch(@PathVariable Long id, @RequestParam Long personId) {
        SavedSearch search = searches.findById(id).orElseThrow(() -> notFound("Recherche", id));
        if (!search.getPerson().getId().equals(personId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        searches.delete(search);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/personnes/{personId}/lots")
    public List<ItemLot> lots(@PathVariable Long personId) {
        findPerson(personId);
        return lots.findByOwnerId(personId);
    }

    @PostMapping(path = "/personnes/{personId}/lots", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ItemLot> createLot(@PathVariable Long personId, @Valid @RequestBody LotRequest request) {
        ItemLot lot = lots.save(new ItemLot(findPerson(personId), request.name()));
        return ResponseEntity.created(URI.create("/api/lots/" + lot.getId())).body(lot);
    }

    @PostMapping("/lots/{lotId}/annonces/{listingId}")
    @Transactional
    public ItemLot addToLot(@PathVariable Long lotId, @PathVariable Long listingId,
            @RequestParam Long ownerId) {
        ItemLot lot = lots.findById(lotId).orElseThrow(() -> notFound("Lot", lotId));
        if (!lot.getOwner().getId().equals(ownerId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        lot.add(findListing(listingId));
        return lot;
    }

    @DeleteMapping("/lots/{lotId}/annonces/{listingId}")
    @Transactional
    public ResponseEntity<Void> removeFromLot(@PathVariable Long lotId, @PathVariable Long listingId,
            @RequestParam Long ownerId) {
        ItemLot lot = lots.findById(lotId).orElseThrow(() -> notFound("Lot", lotId));
        if (!lot.getOwner().getId().equals(ownerId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        lot.getListings().removeIf(item -> item.getId().equals(listingId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/personnes/{personId}/messages")
    public List<Message> messages(@PathVariable Long personId) {
        findPerson(personId);
        return messages.findBySenderIdOrRecipientIdOrderBySentAtDesc(personId, personId);
    }

    @PostMapping(path = "/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Message> sendMessage(@Valid @RequestBody MessageRequest request) {
        Message message = messages.save(new Message(findPerson(request.senderId()),
                findPerson(request.recipientId()), request.listingId() == null ? null
                        : findListing(request.listingId()), request.body()));
        return ResponseEntity.created(URI.create("/api/messages/" + message.getId())).body(message);
    }

    private Listing findListing(Long id) {
        return listings.findById(id).orElseThrow(() -> notFound("Annonce", id));
    }

    private Person findPerson(Long id) {
        return people.findById(id).orElseThrow(() -> notFound("Personne", id));
    }

    private void requireOwner(Listing listing, Long ownerId) {
        if (!listing.ownedBy(ownerId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private ResponseStatusException notFound(String type, Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " introuvable : " + id);
    }

    public record ListingRequest(@NotBlank String title, String description,
            @NotNull ItemState state, @NotBlank String area,
            @NotNull DeliveryMode deliveryMode, @NotNull Long ownerId, Set<String> keywords) {
        Listing toListing(Person owner) {
            return new Listing(title, description, state, area, deliveryMode, owner,
                    keywords == null ? Set.of() : new LinkedHashSet<>(keywords));
        }
    }

    public record PersonRequest(@NotBlank String name, @Email @NotBlank String email) { }
    public record SearchRequest(String query, String area, ItemState state, String keyword) { }
    public record LotRequest(@NotBlank String name) { }
    public record MessageRequest(@NotNull Long senderId, @NotNull Long recipientId,
            Long listingId, @NotBlank String body) { }
}
