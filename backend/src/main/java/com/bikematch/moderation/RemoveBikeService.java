package com.bikematch.moderation;

import com.bikematch.bike.BikeNotFoundException;
import com.bikematch.bike.BikeStatus;
import com.bikematch.bike.DeleteBikeService;
import org.springframework.stereotype.Service;

/**
 * A moderator removes a bike: it is deleted completely, as when its owner deletes it, and the
 * owner keeps a notice with the reason. The notice is saved in the deletion's transaction, so
 * it exists only if the bike is really gone.
 */
@Service
public class RemoveBikeService {

    private final DeleteBikeService deleteBikeService;
    private final BikeRemovalNoticeRepository noticeRepository;

    public RemoveBikeService(
            DeleteBikeService deleteBikeService,
            BikeRemovalNoticeRepository noticeRepository
    ) {
        this.deleteBikeService = deleteBikeService;
        this.noticeRepository = noticeRepository;
    }

    public void remove(long bikeId, String reason) {
        deleteBikeService.deleteBike(bikeId, bike -> {
            // Private bikes never reach moderation, so a moderator cannot remove them either.
            if (bike.getStatus() == BikeStatus.PRIVATE) {
                throw new BikeNotFoundException();
            }
            noticeRepository.save(BikeRemovalNotice.forRemovedBike(bike, reason));
        });
    }
}
