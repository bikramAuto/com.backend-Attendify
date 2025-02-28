package com.Attendify.Attendify.controller;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Attendify.Attendify.repository.UserRepository;
import com.Attendify.Attendify.service.FaceRecognitionService;
import com.Attendify.Attendify.user.User;


@RestController
@RequestMapping("/api/face")
public class FaceRecognitionController {

	private final FaceRecognitionService faceRecognitionService;
    private final UserRepository userRepository;

    public FaceRecognitionController(FaceRecognitionService faceRecognitionService, UserRepository userRepository) {
        this.faceRecognitionService = faceRecognitionService;
        this.userRepository = userRepository;
    }

    @GetMapping("/register/webcam")
    public ResponseEntity<String> registerFaceFromWebcam(@RequestParam String username) throws IOException {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (!userOptional.isPresent()) {
            return ResponseEntity.badRequest().body("User not found!");
        }

        User user = userOptional.get();
        String response = faceRecognitionService.registerFaceFromWebcam(user.getId());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/recognize/webcam")
    public ResponseEntity<String> recognizeFaceFromWebcam() throws IOException {
        String response = faceRecognitionService.recognizeFaceFromWebcam();
		return ResponseEntity.ok(response);
    }
}
