package com.bikematch.moderation;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lets owners read the notices about their removed bikes and dismiss them. */
@Service
public class RemovalNoticeService {

    private final BikeRemovalNoticeRepository noticeRepository;

    public RemovalNoticeService(BikeRemovalNoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    @Transactional(readOnly = true)
    public List<BikeRemovalNotice> listPending(long ownerId) {
        return noticeRepository.findPendingByOwnerId(ownerId);
    }

    @Transactional
    public void dismiss(long ownerId, long noticeId) {
        BikeRemovalNotice notice = noticeRepository.findOwnedById(noticeId, ownerId)
                .orElseThrow(RemovalNoticeNotFoundException::new);
        notice.dismiss();
        noticeRepository.save(notice);
    }
}
