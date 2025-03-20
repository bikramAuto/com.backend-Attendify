package com.Attendify.Attendify.controller;

import java.io.IOException;
import java.util.Optional;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
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
import com.Attendify.Attendify.service.FaceRegistrationService;
import com.Attendify.Attendify.user.User;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/face/register")
public class FaceRegistrationController {

	private final UserRepository userRepository;
	private final FaceRegistrationService faceRegistrationService;
	
	public FaceRegistrationController(UserRepository userRepository, FaceRegistrationService faceRegistrationService) {
		this.userRepository = userRepository;
		this.faceRegistrationService = faceRegistrationService;
	}
	
	 @PostMapping("/upload")
    public ResponseEntity<String> registerFaceFromUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username) {

        System.out.println("Received username: " + username);
        if (file.isEmpty() || username == null || username.isBlank()) return ResponseEntity.badRequest().body("Image and username are required.");    
        Optional<User> userOptional = userRepository.findByUsername(username);        
        if (!userOptional.isPresent()) return ResponseEntity.badRequest().body("User not found for username: " + username);
        User user = userOptional.get();
        System.out.println("Found user with ID: " + user.getId());
        try {
            byte[] imageBytes = file.getBytes();
            Mat frame = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
            Rect[] detectedFaces = faceRegistrationService.detectFacesDNN(frame);
            if (detectedFaces.length == 0) {
                return ResponseEntity.badRequest().body("No face detected in the image.");
            }
            String response = faceRegistrationService.registerFace(username, file);
            return ResponseEntity.ok("from controller Face registered successfully for " + response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing image.");
        }
    }

    @GetMapping("/webcam")
    public ResponseEntity<String> registerFaceFromWebcam(@RequestParam String username) throws IOException {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (!userOptional.isPresent()) return ResponseEntity.badRequest().body("User not found!");
        User user = userOptional.get();
        String response = faceRegistrationService.registerFaceFromWebcam(user.getId());
        return ResponseEntity.ok(response);
    }
    
    
}
