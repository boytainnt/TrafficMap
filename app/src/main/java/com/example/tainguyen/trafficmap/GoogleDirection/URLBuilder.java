package com.example.tainguyen.trafficmap.GoogleDirection;

/**
 * Created by Tai Nguyen on 10/29/2016.
 */

public class URLBuilder {
    private static final String PREFIX = "https://maps.googleapis.com/maps/api/directions/json?";
    private static final String API_KEY = "AIzaSyAlF7iV1TtZAiLFwCY8TtKvg55L9QfO-JI";

    public static String getURL(String origin, String dest) {
        StringBuffer buff = new StringBuffer();
        buff.append(PREFIX);
        buff.append("origin=");
        buff.append(origin);
        buff.append("&destination=");
        buff.append(dest);
        buff.append("&alternatives=true");
        buff.append("&key=");
        buff.append(API_KEY);

        return buff.toString();
    }
}
