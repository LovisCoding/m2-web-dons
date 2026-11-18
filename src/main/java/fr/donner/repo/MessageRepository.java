package fr.donner.repo;
import fr.donner.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderIdOrRecipientIdOrderBySentAtDesc(Long senderId, Long recipientId);

    @Modifying
    @Query("update Message m set m.listing = null where m.listing.id = :listingId")
    void detachFromListing(@Param("listingId") Long listingId);
}
