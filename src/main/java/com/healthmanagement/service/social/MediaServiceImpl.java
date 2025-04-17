package com.healthmanagement.service.social;

import com.healthmanagement.dao.social.MediaFileRepository;
import com.healthmanagement.model.social.MediaFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MediaServiceImpl implements MediaService {

    @Autowired
    private MediaFileRepository repo;

    @Override
    public MediaFile save(String url, String type, Integer refId) {
        MediaFile media = new MediaFile();
        media.setUrl(url);
        media.setType(type);
        media.setRefId(refId);
        media.setUploadedAt(LocalDateTime.now());
        return repo.save(media);
    }
}
