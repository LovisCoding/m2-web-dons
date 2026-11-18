package fr.donner.web;
import fr.donner.domain.DeliveryMode;
import fr.donner.domain.ItemState;
import fr.donner.domain.Listing;
import fr.donner.domain.Person;
import fr.donner.domain.Favorite;
import fr.donner.domain.Message;
import fr.donner.domain.SavedSearch;
import fr.donner.repo.FavoriteRepository;
import fr.donner.repo.ListingRepository;
import fr.donner.repo.MessageRepository;
import fr.donner.repo.PersonRepository;
import fr.donner.repo.SavedSearchRepository;
import fr.donner.service.NotificationService;
import fr.donner.service.ListingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ListingController {
    private final ListingRepository listings;
    private final PersonRepository people;
    private final FavoriteRepository favorites;
    private final SavedSearchRepository searches;
    private final MessageRepository messages;
    private final NotificationService notifications;
    private final ListingService listingService;

    public ListingController(ListingRepository listings, PersonRepository people,
            FavoriteRepository favorites, SavedSearchRepository searches,
            MessageRepository messages, NotificationService notifications, ListingService listingService) {
        this.listings = listings;
        this.people = people;
        this.favorites = favorites;
        this.searches = searches;
        this.messages = messages;
        this.notifications = notifications;
        this.listingService = listingService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/annonces";
    }

    @GetMapping("/annonces")
    public String index(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) ItemState etat,
            @RequestParam(required = false) String motCle,
            Model model) {
        model.addAttribute("annonces", listings.search(blank(q), blank(zone), etat, blank(motCle)));
        model.addAttribute("q", q);
        model.addAttribute("zone", zone);
        model.addAttribute("etat", etat);
        model.addAttribute("motCle", motCle);
        model.addAttribute("etats", ItemState.values());
        return "annonces";
    }

    @GetMapping("/annonces/nouvelle")
    public String form(Model model) {
        model.addAttribute("annonce", new ListingForm());
        model.addAttribute("etats", ItemState.values());
        model.addAttribute("modes", DeliveryMode.values());
        return "formulaire";
    }

    @PostMapping("/annonces")
    public String create(@Valid @ModelAttribute("annonce") ListingForm form) {
        Person owner = people.findById(form.ownerId).orElseThrow();
        Listing listing = new Listing(
                form.title, form.description, form.state, form.area,
                form.deliveryMode, owner, form.keywords());
        listings.save(listing);
        notifications.notifyMatchingSearches(listing);
        return "redirect:/annonces/" + listing.getId();
    }

    @GetMapping("/annonces/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Listing annonce = listings.findById(id).orElseThrow();
        model.addAttribute("annonce", annonce);
        model.addAttribute("datePublication", DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.systemDefault()).format(annonce.getPublishedAt()));
        model.addAttribute("personnes", people.findAll());
        return "detail";
    }

    @PostMapping("/annonces/{id}/favoris")
    public String favorite(@PathVariable Long id, @RequestParam Long personId) {
        Person person = people.findById(personId).orElseThrow();
        Listing listing = listings.findById(id).orElseThrow();
        if (!favorites.existsByPersonIdAndListingId(personId, id)) {
            favorites.save(new Favorite(person, listing));
        }
        return "redirect:/annonces/" + id;
    }

    @PostMapping("/annonces/{id}/messages")
    public String message(@PathVariable Long id, @RequestParam Long senderId, @RequestParam String body) {
        Listing listing = listings.findById(id).orElseThrow();
        Person sender = people.findById(senderId).orElseThrow();
        messages.save(new Message(sender, listing.getOwner(), listing, body));
        return "redirect:/annonces/" + id;
    }

    @PostMapping("/annonces/{id}/modifier")
    public String update(@PathVariable Long id, @RequestParam Long ownerId,
            @RequestParam @NotBlank String title, @RequestParam String description,
            @RequestParam @NotNull ItemState state, @RequestParam @NotBlank String area,
            @RequestParam @NotNull DeliveryMode deliveryMode, @RequestParam(required = false) String keywords) {
        Listing listing = listings.findById(id).orElseThrow();
        if (!listing.ownedBy(ownerId)) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN);
        listing.update(title, description, state, area, deliveryMode,
                keywords == null || keywords.isBlank() ? Set.of()
                        : new LinkedHashSet<>(Arrays.asList(keywords.split(",\\s*"))));
        return "redirect:/annonces/" + id;
    }

    @PostMapping("/annonces/{id}/supprimer")
    public String delete(@PathVariable Long id, @RequestParam Long ownerId) {
        Listing listing = listings.findById(id).orElseThrow();
        if (!listing.ownedBy(ownerId)) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN);
        listingService.delete(listing);
        return "redirect:/annonces";
    }

    @PostMapping("/recherches")
    public String saveSearch(@RequestParam Long personId, @RequestParam(required = false) String q,
            @RequestParam(required = false) String zone, @RequestParam(required = false) ItemState etat,
            @RequestParam(required = false) String motCle) {
        Person person = people.findById(personId).orElseThrow();
        searches.save(new SavedSearch(person, blank(q), blank(zone),
                etat == null ? null : etat.name(), blank(motCle)));
        return "redirect:/annonces";
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static class ListingForm {
        @NotBlank
        public String title;
        public String description;
        @NotBlank
        public String area;
        public String keywords;
        @NotNull
        public ItemState state;
        @NotNull
        public DeliveryMode deliveryMode;
        public Long ownerId = 1L;

        Set<String> keywords() {
            return keywords == null || keywords.isBlank()
                    ? Set.of()
                    : new LinkedHashSet<>(Arrays.asList(keywords.split(",\\s*")));
        }
    }
}
