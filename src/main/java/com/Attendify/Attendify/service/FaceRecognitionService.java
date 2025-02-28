package com.Attendify.Attendify.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.springframework.stereotype.Service;

import com.Attendify.Attendify.repository.UserFaceRepository;
import com.Attendify.Attendify.user.UserFace;

@Service
public class FaceRecognitionService {

	private final UserFaceRepository userFaceRepository;
    private static final String FACE_CASCADE_PATH = "src/main/resources/haarcascade_frontalface_default.xml";
    private static final Size FACE_SIZE = new Size(100, 100); // Standardized size for better matching

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("OpenCV Version: " + Core.VERSION);
    }

    public FaceRecognitionService(UserFaceRepository userFaceRepository) {
        this.userFaceRepository = userFaceRepository;
    }

    public String registerFaceFromWebcam(String userId) {
        VideoCapture webcam = new VideoCapture(0);

        if (!webcam.isOpened()) {
            return "Error: Failed to access webcam!";
        }

        try {
            CascadeClassifier faceDetector = new CascadeClassifier(FACE_CASCADE_PATH);
            List<byte[]> faceImages = new ArrayList<>();
            int capturedImages = 0;
            int maxImages = 5;

            while (capturedImages < maxImages) {
                Mat frame = new Mat();
                if (!webcam.read(frame)) {
                    return "Error: Failed to capture image from webcam!";
                }

                MatOfRect faceDetections = new MatOfRect();
                faceDetector.detectMultiScale(frame, faceDetections);

                if (!faceDetections.empty()) {
                    Mat faceImage = new Mat(frame, faceDetections.toArray()[0]);
                    Mat grayFace = preprocessFace(faceImage);
                    byte[] faceBytes = convertMatToByteArray(grayFace);

                    faceImages.add(faceBytes);
                    capturedImages++;
                    System.out.println("Captured image " + capturedImages);
                    Thread.sleep(500);
                }
            }

            Optional<UserFace> existingFace = userFaceRepository.findByUserId(userId);

            if (existingFace.isPresent()) {
                UserFace userFace = existingFace.get();
                userFace.getFaceImages().addAll(faceImages); // Append new images
                userFaceRepository.save(userFace);
                return "Face updated successfully!";
            } else {
                userFaceRepository.save(new UserFace(userId, faceImages));
                return "Face registered successfully!";
            }

        } catch (Exception e) {
            return "Error processing face registration: " + e.getMessage();
        } finally {
            webcam.release();
        }
    }

    public String recognizeFaceFromWebcam() {
        VideoCapture webcam = new VideoCapture(0);
        if (!webcam.isOpened()) {
            return "Failed to access webcam!";
        }

        try {
            Mat frame = new Mat();
            if (!webcam.read(frame)) {
                return "Failed to capture image!";
            }

            CascadeClassifier faceDetector = new CascadeClassifier(FACE_CASCADE_PATH);
            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(frame, faceDetections);
            System.out.println("Detected faces: " + faceDetections.toArray().length);
            if (faceDetections.empty()) {
                return "No face detected!";
            }

            for (Rect face : faceDetections.toArray()) {
                Mat faceImage = new Mat(frame, face);
                Mat processedFace = preprocessFace(faceImage);
                System.out.println("Processed face size: " + processedFace.size());

                Optional<String> recognizedUser = recognizeFace(processedFace);
                if (recognizedUser.isPresent()) {
                    return "Face recognized! User ID: " + recognizedUser.get() + ". Current Date & Time: " + java.time.LocalDateTime.now();
                }
            }

        } catch (Exception e) {
            return "Error processing face recognition: " + e.getMessage();
        } finally {
            webcam.release();
        }

        return "Face not recognized!";
    }

    private Optional<String> recognizeFace(Mat testFace) {
        List<UserFace> storedFaces = userFaceRepository.findAll();
        if (storedFaces.isEmpty()) return Optional.empty();

        List<Mat> images = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();
        Map<Integer, String> labelToUserMap = new HashMap<>();
        int label = 0;

        for (UserFace userFace : storedFaces) {
            for (byte[] faceBytes : userFace.getFaceImages()) {
                Mat faceMat = Imgcodecs.imdecode(new MatOfByte(faceBytes), Imgcodecs.IMREAD_GRAYSCALE);
                if (faceMat.empty()) continue;

                Imgproc.resize(faceMat, faceMat, FACE_SIZE);
                images.add(faceMat);
                labels.add(label);
            }
            labelToUserMap.put(label, userFace.getUserId());
            label++;
        }

        if (images.isEmpty()) return Optional.empty();

        // Train LBPH Recognizer
        LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
        MatOfInt labelsMat = new MatOfInt();
        labelsMat.fromList(labels);
        recognizer.train(images, labelsMat);

        // Predict
        int[] predictedLabel = new int[1];
        double[] confidence = new double[1];
        recognizer.predict(testFace, predictedLabel, confidence);

        System.out.println("Predicted label: " + predictedLabel[0] + ", Confidence: " + confidence[0]);

        if (predictedLabel[0] != -1 && confidence[0] < 50) { // Lower threshold for better matching
            return Optional.of(labelToUserMap.get(predictedLabel[0]));
        }

        return Optional.empty();
    }

    private Mat preprocessFace(Mat face) {
        Mat grayFace = new Mat();
        Imgproc.cvtColor(face, grayFace, Imgproc.COLOR_BGR2GRAY);
        Imgproc.resize(grayFace, grayFace, FACE_SIZE);
        return grayFace;
    }

    private byte[] convertMatToByteArray(Mat mat) throws IOException {
        File tempFile = File.createTempFile("face", ".jpg");
        Imgcodecs.imwrite(tempFile.getAbsolutePath(), mat);
        return Files.readAllBytes(tempFile.toPath());
    }
}
