package com.Attendify.Attendify.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "attendance_records")
public class AttendanceRecord {
    @Id
    private String id;
    private String userId;
    private LocalDateTime signInTime;
    private LocalDateTime signOutTime;

    public AttendanceRecord(String userId, LocalDateTime signInTime) {
        this.userId = userId;
        this.signInTime = signInTime;
        this.signOutTime = signInTime;
    }

    public String getUserId() {
        return userId;
    }

    public LocalDateTime getSignInTime() {
        return signInTime;
    }

    public LocalDateTime getSignOutTime() {
        return signOutTime;
    }

    public void setSignOutTime(LocalDateTime signOutTime) {
        this.signOutTime = signOutTime;
    }
}
