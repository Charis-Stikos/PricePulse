package com.pricepulse.repository;

public interface RepoCallback<T> {
    void onComplete(T result);
}
