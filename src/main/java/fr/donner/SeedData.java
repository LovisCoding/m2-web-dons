package fr.donner;
import fr.donner.domain.DeliveryMode;
import fr.donner.domain.ItemState;
import fr.donner.domain.Listing;
import fr.donner.domain.Person;
import fr.donner.repo.ListingRepository;
import fr.donner.repo.PersonRepository;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
class SeedData {
    @Bean
    CommandLineRunner seed(PersonRepository people, ListingRepository listings, PasswordEncoder passwordEncoder) {
        return args -> {
            if (people.count() == 0) {
                String defaultHash = passwordEncoder.encode("password");
                Person camille = people.save(new Person("Camille", "camille@example.fr", defaultHash));
                Person yanis = people.save(new Person("Yanis", "yanis@example.fr", defaultHash));

                listings.save(new Listing(
                        "Lampe de bureau vintage",
                        "Lampe fonctionnelle, ampoule incluse.",
                        ItemState.BON,
                        "Lyon",
                        DeliveryMode.MAIN_PROPRE,
                        camille,
                        Set.of("maison", "lumière")));
                listings.save(new Listing(
                        "Roman — Le Petit Prince",
                        "Livre en très bon état.",
                        ItemState.TRES_BON,
                        "Villeurbanne",
                        DeliveryMode.LES_DEUX,
                        yanis,
                        Set.of("livre", "culture")));
            } else {
                people.findAll().forEach(person -> {
                    if (person.getPasswordHash() == null || person.getPasswordHash().isBlank()) {
                        person.setPasswordHash(passwordEncoder.encode("password"));
                        people.save(person);
                    }
                });
            }
        };
    }
}
