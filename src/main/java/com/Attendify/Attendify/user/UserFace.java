package com.Attendify.Attendify.user;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_faces")
public class UserFace {

    @Id
    private String id;
	private String userId;
	private List<byte[]> faceImages;
	private List<Double> faceEmbeddings;

	public UserFace(String userId, List<byte[]> faceImages) {
        this.userId = userId;
        this.faceImages = faceImages;
    }
    
    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Double> getFaceEmbeddings() {
		return faceEmbeddings;
	}

	public void setFaceEmbeddings(List<Double> faceEmbeddings) {
		this.faceEmbeddings = faceEmbeddings;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public List<byte[]> getFaceImages() {
        return faceImages;
    }

    public void setFaceImages(List<byte[]> faceImages) {
        this.faceImages = faceImages;
    }

}

