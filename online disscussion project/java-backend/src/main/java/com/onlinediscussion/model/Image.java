package com.onlinediscussion.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * A binary picture that can be attached to a {@link Message}.
 *
 * <p>Persisted in the {@code Message} table as the triple {@code ImageName}, {@code ImageType}
 * and {@code ImageData}.
 */
public class Image {

    private String name;
    private String type;
    private byte[] data;

    public Image() {
    }

    public Image(String name, String type, byte[] data) {
        this.name = name;
        this.type = type;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getSize() {
        return data == null ? 0 : data.length;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Image)) {
            return false;
        }
        Image that = (Image) other;
        return Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(name, type) + Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return "Image[name=" + name + ", type=" + type + ", bytes=" + getSize() + "]";
    }
}
