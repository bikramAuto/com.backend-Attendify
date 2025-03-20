package com.Attendify.Attendify.service;

import java.io.IOException;
import java.nio.*;
import java.util.*;
import java.util.Optional;

import org.bytedeco.javacpp.Loader;
import org.opencv.core.*;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import com.Attendify.Attendify.repository.UserFaceRepository;
import com.Attendify.Attendify.repository.UserRepository;
import com.Attendify.Attendify.user.UserFace;

@Service
public class FaceRecognitionService {

    private final Net faceDetector;
    private final Net faceRecognizer;
    private final UserFaceRepository userFaceRepository;
    private static final Size FACE_SIZE = new Size(100, 100);
//    private final String FACE_CASCADE_PATH = "src/main/resources/haarcascade_frontalface_default.xml";

    static {
        Loader.load(org.bytedeco.opencv.opencv_java.class);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("OpenCV Version: " + Core.VERSION);
    }

    @Autowired
    public FaceRecognitionService(UserFaceRepository userFaceRepository, ResourceLoader resourceLoader, UserRepository userRepository) throws IOException {
        this.userFaceRepository = userFaceRepository;
        Resource detectorProtoResource = resourceLoader.getResource("classpath:models/deploy.prototxt");
        Resource detectorModelResource = resourceLoader.getResource("classpath:models/res10_300x300_ssd_iter_140000.caffemodel");
        Resource recognizerModelResource = resourceLoader.getResource("classpath:models/face_recognition_sface_2021dec.onnx");
        this.faceDetector = Dnn.readNetFromCaffe(detectorProtoResource.getFile().getAbsolutePath(), detectorModelResource.getFile().getAbsolutePath());
        this.faceRecognizer = Dnn.readNetFromONNX(recognizerModelResource.getFile().getAbsolutePath());
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

    public Optional<String> recognizeFace(Mat faceImage) {
        if (faceImage == null || faceImage.empty()) {
            return Optional.empty();
        }
        
        Mat blob = Dnn.blobFromImage(faceImage, 1.0, new Size(112, 112), new Scalar(0, 0, 0), true, false);
        faceRecognizer.setInput(blob);
        Mat output = faceRecognizer.forward();

        // Extract face features
        float[] faceFeatures = new float[(int) output.total()];
        output.get(0, 0, faceFeatures);

        // Extract the matched user ID from Optional
        return matchFace(faceFeatures); 
    }

    private Optional<String> matchFace(float[] faceFeatures) {
	    List<UserFace> storedFaces = userFaceRepository.findAll();
	
	    double bestMatchScore = -1.0; 
	    String bestMatchUserId = null;
	
	    for (UserFace userFace : storedFaces) {
	        for (byte[] faceData : userFace.getFaceImages()) {
	            float[] storedEmbedding = convertByteArrayToFloatArray(faceData);
	            double score = calculateCosineSimilarity(faceFeatures, storedEmbedding);
	            if (score > bestMatchScore) { 
	                bestMatchScore = score;
	                bestMatchUserId = userFace.getUserId();
	            }
	        }
	    }
	
	    return bestMatchScore > 0.5 ? Optional.ofNullable(bestMatchUserId) : Optional.empty();
	}

	private float[] convertByteArrayToFloatArray(byte[] byteArray) {
	    ByteBuffer buffer = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN);
	    float[] floatArray = new float[byteArray.length / 4];
	    for (int i = 0; i < floatArray.length; i++) {
	        floatArray[i] = buffer.getFloat();
	    }
	    return floatArray;
	}

    private double calculateCosineSimilarity(float[] vec1, float[] vec2) {
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            normA += Math.pow(vec1[i], 2);
            normB += Math.pow(vec2[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
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

    public String recognizeFaceFromWebcam() {
        VideoCapture webcam = new VideoCapture(0, Videoio.CAP_DSHOW);
        if (!webcam.isOpened()) {
            return "Error: Failed to access webcam!";
        }
        try {
            Mat frame = new Mat();
            if (!webcam.read(frame)) {
                return "Error: Failed to capture image from webcam!";
            }
            Rect[] detectedFaces = detectFacesDNN(frame);
            if (detectedFaces.length == 0) {
                return "No face detected!";
            }
            Rect faceRect = detectedFaces[0];
            Mat faceImage = new Mat(frame, new org.opencv.core.Rect(faceRect.x, faceRect.y, faceRect.width, faceRect.height));
            Mat grayFace = preprocessFace(faceImage);
            Optional<String> recognizedUserId = recognizeFace(grayFace);
            return recognizedUserId.orElse("Face not recognized!");
        } catch (Exception e) {
            return "Error processing face recognition: " + e.getMessage();
        } finally {
            webcam.release();
        }
    }

}
