package com.healthmanagement.service.social;

import com.healthmanagement.model.social.MediaFile;

public interface MediaService {
    MediaFile save(String url, String type, Integer refId);
}
