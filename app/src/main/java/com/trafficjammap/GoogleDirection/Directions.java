package com.trafficjammap.GoogleDirection;

import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;

/**
 * Created by Tai Nguyen on 10/29/2016.
 */
public final class Directions {
    public final Route routes[];
    public final String status;


    public static final int getColor(int routeth){
        switch (routeth){
            case 0: return Color.RED;
            case 1: return Color.CYAN;
            case 2: return Color.BLUE;
            default: return Color.BLACK;
        }
    }

    public Route[] getRoutes() {
        return routes;
    }
    public String getStatus() {
        return status;
    }
    public Directions(Route[] routes, String status){
        this.routes = routes;
        this.status = status;
    }

    public static final class Route {
        public final Leg legs[];
        public ArrayList<LatLng> reportList = new ArrayList<>();

        public Leg[] getLegs() {
            return legs;
        }
        public Route(Leg[] legs){
            this.legs = legs;
        }
    }
    public static final class Leg {
        public final String end_address;
        public final End_location end_location;
        public final String start_address;
        public final Start_location start_location;
        public final Step steps[];


        public String getEnd_address() {
            return end_address;
        }

        public End_location getEnd_location() {
            return end_location;
        }

        public String getStart_address() {
            return start_address;
        }

        public Start_location getStart_location() {
            return start_location;
        }

        public Step[] getSteps() {
            return steps;
        }

        public Leg(String end_address, End_location end_location, String start_address, Start_location start_location, Step[] steps){
            this.end_address = end_address;
            this.end_location = end_location;
            this.start_address = start_address;
            this.start_location = start_location;
            this.steps = steps;
        }

        public static final class End_location {
            public final double lat;
            public final double lng;

            public End_location(double lat, double lng){
                this.lat = lat;
                this.lng = lng;
            }

            public double getLat() {
                return lat;
            }

            public double getLng() {
                return lng;
            }
        }

        public static final class Start_location {
            public final double lat;
            public final double lng;

            public Start_location(double lat, double lng){
                this.lat = lat;
                this.lng = lng;
            }

            public double getLat() {
                return lat;
            }

            public double getLng() {
                return lng;
            }

        }

        public static final class Polyline {
            public final String points;

            public String getPoints() {
                return points;
            }

            public Polyline(String points) {
                this.points = points;
            }
        }

        public static final class Step {
            public final End_location end_location;
            public final String html_instructions;
            public final Start_location start_location;
            public final String travel_mode;
            public final String maneuver;
            public final Polyline polyline;


            public End_location getEnd_location() {
                return end_location;
            }


            public String getHtml_instructions() {
                return html_instructions;
            }


            public Start_location getStart_location() {
                return start_location;
            }


            public String getTravel_mode() {
                return travel_mode;
            }

            public String getManeuver() {
                return maneuver;
            }

            public Polyline getPolyline(){
                return polyline;
            }

            public Step(End_location end_location, String html_instructions, Start_location start_location, String travel_mode, String maneuver, Polyline polyline){
                this.end_location = end_location;
                this.html_instructions = html_instructions;
                this.start_location = start_location;
                this.travel_mode = travel_mode;
                this.maneuver = maneuver;
                this.polyline = polyline;
            }
        }
    }
}
