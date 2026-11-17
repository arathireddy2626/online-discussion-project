package com.onlinediscussion.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Holds the discussions that the instructor has archived.
 */
public class Archive {

    private final List<Discussion> discussions = new ArrayList<>();

    public Archive() {
    }

    public Archive(Collection<Discussion> discussions) {
        if (discussions != null) {
            this.discussions.addAll(discussions);
        }
    }

    public List<Discussion> getDiscussions() {
        return discussions;
    }

    public void setDiscussions(Collection<Discussion> newDiscussions) {
        discussions.clear();
        if (newDiscussions != null) {
            discussions.addAll(newDiscussions);
        }
    }

    /** Adds a discussion to the archive. Duplicate discussions are ignored. */
    public boolean addDiscussion(Discussion d) {
        if (d == null || discussions.contains(d)) {
            return false;
        }
        return discussions.add(d);
    }

    /** Finds an archived discussion by identifier. Returns {@code null} when absent. */
    public Discussion retrieveDiscussionByID(String id) {
        if (id == null) {
            return null;
        }
        for (Discussion discussion : discussions) {
            if (id.equals(discussion.getDiscussionID())) {
                return discussion;
            }
        }
        return null;
    }

    /** Collects every archived discussion whose topic matches the given text. */
    public List<Discussion> retrieveDiscussionByTopic(String topic) {
        List<Discussion> matches = new ArrayList<>();
        if (topic == null) {
            return matches;
        }
        for (Discussion discussion : discussions) {
            if (topic.equalsIgnoreCase(discussion.getTopic())) {
                matches.add(discussion);
            }
        }
        return matches;
    }

    @Override
    public String toString() {
        return "Archive[discussions=" + discussions.size() + "]";
    }
}
