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
            where (cast(:q as string) is null or lower(l.title) like lower(concat('%', cast(:q as string), '%'))
                or lower(l.description) like lower(concat('%', cast(:q as string), '%')))
                and (cast(:area as string) is null or lower(l.area) like lower(concat('%', cast(:area as string), '%')))
                and (:state is null or l.state = :state)
                and (cast(:keyword as string) is null or lower(k) like lower(concat('%', cast(:keyword as string), '%')))
            order by l.publishedAt desc
            """)
    List<Listing> search(
            @Param("q") String query,
            @Param("area") String area,
            @Param("state") ItemState state,
            @Param("keyword") String keyword);
}
