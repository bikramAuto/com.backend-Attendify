package com.Attendify.Attendify.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Attendify.Attendify.user.User;


public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
}
