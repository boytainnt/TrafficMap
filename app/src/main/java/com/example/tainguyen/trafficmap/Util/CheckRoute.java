package com.example.tainguyen.trafficmap.Util;

import android.location.Location;
import android.util.Log;

import com.example.tainguyen.trafficmap.GoogleDirection.Directions;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Tai Nguyen on 10/29/2016.
 */

public class CheckRoute {
    static double MAX_DIS = 20;
    static private class Report{
        public LatLng coor;
    }

    static double calDis(Directions.Leg.Start_location ori, Directions.Leg.End_location des, LatLng point){
        //LatLng s = new LatLng(ori.getLat(),ori.getLng());
        //LatLng d = new LatLng(des.getLat(),ori.getLng());
        Location s = new Location("");
        s.setLatitude(ori.getLat());
        s.setLongitude(ori.getLng());
        Location d = new Location("");
        d.setLatitude(des.getLat());
        d.setLongitude(des.getLng());
        Location p = new Location("");
        p.setLatitude(point.latitude);
        p.setLongitude(point.longitude);
        double sp = s.distanceTo(p);
        double sd = s.distanceTo(d);
        double dp = d.distanceTo(p);
        double P=(sp+sd+dp)/2;
        double S = Math.sqrt(P*(P-sp)*(P-sd)*(P-dp));
        double h = S*2 / dp;
        if (Math.pow(sp,2)-Math.pow(h,2) <=Math.pow(sd,2) &&  Math.pow(dp,2)-Math.pow(h,2) <= Math.pow(sd,2))
            return h;
        return 100000;
    }

    static public ArrayList countReportOnLeg(Directions.Leg.Step step, ArrayList<LatLng> reList){

        int reportCount = 0;
        ArrayList<LatLng> res = new ArrayList<>();
        for(int i=0; i<reList.size(); ++i) {
            double dis = calDis(step.getStart_location(), step.getEnd_location(), reList.get(i));
            if (dis <= MAX_DIS){
                //Log.d("found report",""+reList.get(i).latitude+","+reList.get(i).longitude);
                res.add(reList.get(i));
            }
        }
//        return reportCount;
        return res;
    }

    static public ArrayList countReportOnRoute(Directions.Route route, ArrayList<LatLng> reList){
        if (route.reportList == null) route.reportList = new ArrayList<LatLng>();

        route.reportList.clear();

        for(int i=0; i<route.legs.length; ++i){
            for(int j=0; j<route.legs[i].steps.length; ++j)
                route.reportList.addAll(countReportOnLeg(route.legs[i].steps[j],reList));
        }
        return route.reportList;
    }
    static public int onlyCountReportOnRoute(Directions.Route route, ArrayList<LatLng> reList){

        int res = 0;
        for(int i=0; i<route.legs.length; ++i){
            for(int j=0; j<route.legs[i].steps.length; ++j)
                res += countReportOnLeg(route.legs[i].steps[j],reList).size();
        }
        return res;
    }
}
