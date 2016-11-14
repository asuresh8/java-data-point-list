package com.ge.pw.data;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by 212464591 on 6/21/2016.
 */
public class DateWindow implements Comparator<DateWindow>, Comparable<DateWindow>{
    public Date start;
    public Date end;
    public String type;

    public DateWindow(){}

    public DateWindow(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    public DateWindow(Date start, Date end, String type) {
        this.start = start;
        this.end = end;
        this.type = type;
    }

    public DateWindow(DateWindow d){
        this.start = d.start;
        this.end = d.end;
        this.type = d.type;
    }

    public DateWindow getWindow(){
        return new DateWindow(this.start, this.end);
    }

    public DateWindow getWindow(String type) {
        return new DateWindow(this.start, this.end, type);
    }

    public long getDuration(){
        return this.end.getTime()-this.start.getTime();
    }

    public boolean contains(Date date) {
        return (date.after(start) && date.before(end)) || (date.getTime() == this.start.getTime() || date.getTime() == this.end.getTime()) ;
    }

    public void intersect(DateWindow d) {
        if(this.start.before(d.start)) {
            this.start = d.start;
        }
        if (this.end.after(d.end)) {
            this.end = d.end;
        }
    }

    public DateWindow union(DateWindow d){
        DateWindow copy = new DateWindow(this.start, this.end, this.type);
        copy.extend(d.start);
        copy.extend(d.end);
        return copy;
    }

    public void extend(Date date) {
        if (!this.contains(date)) {
            if (date.before(start)) {
                this.start = date;
            } else if (date.after(end)) {
                this.end = date;
            }
        }
    }

    public boolean overlaps(DateWindow d, double tolerance) {
        //contains or
        boolean tempContains = contains(d.start) || contains(d.end) || d.contains(this.start) || d.contains(this.end);
        boolean tempClose = getDifference(d.start) < tolerance || getDifference(d.end) < tolerance;
        return tempContains || tempClose;
    }

    public boolean overlaps(DateWindow d){
        return contains(d.start) || contains(d.end);
    }

    public long getDifference(Date date) {
        if (!this.contains(date)) {
            if (date.before(start)) {
                return this.start.getTime() - date.getTime();
            } else {
                return date.getTime() - this.end.getTime();
            }
        } else return 0;
    }

    // Overriding the compareTo method
    public int compareTo(DateWindow d){
        return (this.start).compareTo(d.start);
    }

    // Overriding the compare method to sort the age
    public int compare(DateWindow d, DateWindow d1){
        return (int)(d.start.getTime() - d1.start.getTime());
    }

    public String toString(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String s = "";
        if (this.type != null) {
            s = s+this.type;
            s= s+"\tstart= " + dateFormat.format(start);
            s= s+"\tend= " + dateFormat.format(end);
        } else {
            s= s+"start= " + dateFormat.format(start);
            s= s+"\tend= " + dateFormat.format(end);
        }
        return s;
    }
}
