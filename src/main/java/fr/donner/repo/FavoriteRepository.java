package fr.donner.repo;
import fr.donner.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    boolean existsByPersonIdAndListingId(Long personId, Long listingId);
}
