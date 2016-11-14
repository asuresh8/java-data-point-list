package com.ge.pw.data;

import java.util.*;

/**
 * Created by 212464591 on 6/20/2016.
 */
public class DataPointList extends ArrayList<DataPoint> {

    public DataPointList(){
        super();
    }

    public DataPointList(List<DataPoint> points) {
        super(points);
    }

    public DataPoint get(Date date){
        if (this.size()<1) {
            return null;
        }
        int guessIndex = getNearestIndex(date);
        return estimatePoint(date, guessIndex);
    }

    public DataPoint estimatePoint(Date date, int guessIndex) {
        DataPoint guessPoint = this.get(guessIndex);
        if (guessIndex == (this.size()-1) || guessIndex ==0) {
            return new DataPoint(guessPoint.name, date, guessPoint.value, guessPoint.flag);
        }
        if (guessPoint.timestamp.after(date)) {
            DataPoint leftPoint = this.get(guessIndex-1);
            double dt = (double)Math.abs(guessPoint.timestamp.getTime() - leftPoint.timestamp.getTime());
            double dt1 = (double)Math.abs(date.getTime() - leftPoint.timestamp.getTime());
            double newValue = dt1/dt * (guessPoint.value - leftPoint.value) + leftPoint.value;
            return new DataPoint(guessPoint.name, date, newValue, leftPoint.flag && guessPoint.flag);
        } else if (guessPoint.timestamp.compareTo(date) == 0) {
            return guessPoint;
        } else {
            DataPoint rightPoint = this.get(guessIndex+1);
            double dt = (double)Math.abs(rightPoint.timestamp.getTime() - guessPoint.timestamp.getTime());
            double dt1 = (double)Math.abs(date.getTime() - guessPoint.timestamp.getTime());
            double newValue = dt1/dt * (rightPoint.value - guessPoint.value) + guessPoint.value;
            return new DataPoint(guessPoint.name, date, newValue, rightPoint.flag && guessPoint.flag);
        }
    }

    public void correctSingularities() {
        int numBad = 0;
        Date firstBad = null;
        long timeBad = 0;
        for(int i=0; i<this.size(); i++) {
            DataPoint p = this.get(i);
            //if has a bad flag
            if (p.flag) {
                if (timeBad < 600000) {
                    for (int j=(i-numBad); j< i; j++) {
                        DataPoint tempDataPoint = this.get(j);
                        tempDataPoint.value = (i-j)/(numBad)*(p.value-this.get(i-numBad).value) + this.get(i-numBad).value;
                        tempDataPoint.flag = true;
                        this.set(j, tempDataPoint);
                    }
                }
                numBad = 0;
                timeBad = 0;
                firstBad = null;
            } else {
                numBad++;
                if (firstBad == null) {
                    firstBad = this.get(i).timestamp;
                } else {
                    timeBad = timeBad + this.get(i).timestamp.getTime() - firstBad.getTime();
                }
            }
        }
    }

    public DataPointList subset(int start, int end) {
        if (start < end) {
            return (new DataPointList(this.subList(start, end)));
        } else {
            return (new DataPointList(this.subList(end, start)));
        }
    }

    public DataPointList subset(Date startDate, Date endDate) {
        //output list
        DataPointList output = new DataPointList();
        int firstIndex = getNearestIndex(startDate);
        int lastIndex = getNearestIndex(endDate);
        if (firstIndex > lastIndex) {
            int temp = firstIndex;
            firstIndex = lastIndex;
            lastIndex = temp;
        }
        DataPoint firstPoint = this.get(firstIndex);
        DataPoint lastPoint = this.get(lastIndex);
        // calculate new front point
        output.add(this.get(startDate));
        if (firstPoint.timestamp.before(startDate) && firstIndex != (this.size()-1)) {
            firstIndex++;
        }
        if (lastPoint.timestamp.after(endDate) && lastIndex != (this.size()-1)) {
            lastIndex --;
        }
        output.addAll(this.subset(firstIndex, lastIndex));
        output.add(this.get(endDate));
        return output;
    }

    public int getNearestIndex(Date date) {
        int firstIndex = 0;
        int lastIndex = this.size() - 1;

        // while more than 10 points apart, use binary search
        while ((lastIndex - firstIndex) > 10) {
            if (date.compareTo(this.get(firstIndex).timestamp)<=0) {
                return firstIndex;
            }
            if (date.compareTo(this.get(lastIndex).timestamp)> 0) {
                return lastIndex;
            }
            int guessIndex = (int)(((double)firstIndex + (double)lastIndex)/2);
            Date guessDate = this.get(guessIndex).timestamp;
            //if guess is before actual time
            if (date.after(guessDate)) {
                firstIndex = guessIndex;
            } else if (date.compareTo(guessDate) == 0) {
                return guessIndex;
            } else {
                lastIndex = guessIndex;
            }
        }
        //linear search to find sign change if less than 10 points apart
        double minDifference = Double.MAX_VALUE;
        int minIndex = firstIndex;
        for (int i = firstIndex; i < lastIndex; i++) {
            double diff = Math.abs(this.get(i).timestamp.getTime()-date.getTime());
            if (diff<minDifference) {
                minIndex = i;
                minDifference = diff;
            }
        }
        return minIndex;
    }

    public ArrayList<Double> getRunningAverage(int n) {
        ArrayList<Double> averages = new ArrayList<Double>();
        LinkedList<Double> bufferValues = new LinkedList<Double>();
        double sum = 0;
        int indexToSubtract;
        for(int i=0; i<this.size(); i++) {
            double value = this.get(i).value;
            sum += value;
            if (i<n) {
                bufferValues.add(value);
                averages.add(sum/(double)(i+1));
            } else {
                sum -= bufferValues.removeFirst();
                bufferValues.add(value);
                averages.add(sum/(double)n);
            }
        }
        return averages;
    }

    public double getAverageValue(){
        double sum = 0;
        for(int i=1; i<this.size(); i++) {
            double value = (this.get(i).value + this.get(i-1).value)/2.0
                    *((double)this.get(i).timestamp.getTime() - (double)this.get(i-1).timestamp.getTime());
            sum+= value;
        }
        double totalTime = (double)(this.get(this.size()-1).timestamp.getTime()-this.get(0).timestamp.getTime());
        if (totalTime != 0) {
            return sum/totalTime;
        } else {
            return (this.get(0).value + this.get(this.size()-1).value)/2;
        }
    }

    public DataPoint getMaxValue(){
        double maxVal = -1111;
        int index = 0;
        for(int i=0; i<this.size(); i++){
            if(this.get(i).value > maxVal) {
                index = i;
                maxVal = this.get(i).value;
            }
        }
        return this.get(index);
    }

    public int guessIndex(Date date, Date firstDate, Date lastDate, double numPoints) {
        return (int)Math.round((double)(date.getTime()-firstDate.getTime())/(double)(lastDate.getTime()-firstDate.getTime()) * (numPoints));
    }

    public ArrayList<DateWindow> getSegmentsBetween(double d1, double d2, double tolerance, String name) {
        if (this.size() < 1) {
            return new ArrayList<DateWindow>();
        }
        ArrayList<DateWindow> segments = new ArrayList<DateWindow>();
        ArrayList<HashMap<String, Integer>> goodSegments = new ArrayList<HashMap<String, Integer>>();
        ArrayList<HashMap<String, Integer>> badSegments = new ArrayList<HashMap<String, Integer>>();
        if (this.size()<=0) {
            return segments;
        }
        int i=0;
        int j=1;
        boolean prevBetween = this.get(i).value <= (d2+tolerance) && this.get(i).value >= (d1-tolerance);
        while (j<this.size()) {
            DataPoint currentPoint = this.get(j);
            if (prevBetween && (currentPoint.value > (d2+tolerance) && currentPoint.value < (d1-tolerance))) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j);
                goodSegments.add(tempIndices);
                i = j;
            } else if(!prevBetween && (currentPoint.value <= (d2+tolerance) && currentPoint.value >= (d1-tolerance))) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j);
                badSegments.add(tempIndices);
                i = j;
            }
            j++;
            prevBetween = currentPoint.value <= (d2+tolerance) && currentPoint.value >= (d1-tolerance);
        }
        if (i!=j) {
            HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
            tempIndices.put("start", i);
            tempIndices.put("end", j);
            if (this.get(i).value <= (d2+tolerance) && this.get(i).value >= (d1-tolerance)) {
                goodSegments.add(tempIndices);
            } else {
                badSegments.add(tempIndices);
            }
        }
        for(HashMap<String, Integer> seg: goodSegments){
            segments.add(new DateWindow(this.get(seg.get("start")).timestamp, this.get(seg.get("end")-1).timestamp, name));
        }
        return segments;
    }

    public ArrayList<DateWindow> getSegmentsNear(double d, double tolerance, String name) {
        if (this.size() < 1) {
            return new ArrayList<DateWindow>();
        }
        ArrayList<DateWindow> segments = new ArrayList<DateWindow>();
        ArrayList<HashMap<String, Integer>> goodSegments = new ArrayList<HashMap<String, Integer>>();
        ArrayList<HashMap<String, Integer>> badSegments = new ArrayList<HashMap<String, Integer>>();
        int i=0;
        int j=1;
        boolean prevNear = Math.abs(this.get(i).value - d) <= tolerance;
        while (j<this.size()) {
            DataPoint currentPoint = this.get(j);
            if (prevNear && Math.abs(currentPoint.value- d) > tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j);
                goodSegments.add(tempIndices);
                i = j;
            } else if(!prevNear && Math.abs(currentPoint.value - d) <= tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j);
                badSegments.add(tempIndices);
                i = j;
            }
            j++;
            prevNear = Math.abs(currentPoint.value - d) <= tolerance;
        }
        if (i!=j) {
            HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
            tempIndices.put("start", i);
            tempIndices.put("end", j);
            if (Math.abs(this.get(i).value - d) <= tolerance) {
                goodSegments.add(tempIndices);
            } else {
                badSegments.add(tempIndices);
            }
        }
        for(HashMap<String, Integer> seg: goodSegments){
            segments.add(new DateWindow(this.get(seg.get("start")).timestamp, this.get(seg.get("end")-1).timestamp, name));
        }
        return segments;
    }

    public ArrayList<DateWindow> getSegmentsGreater(double d, double tolerance, String name) {
        if (this.size() < 1) {
            return new ArrayList<DateWindow>();
        }
        ArrayList<DateWindow> segments = new ArrayList<DateWindow>();
        ArrayList<HashMap<String, Integer>> goodSegments = new ArrayList<HashMap<String, Integer>>();
        ArrayList<HashMap<String, Integer>> badSegments = new ArrayList<HashMap<String, Integer>>();
        int i=0;
        int j=1;
        boolean prevLess = this.get(i).value <= d-tolerance;
        while (j<this.size()) {
            DataPoint currentPoint = this.get(j);
            if (!prevLess && currentPoint.value <= d-tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j);
                goodSegments.add(tempIndices);
                i = j;
            } else if(prevLess && currentPoint.value > d-tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j);
                badSegments.add(tempIndices);
                i = j;
            }
            j++;
            prevLess = currentPoint.value <= d-tolerance;
        }
        if (i!=j) {
            HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
            tempIndices.put("start", i);
            tempIndices.put("end", j);
            if (this.get(i).value > d-tolerance) {
                goodSegments.add(tempIndices);
            } else {
                badSegments.add(tempIndices);
            }
        }
        for(HashMap<String, Integer> seg: goodSegments){
            segments.add(new DateWindow(this.get(seg.get("start")).timestamp, this.get(seg.get("end")-1).timestamp, name));
        }
        return segments;
    }

    public ArrayList<DateWindow> getSegmentsLess(double d, double tolerance, String name) {
        if (this.size() < 1) {
            return new ArrayList<DateWindow>();
        }
        ArrayList<DateWindow> segments = new ArrayList<DateWindow>();
        ArrayList<HashMap<String, Integer>> goodSegments = new ArrayList<HashMap<String, Integer>>();
        ArrayList<HashMap<String, Integer>> badSegments = new ArrayList<HashMap<String, Integer>>();
        int i=0;
        int j=1;
        boolean prevGreater = this.get(i).value > d+tolerance;
        while (j<this.size()) {
            DataPoint currentPoint = this.get(j);
            if (!prevGreater && currentPoint.value > d+tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j);
                goodSegments.add(tempIndices);
                i = j;
            } else if(prevGreater && currentPoint.value <= d+tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j);
                badSegments.add(tempIndices);
                i = j;
            }
            j++;
            prevGreater = currentPoint.value > d+tolerance;
        }
        if (i!=j) {
            HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
            tempIndices.put("start", i);
            tempIndices.put("end", j);
            if (this.get(i).value <= d+tolerance) {
                goodSegments.add(tempIndices);
            } else {
                badSegments.add(tempIndices);
            }
        }
        for(HashMap<String, Integer> seg: goodSegments){
            segments.add(new DateWindow(this.get(seg.get("start")).timestamp, this.get(seg.get("end")-1).timestamp, name));
        }
        return segments;
    }

    //segment data on intervals that are good and bad
    public ArrayList<DateWindow> getSegments() {
        if (this.size() < 1) {
            return new ArrayList<DateWindow>();
        }
        ArrayList<DateWindow> segments = new ArrayList<DateWindow>();
        ArrayList<HashMap<String, Integer>> goodSegments = new ArrayList<HashMap<String, Integer>>();
        ArrayList<HashMap<String, Integer>> badSegments = new ArrayList<HashMap<String, Integer>> ();
        int i=0;
        int j=1;
        boolean prevFlag = this.get(i).flag;
        while (j<this.size()) {
            DataPoint currentPoint = this.get(j);
            if (prevFlag && !currentPoint.flag) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j);
                goodSegments.add(tempIndices);
                i = j;
            } else if (!prevFlag && currentPoint.flag) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j);
                badSegments.add(tempIndices);
                i = j;
            }
            j++;
            prevFlag = currentPoint.flag;
        }
        if (i!=j) {
            HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
            tempIndices.put("start", i);
            tempIndices.put("end", j);
            if (this.get(i).flag) {
                goodSegments.add(tempIndices);
            } else {
                badSegments.add(tempIndices);
            }
        }
        for(HashMap<String, Integer> seg: goodSegments){
            segments.add(new DateWindow(this.get(seg.get("start")).timestamp, this.get(seg.get("end")-1).timestamp));
        }
        return segments;
    }

    public double getLeftDerivative(int index) {
        return 1000*(this.get(index).value - this.get(index-1).value)/(this.get(index).timestamp.getTime()- this.get(index-1).timestamp.getTime());
    }

    public double getRightDerivative(int index) {
        return 1000*(this.get(index+1).value - this.get(index).value)/(this.get(index+1).timestamp.getTime()- this.get(index).timestamp.getTime());
    }

    public double getDerivative(int index) {
        double deriv = 0;
        if(index==0) {
            deriv = getRightDerivative(index);
        } else if (index == this.size()-1) {
            deriv = getLeftDerivative(index);
        } else {
            double leftDeriv = getLeftDerivative(index);
            double rightDeriv = getRightDerivative(index);
            deriv = (leftDeriv+rightDeriv)/2;
        }
        return deriv;
    }

    public DataPointList getLeftDerivatives() {
        DataPointList points = new DataPointList();
        for (int i=1; i<this.size(); i++) {
            double deriv = getLeftDerivative(i);
            DataPoint tempPoint = new DataPoint(this.get(i));
            tempPoint.name = tempPoint.name + "_left_deriv";
            tempPoint.value = deriv;
            points.add(tempPoint);
        }
        return points;
    }

    public DataPointList getRightDerivatives() {
        DataPointList points = new DataPointList();
        for (int i=0; i<this.size()-1; i++) {
            double deriv = getRightDerivative(i);
            DataPoint tempPoint = new DataPoint(this.get(i));
            tempPoint.name = tempPoint.name + "_right_deriv";
            tempPoint.value = deriv;
            points.add(tempPoint);
        }
        return points;
    }

    public DataPointList interpolate(int milliseconds) {
        if (this.size() < 2) {
            return new DataPointList(this);
        }
        long interval = (long)milliseconds;
        long start = this.get(0).timestamp.getTime() - (this.get(0).timestamp.getTime() % interval);
        long end = this.get(this.size()-1).timestamp.getTime() + interval - (this.get(this.size()-1).timestamp.getTime() % interval);
        DataPointList temp = new DataPointList();
        temp.add(new DataPoint(this.get(0).name, new Date(start), this.get(0).value, this.get(0).flag));
        for(long t=start+interval; t< end; t+=interval) {
            temp.add(this.get(new Date(t)));
        }
        temp.add(this.get(this.size()-1));
        return temp;
    }

    public DataPointList getDerivatives() {
        DataPointList points = new DataPointList();
        for (int i=0; i<this.size(); i++) {
            double deriv = getDerivative(i);
            DataPoint tempPoint = new DataPoint(this.get(i));
            tempPoint.name = tempPoint.name + "_deriv";
            tempPoint.value = deriv;
            points.add(tempPoint);
        }
        return points;
    }

    public DataPointList merge(DataPointList d2) {
        DataPointList copy = new DataPointList(this);
        copy.addAll(d2);
        Collections.sort(copy);
        DataPointList average_copy = new DataPointList();
        DataPoint prevPoint = copy.get(0);
        for (int i=1; i<copy.size(); i++){
            if (this.get(i).timestamp.getTime() != prevPoint.timestamp.getTime()) {
                average_copy.add(prevPoint);
                if (i == copy.size()-1) {
                    average_copy.add(this.get(i));
                }
            } else{
                average_copy.add(prevPoint.merge(this.get(i)));
            }
            prevPoint = this.get(i);
        }
        return average_copy;
    }
}
