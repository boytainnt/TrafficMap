package com.trafficjammap;

import com.google.android.gms.maps.model.LatLng;


public class Info {
    public  String key;
    public LatLng pos;
    public long time;
    String urlImage;
    String nameImage;

    public Info(){
        //do something
    }

    public Info(String key, LatLng pos, long time, String urlImage, String nameImage) {
        this.key = key;
        this.pos = pos;
        this.time = time;
        this.urlImage = urlImage;
        this.nameImage = nameImage;
    }
}
