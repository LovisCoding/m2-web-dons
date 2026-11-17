package fr.donner.repo;
import fr.donner.domain.ItemState;
import fr.donner.domain.Listing;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingRepository extends JpaRepository<Listing,Long> {
    @Query("""
            select distinct l from Listing l left join l.keywords k
            where (:q is null or lower(l.title) like lower(concat('%', :q, '%'))
                or lower(l.description) like lower(concat('%', :q, '%')))
                and (:area is null or lower(l.area) like lower(concat('%', :area, '%')))
                and (:state is null or l.state = :state)
                and (:keyword is null or lower(k) like lower(concat('%', :keyword, '%')))
            order by l.publishedAt desc
            """)
    List<Listing> search(
            @Param("q") String query,
            @Param("area") String area,
            @Param("state") ItemState state,
            @Param("keyword") String keyword);
}
