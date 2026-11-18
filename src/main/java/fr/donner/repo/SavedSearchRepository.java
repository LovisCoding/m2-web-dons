package fr.donner.repo;
import fr.donner.domain.SavedSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {
    List<SavedSearch> findByPersonId(Long personId);
}
