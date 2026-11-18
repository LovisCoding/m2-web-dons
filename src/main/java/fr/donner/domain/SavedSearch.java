package fr.donner.domain;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class SavedSearch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Person person;

    private String query;
    private String area;
    private String state;
    private String keyword;
    private boolean notifications = true;

    protected SavedSearch() {
    }

    public SavedSearch(Person person, String query, String area, String state, String keyword) {
        this.person = person;
        this.query = query;
        this.area = area;
        this.state = state;
        this.keyword = keyword;
    }

    public boolean isNotifications() {
        return notifications;
    }

    public Long getId() { return id; }
    public Person getPerson() { return person; }
    public String getQuery() { return query; }
    public String getArea() { return area; }
    public String getState() { return state; }
    public String getKeyword() { return keyword; }

    public boolean matches(Listing listing) {
        return (query == null || listing.getTitle().toLowerCase().contains(query.toLowerCase())
                || (listing.getDescription() != null
                    && listing.getDescription().toLowerCase().contains(query.toLowerCase())))
                && (area == null || listing.getArea().toLowerCase().contains(area.toLowerCase()))
                && (state == null || listing.getState().name().equalsIgnoreCase(state))
                && (keyword == null || listing.getKeywords().stream()
                        .anyMatch(value -> value.equalsIgnoreCase(keyword)));
    }

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
    }

    public void update(String query, String area, String state, String keyword) {
        this.query = query;
        this.area = area;
        this.state = state;
        this.keyword = keyword;
    }
}
