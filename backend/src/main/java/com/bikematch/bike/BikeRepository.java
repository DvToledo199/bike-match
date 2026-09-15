package com.bikematch.bike;

import jakarta.persistence.LockModeType;
import com.bikematch.moderation.PendingBikeSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BikeRepository extends JpaRepository<Bike, Long> {

    Optional<Bike> findByIdAndOwnerId(Long id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bike from Bike bike where bike.id = :bikeId and bike.owner.id = :ownerId")
    Optional<Bike> findOwnedByIdForUpdate(
            @Param("bikeId") Long bikeId,
            @Param("ownerId") Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bike from Bike bike join fetch bike.owner where bike.id = :bikeId")
    Optional<Bike> findByIdForUpdate(@Param("bikeId") Long bikeId);

    /** Deletes the bike row; the database cascades to its result and explanations. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Bike bike where bike.id = :bikeId")
    int deleteBikeById(@Param("bikeId") Long bikeId);

    @Query("""
            select new com.bikematch.bike.OwnedBikeSummary(
                bike.id,
                bike.brand,
                bike.model,
                bike.modelYear,
                bike.category,
                bike.photoUrl,
                bike.status,
                case when result.id is null then false else true end,
                bike.createdAt
            )
            from Bike bike
            left join bike.kinematicsResult result
            where bike.owner.id = :ownerId
            order by bike.createdAt desc, bike.id desc
            """)
    List<OwnedBikeSummary> findSummariesByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
            select new com.bikematch.bike.PublicBikeSummary(
                bike.id,
                bike.brand,
                bike.model,
                bike.modelYear,
                bike.category,
                bike.photoUrl,
                bike.createdAt
            )
            from Bike bike
            where bike.status = com.bikematch.bike.BikeStatus.PUBLIC
              and (:category is null or bike.category = :category)
            order by bike.createdAt desc, bike.id desc
            """)
    Page<PublicBikeSummary> findPublicSummaries(
            @Param("category") BikeCategory category,
            Pageable pageable);

    @Query("""
            select new com.bikematch.moderation.PendingBikeSummary(
                bike.id,
                bike.brand,
                bike.model,
                bike.modelYear,
                bike.category,
                bike.photoUrl,
                bike.owner.username,
                bike.publicationRequestedAt
            )
            from Bike bike
            where bike.status = com.bikematch.bike.BikeStatus.PENDING
            order by bike.publicationRequestedAt asc, bike.id asc
            """)
    List<PendingBikeSummary> findPendingSummaries();
}
