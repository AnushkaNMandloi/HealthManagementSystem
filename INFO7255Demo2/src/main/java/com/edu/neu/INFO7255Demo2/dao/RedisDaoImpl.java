package com.edu.neu.INFO7255Demo2.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import com.edu.neu.INFO7255Demo2.repository.RedisRepository;

@Repository
public class RedisDaoImpl<T> implements RedisRepository<T> {

	@Autowired
	private RedisTemplate<String, T> redisTemplate;
	private HashOperations<String, Object, T> hashOperations;

	private ValueOperations<String, T> valueOperations;

	@Autowired
	RedisDaoImpl(RedisTemplate<String, T> redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.valueOperations = redisTemplate.opsForValue();

	}

//	    public T getHash(String input) {
//	        return valueOperations.get(input + CommonConstants.HASH_MARK);
//	    }

	public void putValue(String key, Object value) {
		valueOperations.set(key, (T) value);
	}

	public T getValue(String key) {
		return valueOperations.get(key);
	}

	public boolean deleteValue(String key) {
		System.out.println(key + " keykeykey");
		return redisTemplate.delete(key);
	}

	@Override
	public List<T> findAll() {
		return (List<T>) redisTemplate.opsForHash().values("Plan");
	}



}
