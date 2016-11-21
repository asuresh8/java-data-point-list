package com.ge.pw.data;

import java.util.Comparator;
import java.util.Date;

/**
 * Created by 212464591 on 6/16/2016.
 */
public class Point implements Comparator<Point>, Comparable<Point>{

    public Date timestamp;
    public double value;
    public boolean flag;
    public String name;

    public Point(){}

    public Point(String name, Date timestamp, double value, boolean flag) {
        this.name = name;
        this.timestamp = timestamp;
        this.value = value;
        this.flag = flag;
    }

    public Point(Point d) {
        this.timestamp = d.timestamp;
        this.value = d.value;
        this.flag = d.flag;
        this.name = d.name;
    }

    public Point merge(Point d){
        if (!d.flag) {
            return new Point(this);
        } else if (!this.flag) {
            return new Point(d);
        } else {
            double average_value = (this.value + d.value)/2;
            return new Point(this.name, this.timestamp, average_value, true);
        }
    }

    public Point merge(Point d, Date targetDate) {
        if (!d.flag) {
            return new Point(this.name, targetDate, this.value, this.flag);
        } else if (!this.flag) {
            return new Point(d.name, targetDate, d.value, true);
        } else {
            long delta1 = Math.abs(targetDate.getTime() - this.timestamp.getTime());
            long delta2 = Math.abs(targetDate.getTime() - d.timestamp.getTime());
            double average_value = (this.value * (double)delta1 + d.value*(double)delta2);
            return new Point(this.name, targetDate, average_value, true);
        }
    }

    // Overriding the compareTo method
    public int compareTo(Point d){
        return (this.timestamp).compareTo(d.timestamp);
    }

    // Overriding the compare method to sort the age
    public int compare(Point d, Point d1){
        return (int)(d.timestamp.getTime() - d1.timestamp.getTime());
    }

    public String toString(){
        return name+"\t"+timestamp+"\t"+value+"\t"+flag;
    }
}
