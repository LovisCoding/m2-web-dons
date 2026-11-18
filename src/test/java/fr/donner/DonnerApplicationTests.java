package fr.donner;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.donner.domain.DeliveryMode;
import fr.donner.domain.ItemLot;
import fr.donner.domain.ItemState;
import fr.donner.domain.Listing;
import fr.donner.domain.Person;
import fr.donner.domain.SavedSearch;
import fr.donner.repo.SavedSearchRepository;
import fr.donner.service.NotificationService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DonnerApplicationTests {
    @Test
    void lotOnlyAcceptsItemsFromItsOwner() {
        Person owner = new Person("A", "a@exemple.fr");
        Person other = new Person("B", "b@exemple.fr");
        ItemLot lot = new ItemLot(owner, "Lot");
        Listing item = new Listing(
                "Livre", "", ItemState.BON, "Paris", DeliveryMode.MAIN_PROPRE, owner, null);
        Listing foreignItem = new Listing(
                "Lampe", "", ItemState.BON, "Paris", DeliveryMode.MAIN_PROPRE, other, null);

        assertDoesNotThrow(() -> lot.add(item));
        assertThrows(IllegalArgumentException.class, () -> lot.add(foreignItem));
    }

    @Test
    void savedSearchMatchesByCriteria() {
        Person owner = new Person("Camille", "camille@example.fr");
        Listing lamp = new Listing(
                "Lampe de bureau vintage",
                "Ampoule incluse",
                ItemState.BON,
                "Lyon",
                DeliveryMode.MAIN_PROPRE,
                owner,
                Set.of("maison", "lumière"));

        assertTrue(new SavedSearch(owner, "lampe", "lyon", "BON", "maison").matches(lamp));
        assertTrue(new SavedSearch(owner, "ampoule", null, null, null).matches(lamp));
        assertFalse(new SavedSearch(owner, "vélo", "lyon", null, null).matches(lamp));
        assertFalse(new SavedSearch(owner, null, "Paris", null, null).matches(lamp));
        assertFalse(new SavedSearch(owner, null, null, "NEUF", null).matches(lamp));
        assertFalse(new SavedSearch(owner, null, null, null, "sport").matches(lamp));
    }

    @Test
    void notificationServiceOnlyLogsEnabledMatchingSearches() {
        Person alice = new Person("Alice", "alice@example.fr");
        Person bob = new Person("Bob", "bob@example.fr");

        SavedSearch enabledMatching = new SavedSearch(alice, "lampe", "Lyon", null, null);
        SavedSearch disabledMatching = new SavedSearch(bob, "lampe", "Lyon", null, null);
        disabledMatching.setNotifications(false);
        SavedSearch enabledNonMatching = new SavedSearch(bob, "vélo", null, null, null);

        SavedSearchRepository repo = mock(SavedSearchRepository.class);
        when(repo.findAll()).thenReturn(List.of(enabledMatching, disabledMatching, enabledNonMatching));

        Logger logger = (Logger) LoggerFactory.getLogger(NotificationService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            NotificationService service = new NotificationService(repo);
            Listing lamp = new Listing(
                    "Lampe vintage", "Parfaite", ItemState.BON, "Lyon", DeliveryMode.MAIN_PROPRE, bob, Set.of());
            service.notifyMatchingSearches(lamp);

            assertEquals(1, appender.list.size());
            assertTrue(appender.list.get(0).getFormattedMessage().contains("alice@example.fr"));
        } finally {
            logger.detachAppender(appender);
        }
    }
}
