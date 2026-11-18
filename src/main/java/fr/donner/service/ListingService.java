package fr.donner.service;

import fr.donner.domain.ItemLot;
import fr.donner.domain.Listing;
import fr.donner.repo.FavoriteRepository;
import fr.donner.repo.ItemLotRepository;
import fr.donner.repo.ListingRepository;
import fr.donner.repo.MessageRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListingService {
    private final ListingRepository listings;
    private final FavoriteRepository favorites;
    private final ItemLotRepository lots;
    private final MessageRepository messages;
    private final PhotoStorageService photoStorage;

    public ListingService(ListingRepository listings, FavoriteRepository favorites,
            ItemLotRepository lots, MessageRepository messages, PhotoStorageService photoStorage) {
        this.listings = listings;
        this.favorites = favorites;
        this.lots = lots;
        this.messages = messages;
        this.photoStorage = photoStorage;
    }

    @Transactional
    public void delete(Listing listing) {
        Long id = listing.getId();
        String imageUrl = listing.getImageUrl();
        favorites.deleteByListingId(id);
        messages.detachFromListing(id);
        List<ItemLot> containing = lots.findAll().stream()
                .filter(lot -> lot.getListings().removeIf(item -> item.getId().equals(id)))
                .toList();
        lots.saveAll(containing);
        listings.delete(listing);
        photoStorage.deleteIfLocalUpload(imageUrl);
    }
}
