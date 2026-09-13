package com.bikematch.bike;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KinematicsResultRepository extends JpaRepository<KinematicsResult, Long> {

    Optional<KinematicsResult> findByBikeId(Long bikeId);
}
