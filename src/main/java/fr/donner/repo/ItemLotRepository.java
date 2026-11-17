package fr.donner.repo;
import fr.donner.domain.ItemLot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemLotRepository extends JpaRepository<ItemLot, Long> {
}
