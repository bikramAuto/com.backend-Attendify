package com.Attendify.Attendify.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.Attendify.Attendify.repository.UserRepository;
import com.Attendify.Attendify.service.FaceRecognitionService;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/face/recognize")
public class FaceRecognitionController {

	private final FaceRecognitionService faceRecognitionService;

    public FaceRecognitionController(FaceRecognitionService faceRecognitionService, UserRepository userRepository) {
        this.faceRecognitionService = faceRecognitionService;
        
    }
    
    @GetMapping("/webcam")
    public ResponseEntity<String> recognizeFaceFromWebcam() throws IOException {
        String response = faceRecognitionService.recognizeFaceFromWebcam();
		return ResponseEntity.ok(response);
    }
    
    @PostMapping("/upload")
    public ResponseEntity<?> recognizeFaceFromImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body("Uploaded file is empty.");
            Mat testFace = Imgcodecs.imdecode(new MatOfByte(file.getBytes()), Imgcodecs.IMREAD_GRAYSCALE);
            if (testFace.empty()) return ResponseEntity.badRequest().body("Invalid image format or unreadable image.");
            Optional<String> userId = faceRecognitionService.recognizeFace(testFace);
            return ResponseEntity.ok(Collections.singletonMap("user", userId.orElse("Unknown")));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process image.");
        }
    }

}
