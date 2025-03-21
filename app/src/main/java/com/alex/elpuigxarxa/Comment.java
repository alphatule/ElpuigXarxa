package com.alex.elpuigxarxa;

public class Comment {
    public String id;
    public String postId;
    public String parentCommentId;
    public String author;
    public String authorPhotoUrl;
    public String uid;
    public String content;
    public String timestamp;

    public Comment(String id, String postId, String parentCommentId, String author, String authorPhotoUrl, String uid, String content, String timestamp) {
        this.id = id;
        this.postId = postId;
        this.parentCommentId = parentCommentId;
        this.author = author;
        this.authorPhotoUrl = authorPhotoUrl;
        this.uid = uid;
        this.content = content;
        this.timestamp = timestamp;
    }
}
