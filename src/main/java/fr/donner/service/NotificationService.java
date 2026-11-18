package fr.donner.service;

import fr.donner.domain.Listing;
import fr.donner.domain.SavedSearch;
import fr.donner.repo.SavedSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final SavedSearchRepository searches;

    public NotificationService(SavedSearchRepository searches) {
        this.searches = searches;
    }

    public void notifyMatchingSearches(Listing listing) {
        searches.findAll().stream()
                .filter(SavedSearch::isNotifications)
                .filter(search -> search.matches(listing))
                .forEach(search -> log.info(
                        "Nouvelle annonce '{}' correspondant à la recherche {} de {}",
                        listing.getTitle(), search.getId(), search.getPerson().getEmail()));
    }
}
