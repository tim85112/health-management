package com.healthmanagement.model.social;

import java.io.Serializable;
import java.util.Objects;

public class PostLikeId implements Serializable {
    private Integer user;
    private Integer post;

    // 必須提供無參構造
    public PostLikeId() {}

    public PostLikeId(Integer user, Integer post) {
        this.user = user;
        this.post = post;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostLikeId)) return false;
        PostLikeId that = (PostLikeId) o;
        return Objects.equals(user, that.user) &&
               Objects.equals(post, that.post);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, post);
    }
}
