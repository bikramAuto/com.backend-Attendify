package com.Attendify.Attendify.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.Attendify.Attendify.user.UserFace;


public interface UserFaceRepository extends MongoRepository<UserFace, String> {
	Optional<UserFace> findByUserId(String userId);
	
	@Query(value = "{}", fields = "{'faceEmbeddings' : 1}")
	List<UserFace> findAllFaceEmbeddings();
}
