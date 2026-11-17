package com.onlinediscussion.model;

/**
 * A single post inside a {@link Discussion}.
 *
 * <p>{@code messageID} uniquely identifies an object of this class, so it has no setter.
 * Both {@code textualContents} and {@code imageContents} are nullable, but a message must carry
 * at least one of them.
 */
public class Message {

    private String messageID;
    private String discussionID;
    private String dateAndTime;
    private String postedBy;
    private String textualContents;
    private Image imageContents;

    public Message() {
    }

    public Message(String messageID, String discussionID, String dateAndTime, String postedBy,
            String textualContents, Image imageContents) {
        this.messageID = messageID;
        this.discussionID = discussionID;
        this.dateAndTime = dateAndTime;
        this.postedBy = postedBy;
        this.textualContents = textualContents;
        this.imageContents = imageContents;
    }

    public String getMessageID() {
        return messageID;
    }

    public String getDiscussionID() {
        return discussionID;
    }

    public void setDiscussionID(String discussionID) {
        this.discussionID = discussionID;
    }

    public String getDateAndTime() {
        return dateAndTime;
    }

    public void setDateAndTime(String dateAndTime) {
        this.dateAndTime = dateAndTime;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    public String getTextualContents() {
        return textualContents;
    }

    public void setTextualContents(String textualContents) {
        this.textualContents = textualContents;
    }

    public Image getImageContents() {
        return imageContents;
    }

    public void setImageContents(Image imageContents) {
        this.imageContents = imageContents;
    }

    @Override
    public String toString() {
        return "Message[messageID=" + messageID
                + ", discussionID=" + discussionID
                + ", postedBy=" + postedBy
                + ", dateAndTime=" + dateAndTime
                + ", text=" + textualContents
                + (imageContents == null ? "" : ", image=" + imageContents)
                + "]";
    }
}
