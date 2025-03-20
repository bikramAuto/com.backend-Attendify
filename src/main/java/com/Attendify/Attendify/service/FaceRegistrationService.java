package com.Attendify.Attendify.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bytedeco.javacpp.Loader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.Attendify.Attendify.repository.UserFaceRepository;
import com.Attendify.Attendify.repository.UserRepository;
import com.Attendify.Attendify.user.User;
import com.Attendify.Attendify.user.UserFace;

@Service
public class FaceRegistrationService {
	
	private final Net faceDetector;
    private final UserRepository userRepository;
    private final UserFaceRepository userFaceRepository;
    private static final Size FACE_SIZE = new Size(100, 100);

	static {
        Loader.load(org.bytedeco.opencv.opencv_java.class);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("OpenCV Version: " + Core.VERSION);
    }
	
	@Autowired
	public FaceRegistrationService(UserFaceRepository userFaceRepository, ResourceLoader resourceLoader, UserRepository userRepository) throws IOException {
		this.userFaceRepository = userFaceRepository;
        this.userRepository = userRepository;
        Resource detectorProtoResource = resourceLoader.getResource("classpath:models/deploy.prototxt");
        Resource detectorModelResource = resourceLoader.getResource("classpath:models/res10_300x300_ssd_iter_140000.caffemodel");
        this.faceDetector = Dnn.readNetFromCaffe(
        		detectorProtoResource.getFile().getAbsolutePath(), 
        		detectorModelResource.getFile().getAbsolutePath());
	}
	
	public String registerFace(String username, MultipartFile file) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return "Error: User not found!";
        }
        String userId = userOptional.get().getId();

        try {
            byte[] imageBytes = file.getBytes();
            Mat frame = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
            Rect[] detectedFaces = detectFacesDNN(frame);
            if (detectedFaces.length == 0) return "Error: No face detected in the image.";

            Mat faceImage = new Mat(frame, detectedFaces[0]);
            Mat processedFace = preprocessFace(faceImage);
            byte[] faceBytes = convertMatToByteArray(processedFace);
            saveFaceData(userId, faceBytes);
            return "from service Face registered successfully for " + username;

        } catch (IOException e) {
            return "Error: Unable to process image - " + e.getMessage();
        }
    }
    
    private void saveFaceData(String userId, byte[] faceImage) {
        Optional<UserFace> existingFace = userFaceRepository.findByUserId(userId);
        if (existingFace.isPresent()) {
            UserFace userFace = existingFace.get();
            List<byte[]> currentImages = userFace.getFaceImages();
            currentImages.add(faceImage);
            if (currentImages.size() > 20) {
                currentImages = currentImages.subList(currentImages.size() - 20, currentImages.size());
            }
            userFace.setFaceImages(currentImages);
            userFaceRepository.save(userFace);
        } else {
            List<byte[]> newImages = new ArrayList<>();
            newImages.add(faceImage);
            userFaceRepository.save(new UserFace(userId, newImages));
        }
    }

    public String registerFaceFromWebcam(String userId) {
	    System.out.println("Opening webcam...");
	    VideoCapture webcam = new VideoCapture(0, Videoio.CAP_DSHOW);
	    if (!webcam.isOpened()) return "Error: Failed to access webcam!";
	    try {
	        List<byte[]> faceImages = new ArrayList<>();
	        int capturedImages = 0;
	        int maxImages = 3;
	        int maxRetries = 20;
	        int attempts = 0;
	
	        while (capturedImages < maxImages && attempts < maxRetries) {
	            Mat frame = new Mat();
	            if (!webcam.read(frame)) return "Error: Failed to capture image from webcam!";
	            Rect[] detectedFaces = detectFacesDNN(frame);
	            if (detectedFaces.length > 0) {
	                Rect faceRect = detectedFaces[0];
	                Mat faceImage = new Mat(frame, new Rect(faceRect.x, faceRect.y, faceRect.width, faceRect.height));
	                Mat grayFace = preprocessFace(faceImage);
	                byte[] faceBytes = convertMatToByteArray(grayFace);
	
	                faceImages.add(faceBytes);
	                capturedImages++;
	                System.out.println("Captured image " + capturedImages);
	                Thread.sleep(500);
	            } else {
	                attempts++;
	                System.out.println("No face detected, retrying... (" + attempts + "/" + maxRetries + ")");
	                Thread.sleep(500);
	            }
	        }
	        if (capturedImages == 0) return "Error: No face detected after " + maxRetries + " attempts.";
	        System.out.println("Face capture complete, saving data...");
	        Optional<UserFace> existingFace = userFaceRepository.findByUserId(userId);
	        if (existingFace.isPresent()) {
	            UserFace userFace = existingFace.get();
	            List<byte[]> currentImages = userFace.getFaceImages();
	
	            // Maintain last 20 images
	            currentImages.addAll(faceImages);
	            if (currentImages.size() > 20) {
	                currentImages = currentImages.subList(currentImages.size() - 20, currentImages.size());
	            }
	
	            userFace.setFaceImages(currentImages);
	            userFaceRepository.save(userFace);
	            System.out.println("Face updated successfully!");
	            return "Face updated successfully!";
	        } else {
	            userFaceRepository.save(new UserFace(userId, faceImages));
	            System.out.println("Face registered successfully!");
	            return "Face registered successfully!";
	        }
	
	    } catch (Exception e) {
	        System.err.println("Error processing face registration: " + e.getMessage());
	        return "Error processing face registration: " + e.getMessage();
	    } finally {
	        webcam.release();
	        System.out.println("Webcam released.");
	    }
	}

    public Rect[] detectFacesDNN(Mat frame) {
        Mat blob = Dnn.blobFromImage(frame, 1.0, new Size(300, 300), new Scalar(104.0, 177.0, 123.0), true, false);
        faceDetector.setInput(blob);
        Mat detection = faceDetector.forward();
        detection = detection.reshape(1, (int) detection.size(2));
        List<Rect> faces = new ArrayList<>();
        float[] data = new float[7];
        for (int i = 0; i < detection.rows(); i++) {
            detection.get(i, 0, data);
            float confidence = data[2];
            if (confidence > 0.5) {
                int x1 = (int) (data[3] * frame.cols());
                int y1 = (int) (data[4] * frame.rows());
                int x2 = (int) (data[5] * frame.cols());
                int y2 = (int) (data[6] * frame.rows());

                x1 = Math.max(0, x1);
                y1 = Math.max(0, y1);
                x2 = Math.min(frame.cols() - 1, x2);
                y2 = Math.min(frame.rows() - 1, y2);

                if (x2 > x1 && y2 > y1) faces.add(new Rect(x1, y1, x2 - x1, y2 - y1));
            }
        }

        return faces.toArray(new Rect[0]);
    }
    
    public Mat preprocessFace(Mat face) {
        Mat processedFace = new Mat();
        if (face.channels() == 1) {
            Imgproc.cvtColor(face, processedFace, Imgproc.COLOR_BGR2GRAY);
        } else {
            processedFace = face.clone();
        }
        Imgproc.resize(processedFace, processedFace, FACE_SIZE);
        return processedFace;
    }
    
    private byte[] convertMatToByteArray(Mat mat) throws IOException {
        File tempFile = File.createTempFile("face", ".jpg");
        Imgcodecs.imwrite(tempFile.getAbsolutePath(), mat);
        return Files.readAllBytes(tempFile.toPath());
    }
    
    
}
