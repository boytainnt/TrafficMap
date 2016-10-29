package com.example.tainguyen.trafficmap.Util;

import com.example.tainguyen.trafficmap.GoogleDirection.Directions;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Tai Nguyen on 10/29/2016.
 */

public class CheckRoute {
    static double MAX_DIS = 0.005;
    static double DisperLat = 6371*2*3.14/360;
    static private class Report{
        public LatLng coor;
    }

    static double calDis(Directions.Leg.Start_location ori, Directions.Leg.End_location des, LatLng point, double mau){
        double dis = Math.abs((des.getLng() -ori.getLng())*point.latitude - (des.getLat() - ori.getLat())*point.longitude + des.getLat()*ori.getLng() - des.getLng()*ori.getLat())/mau;
        return dis*DisperLat;
    }

    static public int countReportOnLeg(Directions.Leg leg, ArrayList<LatLng> reList){
        double x1 = leg.getStart_location().getLat();
        double y1 = leg.getStart_location().getLng();
        double x2 = leg.getEnd_location().getLat();
        double y2 = leg.getEnd_location().getLng();
        double mau = Math.sqrt(Math.pow(x2-x1,2.0) + Math.pow(y1-y2,2.0));
        int reportCount = 0;
        for(int i=0; i<reList.size(); ++i) {
            double dis = calDis(leg.getStart_location(), leg.getEnd_location(), reList.get(i), mau);
            if (dis <= MAX_DIS) ++reportCount;
        }
        return reportCount;
    }

    static public void countReportOnRoute(Directions.Route route, ArrayList<LatLng> reList){
        route.report = 0;
        for(int i=0; i<route.legs.length; ++i){
            route.report += countReportOnLeg(route.legs[i],reList);
        }
    }
}
