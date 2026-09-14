package com.bikematch.bike;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BikeRepository extends JpaRepository<Bike, Long> {

    Optional<Bike> findByIdAndOwnerId(Long id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bike from Bike bike where bike.id = :bikeId and bike.owner.id = :ownerId")
    Optional<Bike> findOwnedByIdForUpdate(
            @Param("bikeId") Long bikeId,
            @Param("ownerId") Long ownerId);

    List<Bike> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
