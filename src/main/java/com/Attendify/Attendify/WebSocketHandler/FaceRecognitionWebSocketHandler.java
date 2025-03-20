package com.Attendify.Attendify.WebSocketHandler;

import com.Attendify.Attendify.user.AttendanceRecord;
import com.Attendify.Attendify.repository.AttendanceRepository;
import com.Attendify.Attendify.service.FaceRecognitionService;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.Optional;

@Component
public class FaceRecognitionWebSocketHandler extends BinaryWebSocketHandler {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(50 * 1024 * 1024);
        session.setBinaryMessageSizeLimit(50 * 1024 * 1024);
        System.out.println("WebSocket connection established: " + session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        ByteBuffer imageData = message.getPayload();
        System.out.println("Received binary data of size: " + imageData.remaining());
        Mat processedFace = faceRecognitionService.preprocessFace(byteBufferToMat(imageData));        
        if (processedFace.empty()) {
            session.sendMessage(new TextMessage("{\"error\": \"No face detected\"}"));
            return;
        }
        if (processedFace.channels() == 1) Imgproc.cvtColor(processedFace, processedFace, Imgproc.COLOR_GRAY2BGR);
        Optional<String> recognizedUserId = faceRecognitionService.recognizeFace(processedFace);
        if (recognizedUserId.isPresent()) {
            String userId = recognizedUserId.get();
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDate today = currentTime.toLocalDate();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

            Optional<AttendanceRecord> existingRecord = attendanceRepository.findByUserIdAndSignInTimeBetween(userId, startOfDay, endOfDay);

            if (existingRecord.isPresent()) {
                AttendanceRecord record = existingRecord.get();
                if (record.getSignOutTime() == null || currentTime.isAfter(record.getSignOutTime())) {
                    record.setSignOutTime(currentTime);
                    attendanceRepository.save(record);
                }
            } else {
                AttendanceRecord newRecord = new AttendanceRecord(userId, currentTime);
                attendanceRepository.save(newRecord);
            }

            String jsonResponse = "{\"userId\": \"" + userId + "\", \"timestamp\": \"" + currentTime + "\"}";
            session.sendMessage(new TextMessage(jsonResponse));
        } else {
            session.sendMessage(new TextMessage("{\"error\": \"Face not recognized\"}"));
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("WebSocket closed: " + session.getId() + " with status: " + status);
    }
 
    private Mat byteBufferToMat(ByteBuffer buffer) {
        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);

        Mat imageMat = Imgcodecs.imdecode(new MatOfByte(byteArray), Imgcodecs.IMREAD_COLOR);
        if (imageMat.empty()) {
            System.err.println("Failed to decode image from ByteBuffer");
            return new Mat();
        }
        return imageMat;
    }
    
}
