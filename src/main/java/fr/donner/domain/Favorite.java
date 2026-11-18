package fr.donner.domain;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"person_id", "listing_id"}))
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Person person;

    @ManyToOne(optional = false)
    private Listing listing;

    protected Favorite() {
    }

    public Favorite(Person person, Listing listing) {
        this.person = person;
        this.listing = listing;
    }

    public Listing getListing() { return listing; }
}
