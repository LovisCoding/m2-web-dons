package fr.donner.repo;

import fr.donner.domain.Person;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByEmailIgnoreCase(String email);
    Optional<Person> findByNameIgnoreCase(String name);
    boolean existsByEmailIgnoreCase(String email);
}
