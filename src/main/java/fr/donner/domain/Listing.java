package fr.donner.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.*;

@Entity
public class Listing {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @NotBlank private String title;
    @Column(length=4000) private String description;
    @Enumerated(EnumType.STRING) private ItemState state;
    private Instant publishedAt=Instant.now();
    private String area;
    @Enumerated(EnumType.STRING) private DeliveryMode deliveryMode;
    @Column(name = "image_url", length = 1000) private String imageUrl;
    @ElementCollection(fetch=FetchType.EAGER) private Set<String> keywords=new LinkedHashSet<>();
    @ManyToOne(optional=false) private Person owner;
    protected Listing() {}
    public Listing(String title,String description,ItemState state,String area,DeliveryMode mode,Person owner,Set<String> keywords){
        this(title,description,state,area,mode,owner,keywords,null);
    }
    public Listing(String title,String description,ItemState state,String area,DeliveryMode mode,Person owner,Set<String> keywords,String imageUrl){
        this.title=title;
        this.description=description;
        this.state=state;
        this.area=area;
        this.deliveryMode=mode;
        this.owner=owner;
        this.keywords=keywords==null?new LinkedHashSet<>():new LinkedHashSet<>(keywords);
        setImageUrl(imageUrl);
    }
    public Long getId(){return id;} public String getTitle(){return title;} public String getDescription(){return description;} public ItemState getState(){return state;} public Instant getPublishedAt(){return publishedAt;} public String getArea(){return area;} public DeliveryMode getDeliveryMode(){return deliveryMode;} public String getImageUrl(){return imageUrl;} public Set<String> getKeywords(){return keywords;} public Person getOwner(){return owner;}
    public void setImageUrl(String imageUrl) { this.imageUrl = (imageUrl == null || imageUrl.isBlank()) ? null : imageUrl.trim(); }
    public void setPublishedAt(Instant publishedAt) { if (publishedAt != null) { this.publishedAt = publishedAt; } }
    public boolean ownedBy(Long personId) { return owner.getId().equals(personId); }
    public void update(String title,String description,ItemState state,String area,DeliveryMode mode,Set<String> keywords){
        update(title,description,state,area,mode,keywords,this.imageUrl);
    }
    public void update(String title,String description,ItemState state,String area,DeliveryMode mode,Set<String> keywords,String imageUrl){
        this.title=title;
        this.description=description;
        this.state=state;
        this.area=area;
        this.deliveryMode=mode;
        this.keywords=keywords==null?new LinkedHashSet<>():new LinkedHashSet<>(keywords);
        setImageUrl(imageUrl);
    }
}
