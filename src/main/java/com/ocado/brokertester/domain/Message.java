package com.ocado.brokertester.domain;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Message {
    private String id;
    private String parentId;
    private List<String> payload;
    private Date creationDate;

    public Message() {
    }

    public Message(String id, String parentId, List<String> payload) {
        this.id = id;
        this.parentId = parentId;
        this.payload = payload;
        this.creationDate = new Date();
    }

    public String getId() {
        return id;
    }

    public String getParentId() {
        return parentId;
    }

    public List<String> getPayload() {
        return payload;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", parentId='" + parentId + '\'' +
                ", payloadSize=" + payload.size() +
                ", creationDate=" + creationDate +
                ", payloadFirstEl=" + shortcut() +
                '}';
    }

    private String shortcut() {
        return payload.stream().findFirst().orElse("");
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id) &&
                Objects.equals(parentId, message.parentId) &&
                Objects.equals(payload, message.payload) &&
                Objects.equals(creationDate, message.creationDate);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, parentId, payload, creationDate);
    }
}

