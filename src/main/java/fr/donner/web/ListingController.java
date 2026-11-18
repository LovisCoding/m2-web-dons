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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ListingController {
    private final ListingRepository listings;
    private final PersonRepository people;
    private final FavoriteRepository favorites;
    private final SavedSearchRepository searches;
    private final ItemLotRepository lots;
    private final MessageRepository messages;
    private final NotificationService notifications;
    private final ListingService listingService;
    private final AuthService authService;
    private final ApiController apiController;

    public ListingController(ListingRepository listings, PersonRepository people,
            FavoriteRepository favorites, SavedSearchRepository searches,
            ItemLotRepository lots, MessageRepository messages,
            NotificationService notifications, ListingService listingService,
            AuthService authService, ApiController apiController) {
        this.listings = listings;
        this.people = people;
        this.favorites = favorites;
        this.searches = searches;
        this.lots = lots;
        this.messages = messages;
        this.notifications = notifications;
        this.listingService = listingService;
        this.authService = authService;
        this.apiController = apiController;
    }

    @ModelAttribute("utilisateurConnecte")
    public Person currentUser(Authentication authentication) {
        return authService.currentPerson(authentication).orElse(null);
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/annonces";
    }

    @GetMapping({"/login", "/login/"})
    public String login() {
        return "login";
    }

    @GetMapping({"/inscription", "/inscription/"})
    public String registerForm(Model model) {
        model.addAttribute("inscription", new RegisterForm());
        return "inscription";
    }

    @PostMapping("/inscription")
    public String register(@Valid @ModelAttribute("inscription") RegisterForm form,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "inscription";
        }
        try {
            authService.register(form.name, form.email, form.password);
        } catch (ResponseStatusException e) {
            model.addAttribute("erreur", e.getReason());
            return "inscription";
        }
        return "redirect:/login?inscrit";
    }

    @GetMapping({"/annonces", "/annonces/"})
    public Object index(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) ItemState etat,
            @RequestParam(required = false) String motCle,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "publishedAt,desc") String sort,
            HttpServletRequest request,
            Model model) {
        if (wantsJson(request)) {
            return apiController.allListings(q, zone, etat, motCle, page, size, sort);
        }
        Page<Listing> pageAnnonces = listings.search(
                ListingQuerySupport.blank(q),
                ListingQuerySupport.blank(zone),
                etat,
                ListingQuerySupport.blank(motCle),
                ListingQuerySupport.pageRequest(page, size, sort, 50));
        model.addAttribute("pageAnnonces", pageAnnonces);
        model.addAttribute("annonces", pageAnnonces.getContent());
        model.addAttribute("q", q);
        model.addAttribute("zone", zone);
        model.addAttribute("etat", etat);
        model.addAttribute("motCle", motCle);
        model.addAttribute("page", pageAnnonces.getNumber());
        model.addAttribute("size", pageAnnonces.getSize());
        model.addAttribute("sort", sort);
        model.addAttribute("etats", ItemState.values());
        return "annonces";
    }

    @GetMapping({"/annonces/nouvelle", "/annonces/nouvelle/"})
    public String form(Model model) {
        model.addAttribute("annonce", new ListingForm());
        model.addAttribute("etats", ItemState.values());
        model.addAttribute("modes", DeliveryMode.values());
        return "formulaire";
    }

    @PostMapping("/annonces")
    public String create(@Valid @ModelAttribute("annonce") ListingForm form,
            BindingResult bindingResult, Authentication authentication, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("etats", ItemState.values());
            model.addAttribute("modes", DeliveryMode.values());
            return "formulaire";
        }
        Person owner = authService.requirePerson(authentication);
        Listing listing = new Listing(
                form.title, form.description, form.state, form.area,
                form.deliveryMode, owner, form.keywords(),
                ListingQuerySupport.blank(form.imageUrl));
        listings.save(listing);
        notifications.notifyMatchingSearches(listing);
        return "redirect:/annonces/" + listing.getId();
    }

    @GetMapping({"/annonces/{id:[0-9]+}", "/annonces/{id:[0-9]+}/"})
    public Object detail(@PathVariable Long id, Authentication authentication,
            HttpServletRequest request, Model model) {
        if (wantsJson(request)) {
            return apiController.listing(id);
        }
        Listing annonce = findListing(id);
        Person connected = authService.currentPerson(authentication).orElse(null);
        model.addAttribute("annonce", annonce);
        model.addAttribute("datePublication", DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.systemDefault()).format(annonce.getPublishedAt()));
        model.addAttribute("estProprietaire", connected != null && annonce.ownedBy(connected.getId()));
        model.addAttribute("estFavori",
                connected != null && favorites.existsByPersonIdAndListingId(connected.getId(), id));
        return "detail";
    }

    @PostMapping("/annonces/{id:[0-9]+}/favoris")
    public String addFavorite(@PathVariable Long id, Authentication authentication) {
        Person person = authService.requirePerson(authentication);
        Listing listing = findListing(id);
        if (!favorites.existsByPersonIdAndListingId(person.getId(), id)) {
            favorites.save(new Favorite(person, listing));
        }
        return "redirect:/annonces/" + id;
    }

    @PostMapping("/annonces/{id:[0-9]+}/favoris/supprimer")
    @Transactional
    public String removeFavorite(@PathVariable Long id,
            @RequestParam(required = false) String redirect, Authentication authentication) {
        Person person = authService.requirePerson(authentication);
        favorites.deleteByPersonIdAndListingId(person.getId(), id);
        return "espace".equals(redirect) ? "redirect:/mon-espace" : "redirect:/annonces/" + id;
    }

    @PostMapping("/annonces/{id:[0-9]+}/messages")
    public String message(@PathVariable Long id, @RequestParam @NotBlank String body,
            Authentication authentication) {
        Listing listing = findListing(id);
        Person sender = authService.requirePerson(authentication);
        messages.save(new Message(sender, listing.getOwner(), listing, body));
        return "redirect:/annonces/" + id + "?messageEnvoye";
    }

    @PostMapping("/annonces/{id:[0-9]+}/modifier")
    @Transactional
    public String update(@PathVariable Long id,
            @RequestParam @NotBlank String title, @RequestParam String description,
            @RequestParam @NotNull ItemState state, @RequestParam @NotBlank String area,
            @RequestParam @NotNull DeliveryMode deliveryMode, @RequestParam(required = false) String keywords,
            @RequestParam(required = false) String imageUrl,
            Authentication authentication) {
        Person connected = authService.requirePerson(authentication);
        Listing listing = findListing(id);
        if (!listing.ownedBy(connected.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        listing.update(title, description, state, area, deliveryMode, parseKeywords(keywords),
                ListingQuerySupport.blank(imageUrl));
        listings.save(listing);
        return "redirect:/annonces/" + id;
    }

    @PostMapping("/annonces/{id:[0-9]+}/supprimer")
    public String delete(@PathVariable Long id, Authentication authentication) {
        Person connected = authService.requirePerson(authentication);
        Listing listing = findListing(id);
        if (!listing.ownedBy(connected.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        listingService.delete(listing);
        return "redirect:/annonces";
    }

    @PostMapping("/recherches")
    public String saveSearch(@RequestParam(required = false) String q,
            @RequestParam(required = false) String zone, @RequestParam(required = false) ItemState etat,
            @RequestParam(required = false) String motCle, Authentication authentication) {
        Person person = authService.requirePerson(authentication);
        searches.save(new SavedSearch(person,
                ListingQuerySupport.blank(q),
                ListingQuerySupport.blank(zone),
                etat == null ? null : etat.name(),
                ListingQuerySupport.blank(motCle)));
        return "redirect:/mon-espace";
    }

    @PostMapping("/recherches/{id:[0-9]+}/notifications")
    @Transactional
    public String toggleSearchNotifications(@PathVariable Long id,
            @RequestParam boolean enabled, Authentication authentication) {
        Person connected = authService.requirePerson(authentication);
        SavedSearch search = searches.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!search.getPerson().getId().equals(connected.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        search.setNotifications(enabled);
        return "redirect:/mon-espace";
    }

    @PostMapping("/recherches/{id:[0-9]+}/supprimer")
    public String deleteSearch(@PathVariable Long id, Authentication authentication) {
        Person connected = authService.requirePerson(authentication);
        SavedSearch search = searches.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!search.getPerson().getId().equals(connected.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        searches.delete(search);
        return "redirect:/mon-espace";
    }

    @PostMapping("/lots")
    public String createLot(@RequestParam @NotBlank String name, Authentication authentication) {
        Person connected = authService.requirePerson(authentication);
        lots.save(new ItemLot(connected, name.trim()));
        return "redirect:/mon-espace";
    }

    @PostMapping("/lots/{id:[0-9]+}/annonces")
    @Transactional
    public String addListingToLot(@PathVariable Long id, @RequestParam Long listingId,
            Authentication authentication) {
        Person connected = authService.requirePerson(authentication);
        ItemLot lot = lots.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!lot.getOwner().getId().equals(connected.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        try {
            lot.add(findListing(listingId));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        lots.save(lot);
        return "redirect:/mon-espace";
    }

    @PostMapping("/lots/{id:[0-9]+}/annonces/{annonceId:[0-9]+}/supprimer")
    @Transactional
    public String removeListingFromLot(@PathVariable Long id, @PathVariable Long annonceId,
            Authentication authentication) {
        Person connected = authService.requirePerson(authentication);
        ItemLot lot = lots.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!lot.getOwner().getId().equals(connected.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        lot.getListings().removeIf(item -> item.getId().equals(annonceId));
        return "redirect:/mon-espace";
    }

    @PostMapping("/lots/{id:[0-9]+}/supprimer")
    public String deleteLot(@PathVariable Long id, Authentication authentication) {
        Person connected = authService.requirePerson(authentication);
        ItemLot lot = lots.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!lot.getOwner().getId().equals(connected.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        lots.delete(lot);
        return "redirect:/mon-espace";
    }

    @PostMapping("/messages")
    public String sendDirectMessage(@RequestParam Long recipientId,
            @RequestParam(required = false) Long listingId,
            @RequestParam @NotBlank String body, Authentication authentication) {
        Person sender = authService.requirePerson(authentication);
        Person recipient = people.findById(recipientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Listing listing = listingId == null ? null : findListing(listingId);
        messages.save(new Message(sender, recipient, listing, body));
        return "redirect:/mon-espace";
    }

    @GetMapping({"/mon-espace", "/mon-espace/"})
    public String space(Authentication authentication, Model model) {
        Person connected = authService.requirePerson(authentication);
        Long personId = connected.getId();
        model.addAttribute("mesAnnonces", listings.findByOwnerIdOrderByPublishedAtDesc(personId));
        model.addAttribute("mesLots", lots.findByOwnerId(personId));
        model.addAttribute("mesFavoris", favorites.findByPersonId(personId).stream()
                .map(Favorite::getListing).toList());
        model.addAttribute("mesRecherches", searches.findByPersonId(personId));
        model.addAttribute("mesMessages",
                messages.findBySenderIdOrRecipientIdOrderBySentAtDesc(personId, personId));
        model.addAttribute("destinataires", people.findAll().stream()
                .filter(p -> !p.getId().equals(personId))
                .toList());
        return "espace";
    }

    @GetMapping(path = "/personnes/{id}/favoris",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public CollectionModel<EntityModel<Listing>> favoritesApi(@PathVariable Long id,
            Authentication authentication) {
        return apiController.favorites(id, authentication);
    }

    @GetMapping(path = "/personnes/{id}/recherches",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public CollectionModel<EntityModel<SavedSearch>> searchesApi(@PathVariable Long id,
            Authentication authentication) {
        return apiController.searches(id, authentication);
    }

    @GetMapping(path = "/personnes/{id}/lots",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public CollectionModel<EntityModel<ItemLot>> lotsApi(@PathVariable Long id) {
        return apiController.lots(id);
    }

    @GetMapping(path = "/personnes/{id}/messages",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public CollectionModel<EntityModel<Message>> messagesApi(@PathVariable Long id,
            Authentication authentication) {
        return apiController.messages(id, authentication);
    }

    private static boolean wantsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && !accept.contains("text/html")
                && (accept.contains("application/json") || accept.contains("application/hal+json"));
    }

    private Listing findListing(Long id) {
        return listings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Annonce introuvable : " + id));
    }

    private static Set<String> parseKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
        public String imageUrl;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getArea() { return area; }
        public void setArea(String area) { this.area = area; }
        public String getKeywords() { return keywords; }
        public void setKeywords(String keywords) { this.keywords = keywords; }
        public ItemState getState() { return state; }
        public void setState(ItemState state) { this.state = state; }
        public DeliveryMode getDeliveryMode() { return deliveryMode; }
        public void setDeliveryMode(DeliveryMode deliveryMode) { this.deliveryMode = deliveryMode; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        Set<String> keywords() {
            return parseKeywords(keywords);
        }
    }

    public static class RegisterForm {
        @NotBlank
        public String name;
        @Email
        @NotBlank
        public String email;
        @NotBlank
        public String password;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
