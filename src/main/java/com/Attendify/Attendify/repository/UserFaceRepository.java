package com.Attendify.Attendify.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Attendify.Attendify.user.UserFace;


public interface UserFaceRepository extends MongoRepository<UserFace, String> {
	Optional<UserFace> findByUserId(String userId);

}
