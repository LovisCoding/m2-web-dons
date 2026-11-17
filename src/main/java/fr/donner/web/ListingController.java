package fr.donner.web;
import fr.donner.domain.DeliveryMode;
import fr.donner.domain.ItemState;
import fr.donner.domain.Listing;
import fr.donner.domain.Person;
import fr.donner.repo.ListingRepository;
import fr.donner.repo.PersonRepository;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Controller
public class ListingController {
    private final ListingRepository listings;
    private final PersonRepository people;

    public ListingController(ListingRepository listings, PersonRepository people) {
        this.listings = listings;
        this.people = people;
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
        return "redirect:/annonces/" + listing.getId();
    }

    @GetMapping("/annonces/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("annonce", listings.findById(id).orElseThrow());
        return "detail";
    }

    @GetMapping(value = "/api/annonces", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<CollectionModel<EntityModel<Listing>>> api() {
        var resources = listings.findAll().stream()
                .map(listing -> EntityModel.of(
                        listing,
                        linkTo(methodOn(ListingController.class).api()).withSelfRel(),
                        linkTo(ListingController.class).slash("annonces").slash(listing.getId()).withRel("html")))
                .toList();
        var representation = CollectionModel.of(
                resources,
                linkTo(methodOn(ListingController.class).api()).withSelfRel());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
                .body(representation);
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static class ListingForm {
        public String title;
        public String description;
        public String area;
        public String keywords;
        public ItemState state;
        public DeliveryMode deliveryMode;
        public Long ownerId = 1L;

        Set<String> keywords() {
            return keywords == null || keywords.isBlank()
                    ? Set.of()
                    : new LinkedHashSet<>(Arrays.asList(keywords.split(",\\s*")));
        }
    }
}
