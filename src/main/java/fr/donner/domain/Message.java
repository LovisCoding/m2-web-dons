package fr.donner.domain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

@Entity
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Person sender;

    @ManyToOne(optional = false)
    private Person recipient;

    @ManyToOne
    private Listing listing;

    @NotBlank
    @Column(length = 4000)
    private String body;

    private Instant sentAt = Instant.now();

    protected Message() {
    }

    public Message(Person sender, Person recipient, Listing listing, String body) {
        this.sender = sender;
        this.recipient = recipient;
        this.listing = listing;
        this.body = body;
    }

    public Long getId() { return id; }
    public Person getSender() { return sender; }
    public Person getRecipient() { return recipient; }
    public Listing getListing() { return listing; }
    public String getBody() { return body; }
    public Instant getSentAt() { return sentAt; }
}
