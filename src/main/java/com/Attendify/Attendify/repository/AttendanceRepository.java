package com.Attendify.Attendify.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Attendify.Attendify.user.AttendanceRecord;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AttendanceRepository extends MongoRepository<AttendanceRecord, String> {
	Optional<AttendanceRecord> findByUserIdAndSignInTimeBetween(String userId, LocalDateTime start, LocalDateTime end);
}

