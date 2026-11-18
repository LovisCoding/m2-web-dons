package fr.donner.repo;
import fr.donner.domain.ItemLot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemLotRepository extends JpaRepository<ItemLot, Long> {
    List<ItemLot> findByOwnerId(Long ownerId);
}
