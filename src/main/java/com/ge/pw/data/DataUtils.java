package com.ge.pw.data;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by 502659149 on 11/14/2016.
 */
public class DataUtils {

    public ArrayList<DateWindow> cleanup(ArrayList<DateWindow> windows) {
        return cleanup(windows, 0);
    }

    public ArrayList<DateWindow> cleanup(ArrayList<DateWindow> windows, long minLength) {
        return cleanup(windows, minLength, 60000);
    }

    public static ArrayList<DateWindow> cleanup(ArrayList<DateWindow> windows, long minLength, long maxCombineLength) {
        ArrayList<DateWindow> outWindows = new ArrayList<DateWindow>();
        if(windows.size() <1) {
            return outWindows;
        }
        for (int i=0; i<windows.size(); i++){
            //check if close to start of next one
            boolean windowJoined = false;
            if (windows.get(i).getDuration() > minLength) {
                for (int j=0; j<outWindows.size(); j++) {
                    if(outWindows.get(j).overlaps(windows.get(i), maxCombineLength)) {
                        DateWindow tempWindow = new DateWindow(outWindows.get(j));
                        tempWindow = tempWindow.union(windows.get(i));
                        outWindows.set(j, tempWindow);
                        windowJoined = true;
                        break;
                    }
                }
                if (!windowJoined) {
                    outWindows.add(windows.get(i));
                }
            }
        }
        return outWindows;
    }

    public ArrayList<DateWindow> cleanup(ArrayList<DateWindow> windows, long minLength, DataPointList data,
                                         double deltaThreshold) {
        ArrayList<DateWindow> outWindows = new ArrayList<DateWindow>();
        if (windows.size() < 1) {
            return outWindows;
        }
        for (DateWindow w : windows) {
            //check if close to start of next one
            boolean windowJoined = false;
            if (w.getDuration() > minLength && Math.abs(data.get(w.end).value - data.get(w.start).value) > deltaThreshold) {
                for (int j = 0; j < outWindows.size(); j++) {
                    if (outWindows.get(j).overlaps(w, 60000)) {
                        DateWindow tempWindow = new DateWindow(outWindows.get(j));
                        tempWindow = tempWindow.union(w);
                        outWindows.set(j, tempWindow);
                        windowJoined = true;
                        break;
                    }
                }
                if (!windowJoined) {
                    outWindows.add(w);
                }
            }
        }
        return outWindows;
    }

    public ArrayList<DateWindow> getConverseWindows(ArrayList<DateWindow> windows, Date firstDate, Date lastDate) {
        ArrayList<DateWindow> outWindows = new ArrayList<DateWindow>();
        if (windows.size()<1) {
            DateWindow temp = new DateWindow(firstDate, lastDate);
            outWindows.add(temp);
            return outWindows;
        }
        if (firstDate.before(windows.get(0).start)){
            outWindows.add(new DateWindow(firstDate, windows.get(0).start));
        }
        for (int i=0; i<windows.size()-1; i++) {
            outWindows.add(new DateWindow(windows.get(i).end, windows.get(i+1).start));
        }
        if (lastDate.after(windows.get(0).end)){
            outWindows.add(new DateWindow(windows.get(windows.size()-1).end, lastDate));
        }
        return outWindows;
    }


}
