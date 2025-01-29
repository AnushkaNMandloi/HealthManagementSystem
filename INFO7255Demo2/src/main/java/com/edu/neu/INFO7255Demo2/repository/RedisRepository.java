package com.edu.neu.INFO7255Demo2.repository;

import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisRepository<T> {
	    public void putValue(String key, T value);

	    public T getValue(String key);

	    public boolean deleteValue(String key);

	    public List<T> findAll();
}
