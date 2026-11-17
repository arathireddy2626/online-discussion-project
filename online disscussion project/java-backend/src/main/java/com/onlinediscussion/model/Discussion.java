package com.onlinediscussion.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A discussion thread inside a {@link Course}.
 *
 * <p>{@code discussionID} uniquely identifies an object of this class, so it has no setter.
 * {@code topic} and {@code dateAndTime} are set at creation time.
 */
public class Discussion {

    private String discussionID;
    private String topic;
    private String dateAndTime;
    private final List<Message> messages = new ArrayList<>();

    public Discussion() {
    }

    public Discussion(String discussionID, String topic, String dateAndTime) {
        this.discussionID = discussionID;
        this.topic = topic;
        this.dateAndTime = dateAndTime;
    }

    public String getDiscussionID() {
        return discussionID;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDateAndTime() {
        return dateAndTime;
    }

    public void setDateAndTime(String dateAndTime) {
        this.dateAndTime = dateAndTime;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(Collection<Message> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(newMessages);
        }
    }

    /** Appends a message to this discussion. */
    public void addMessage(Message message) {
        if (message != null) {
            messages.add(message);
        }
    }

    /** Posts a message into this discussion. */
    public void postMessage(Message m) {
        addMessage(m);
    }

    /** Removes the given message from this discussion, if present. Returns true when removed. */
    public boolean removeMessage(Message message) {
        return messages.remove(message);
    }

    /**
     * Removes the message that carries the given identifier. Only the instructor may call
     * this. Returns true when a message was removed.
     */
    public boolean removeMessageByMessageID(String id) {
        Message message = searchMessageByMessageID(id);
        return message != null && messages.remove(message);
    }

    /**
     * Removes every message posted by the given user name. Only the instructor may call this.
     * Returns the number of messages that were removed.
     */
    public int removeMessageByUsername(String uname) {
        List<Message> matches = searchMessagesByUsername(uname);
        messages.removeAll(matches);
        return matches.size();
    }

    /** Finds a message by its identifier. Returns {@code null} when there is no match. */
    public Message searchMessageByMessageID(String id) {
        if (id == null) {
            return null;
        }
        for (Message message : messages) {
            if (id.equals(message.getMessageID())) {
                return message;
            }
        }
        return null;
    }

    /** Finds the first message posted by the given user name. Returns {@code null} when absent. */
    public Message searchMessageByUsername(String uname) {
        if (uname == null) {
            return null;
        }
        for (Message message : messages) {
            if (uname.equals(message.getPostedBy())) {
                return message;
            }
        }
        return null;
    }

    /** Collects every message posted by the given user name. */
    public List<Message> searchMessagesByUsername(String uname) {
        List<Message> matches = new ArrayList<>();
        if (uname == null) {
            return matches;
        }
        for (Message message : messages) {
            if (uname.equals(message.getPostedBy())) {
                matches.add(message);
            }
        }
        return matches;
    }

    @Override
    public String toString() {
        return "Discussion[discussionID=" + discussionID
                + ", topic=" + topic
                + ", dateAndTime=" + dateAndTime
                + ", messages=" + messages.size()
                + "]";
    }
}
