package com.bikematch.moderation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BikeRemovalNoticeRepository extends JpaRepository<BikeRemovalNotice, Long> {

    @Query("""
            select notice from BikeRemovalNotice notice
            where notice.owner.id = :ownerId
              and notice.dismissedAt is null
            order by notice.removedAt desc, notice.id desc
            """)
    List<BikeRemovalNotice> findPendingByOwnerId(@Param("ownerId") Long ownerId);

    @Query("select notice from BikeRemovalNotice notice where notice.id = :noticeId and notice.owner.id = :ownerId")
    Optional<BikeRemovalNotice> findOwnedById(
            @Param("noticeId") Long noticeId,
            @Param("ownerId") Long ownerId);
}
