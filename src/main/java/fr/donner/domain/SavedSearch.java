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

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
    }
}
