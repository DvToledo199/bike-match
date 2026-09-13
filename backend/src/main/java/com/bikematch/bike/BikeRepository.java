package com.bikematch.bike;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BikeRepository extends JpaRepository<Bike, Long> {

    Optional<Bike> findByIdAndOwnerId(Long id, Long ownerId);

    List<Bike> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
