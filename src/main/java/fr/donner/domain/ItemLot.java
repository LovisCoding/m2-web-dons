package fr.donner.domain;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
public class ItemLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Person owner;

    @NotBlank
    private String name;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "lot_listing")
    private Set<Listing> listings = new LinkedHashSet<>();

    protected ItemLot() {
    }

    public ItemLot(Person owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public Long getId() { return id; }
    public Person getOwner() { return owner; }
    public String getName() { return name; }
    public Set<Listing> getListings() { return listings; }

    public void add(Listing listing) {
        boolean sameOwner = listing.getOwner() == owner
                || (owner.getId() != null && owner.getId().equals(listing.getOwner().getId()));
        if (!sameOwner) {
            throw new IllegalArgumentException("Un lot ne peut contenir que les annonces de son propriétaire");
        }
        listings.add(listing);
    }
}
