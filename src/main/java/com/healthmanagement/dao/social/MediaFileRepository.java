package com.healthmanagement.dao.social;

import com.healthmanagement.model.social.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaFileRepository extends JpaRepository<MediaFile, Integer> {
    List<MediaFile> findByTypeAndRefId(String type, Integer refId);
}
