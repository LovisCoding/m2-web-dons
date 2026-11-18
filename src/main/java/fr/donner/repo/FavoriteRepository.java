package fr.donner.repo;
import fr.donner.domain.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    boolean existsByPersonIdAndListingId(Long personId, Long listingId);
    List<Favorite> findByPersonId(Long personId);
    Page<Favorite> findByPersonIdOrderByIdDesc(Long personId, Pageable pageable);
    void deleteByPersonIdAndListingId(Long personId, Long listingId);
    void deleteByListingId(Long listingId);
}
