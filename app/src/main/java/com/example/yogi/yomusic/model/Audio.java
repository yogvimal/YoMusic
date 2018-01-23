package com.example.yogi.yomusic.model;

import java.io.Serializable;

/**
 * Created by YOGI on 28-12-2017.
 */

public class Audio implements Serializable {

    private String data;
    private String title;
    private String album;
    private String artist;
    private String id;
    private String album_id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlbum_id() {
        return album_id;
    }

    public void setAlbum_id(String album_id) {
        this.album_id = album_id;
    }

    public Audio(String data, String title, String album, String artist,String id,String album_id)
    {
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.id = id;
        this.album_id = album_id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
