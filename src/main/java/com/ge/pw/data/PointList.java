package com.ge.pw.data;

import java.util.*;

/**
 * Created by 212464591 on 6/20/2016.
 */
public class PointList extends ArrayList<Point> {

    public PointList(){
        super();
    }

    public PointList(List<Point> points) {
        super(points);
    }

    public Point get(Date date){
        if (this.size()<1) {
            return null;
        }
        int guessIndex = getNearestIndex(date);
        return estimatePoint(date, guessIndex);
    }

    public Point estimatePoint(Date date, int guessIndex) {
        Point guessPoint = this.get(guessIndex);
        if (guessIndex == (this.size()-1) || guessIndex ==0) {
            return new Point(guessPoint.name, date, guessPoint.value, guessPoint.flag);
        }
        if (guessPoint.timestamp.after(date)) {
            Point leftPoint = this.get(guessIndex-1);
            double dt = (double) Math.abs(guessPoint.timestamp.getTime() - leftPoint.timestamp.getTime());
            double dt1 = (double) Math.abs(date.getTime() - leftPoint.timestamp.getTime());
            double newValue = dt1/dt * (guessPoint.value - leftPoint.value) + leftPoint.value;
            return new Point(guessPoint.name, date, newValue, leftPoint.flag && guessPoint.flag);
        } else if (guessPoint.timestamp.compareTo(date) == 0) {
            return guessPoint;
        } else {
            Point rightPoint = this.get(guessIndex+1);
            double dt = (double) Math.abs(rightPoint.timestamp.getTime() - guessPoint.timestamp.getTime());
            double dt1 = (double) Math.abs(date.getTime() - guessPoint.timestamp.getTime());
            double newValue = dt1/dt * (rightPoint.value - guessPoint.value) + guessPoint.value;
            return new Point(guessPoint.name, date, newValue, rightPoint.flag && guessPoint.flag);
        }
    }

    public void correctSingularities() {
        // remove duplicated values
        this.removeDuplicates();
        // fix all places where duration of BAD is less than 5 minutes
        int numBad = 0;
        Date firstBad = null;
        long timeBad = 0;
        int i=0;
        // iterate through the first n bad points
        while(i < this.size() && !this.get(i).flag) {
            i++;
        }
        // all points were bad, then just return
        if (i==this.size()) {
            return;
        } else {
            // set all the bad values to the first good value
            for (int j=0; j< i; j++) {
                Point tempPoint = this.get(j);
                tempPoint.value = this.get(i).value;
                tempPoint.flag = this.get(i).flag;
                this.set(j, tempPoint);
            }
        }
        // iterate through the last n bad points
        int k = this.size() - 1;
        while(k>-1 && !this.get(k).flag) {
            k--;
        }
        // redundant check to see if all points were bad
        if (k == -1) {
            return;
        } else {
            // set all the bad values to the last good value
            for (int j=(this.size()-1); j> k; j--) {
                Point tempPoint = this.get(j);
                tempPoint.value = this.get(k).value;
                tempPoint.flag = this.get(k).flag;
                this.set(j, tempPoint);
            }
        }
        for(int n=(i+1); n<(k+1); n++) {
            Point p = this.get(n);
            //if has a good flag
            if (p.flag) {
                // if time bad is less than 5 minutes
                if (numBad > 0 && timeBad < 300000) {
                    // for the last bad points
                    Point lastGoodPoint = this.get(n-numBad-1);
                    for (int j=(n-numBad); j < n; j++) {
                        Point tempPoint = this.get(j);
                        // if the first n points were not bad, then go back to the last good point and interpolate
                        // time difference / total time difference * delta value + initial value
                        tempPoint.value = ((double)(this.get(j).timestamp.getTime() - lastGoodPoint.timestamp.getTime())) /
                                ((double)(this.get(n).timestamp.getTime() - lastGoodPoint.timestamp.getTime())) *
                                (this.get(n).value - lastGoodPoint.value) + lastGoodPoint.value;
                        tempPoint.flag = true;
                        this.set(j, tempPoint);
                    }
                }
                numBad = 0;
                timeBad = 0;
                firstBad = null;
            } else {
                numBad++;
                if (firstBad == null) {
                    firstBad = this.get(n).timestamp;
                } else {
                    timeBad = timeBad + this.get(n).timestamp.getTime() - firstBad.getTime();
                }
            }
        }
        correctZeroSingularities();
    }

    public void correctZeroSingularities() {
        // we also need to check for random zeroes
        // if the sensor is not a logic sensor(maxValue > 1), then
        // we need to find when values are 0 and correct them
        if (this.getMaxValue().value <= 1) {
            // if the data are logic values then no value is above 1
            return;
        }
        // start going through points if nonzero point and then a bunch of zero points
        // examine first n points. If value is 0, the value jumps greater than threshold, then first n points
        // should be corrected
        // find first nonzero point
        int i=0;
        while (i < this.size() && this.get(i).value == 0) {
            i++;
        }
        if (i == this.size()) {
            // all zeroes
            return;
        } else if ((this.get(i).timestamp.getTime() - this.get(0).timestamp.getTime()) < 300000) {
            for (int j=0; j < i; j ++) {
                Point tempPoint = this.get(j);
                tempPoint.value = this.get(i).value;
                this.set(j, tempPoint);
            }
        }
        // examine last n points. If value is 0 and the value jumps, then last n points should be corrected.
        int k = this.size() - 1;
        while(k > -1 && this.get(k).value == 0) {
            k--;
        }
        if (k == -1) {
            // all seroes reading from right
            return;
        }else if ((this.get(this.size()-1).timestamp.getTime() - this.get(k).timestamp.getTime()) < 300000) {
            for (int j=k+1; j < this.size(); j++) {
                Point tempPoint = this.get(j);
                tempPoint.value = this.get(k).value;
                this.set(j, tempPoint);
            }
        }
        // iterate through the dataset. When value goes to zero, record how long the value stays there
        int numZeros = 0;
        double gradientIn = 0;
        Point firstNonZero = this.get(i);
        for (int n=(i+1); n<(k+1); n++) {
            // if value is zero and firstzero is null, then set first zero, else set last zero
            // if the value is not zero, then
            if (this.get(n).value != 0) {
                // if zeros found and duration under 5 minutes, then correct 0's
                if (numZeros > 0 && (this.get(n).timestamp.getTime() - firstNonZero.timestamp.getTime()) < 300000) {
                    double gradientOut = this.get(n).value - this.get(n-1).value;
                    if (Math.abs((gradientIn - gradientOut)/gradientIn) < .1) {
                        for (int j=(n-numZeros); j < n; j++) {
                            Point tempPoint = this.get(j);
                            // if the first n points were not bad, then go back to the last good point and interpolate
                            // time difference / total time difference * delta value + initial value
                            tempPoint.value = ((double)(this.get(j).timestamp.getTime() - firstNonZero.timestamp.getTime())) /
                                    ((double)(this.get(n).timestamp.getTime() - firstNonZero.timestamp.getTime())) *
                                    (this.get(n).value - firstNonZero.value) + firstNonZero.value;
                            this.set(j, tempPoint);
                        }
                    }
                }
                firstNonZero = this.get(n);
                numZeros = 0;
                gradientIn = 0;
            } else{
                if (gradientIn == 0) {
                    gradientIn = this.get(n-1).value - this.get(n).value;
                }
                numZeros ++;
            }
        }
    }

    public PointList subset(int start, int end) {
        int correctedStart = start;
        int correctedEnd = end;
        if (start < 0) {
            correctedStart= 0;
        }
        if (start > this.size()-1) {
            correctedStart = this.size()-1;
        }
        if (end < 0) {
            correctedEnd = 0;
        }
        if (end >this.size()-1) {
            correctedEnd = this.size()-1;
        }
        if (correctedStart < correctedEnd) {
            return (new PointList(this.subList(correctedStart, correctedEnd)));
        } else {
            return (new PointList(this.subList(correctedEnd, correctedStart)));
        }
    }


    public PointList subset(Date startDate, Date endDate) {
        //output list
        PointList output = new PointList();
        int firstIndex = getNearestIndex(startDate);
        int lastIndex = getNearestIndex(endDate);
        if (firstIndex > lastIndex) {
            int temp = firstIndex;
            firstIndex = lastIndex;
            lastIndex = temp;
        }
        Point firstPoint = this.get(firstIndex);
        Point lastPoint = this.get(lastIndex);
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

    public PointList getRunningAverage(int n) {
        PointList averages = new PointList();
        LinkedList<Double> bufferValues = new LinkedList<Double>();
        double sum = 0;
        for(int i=0; i<this.size(); i++) {
            double value = this.get(i).value;
            sum += value;
            if (i<n) {
                bufferValues.add(value);
                double tempValue = sum/(double)(i+1);
                averages.add(new Point(this.get(i).name, this.get(i).timestamp, tempValue, this.get(i).flag));
            } else {
                sum -= bufferValues.removeFirst();
                bufferValues.add(value);
                double tempValue = sum/(double)n;
                averages.add(new Point(this.get(i).name, this.get(i).timestamp, tempValue, this.get(i).flag));
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

    public Point getMaxValue(){
        double maxVal = -Double.MAX_VALUE;
        int index = 0;
        for(int i=0; i<this.size(); i++){
            if(this.get(i).value > maxVal) {
                index = i;
                maxVal = this.get(i).value;
            }
        }
        return this.get(index);
    }

    public Point getMinValue(){
        double minVal = Double.MAX_VALUE;
        int index = 0;
        for(int i=0; i<this.size(); i++){
            if(this.get(i).value < minVal) {
                index = i;
                minVal = this.get(i).value;
            }
        }
        return this.get(index);
    }



    public int guessIndex(Date date, Date firstDate, Date lastDate, double numPoints) {
        return (int)Math.round((double)(date.getTime()-firstDate.getTime())/(double)(lastDate.getTime()-firstDate.getTime()) * (numPoints));
    }

    public ArrayList<Window> getSegmentsBetween(double d1, double d2, double tolerance, String name) {
        if (this.size() < 1) {
            return new ArrayList<Window>();
        }
        ArrayList<Window> segments = new ArrayList<Window>();
        ArrayList<HashMap<String, Integer>> goodSegments = new ArrayList<HashMap<String, Integer>>();
        ArrayList<HashMap<String, Integer>> badSegments = new ArrayList<HashMap<String, Integer>>();
        if (this.size()<=0) {
            return segments;
        }
        int i=0;
        int j=1;
        boolean prevBetween = this.get(i).value <= (d2+tolerance) && this.get(i).value >= (d1-tolerance);
        while (j<this.size()) {
            boolean thisBetween = this.get(j).value <= (d2+tolerance) && this.get(j).value >= (d1-tolerance);
            if (prevBetween && !thisBetween) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j-1);
                goodSegments.add(tempIndices);
                i = j;
            } else if(!prevBetween && thisBetween) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j-1);
                badSegments.add(tempIndices);
                i = j;
            }
            prevBetween = thisBetween;
            j++;
        }
        if (i!=j) {
            HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
            tempIndices.put("start", i);
            tempIndices.put("end", j-1);
            if (this.get(i).value <= (d2+tolerance) && this.get(i).value >= (d1-tolerance)) {
                goodSegments.add(tempIndices);
            } else {
                badSegments.add(tempIndices);
            }
        }
        for(HashMap<String, Integer> seg: goodSegments){
            segments.add(new Window(this.get(seg.get("start")).timestamp, this.get(seg.get("end")).timestamp, name));
        }
        return segments;
    }

    public ArrayList<Window> getSegmentsAway(double d, double tolerance, String name) {
        if (this.size() < 1) {
            return new ArrayList<Window>();
        }
        ArrayList<Window> segments = new ArrayList<Window>();
        ArrayList<HashMap<String, Integer>> goodSegments = new ArrayList<HashMap<String, Integer>>();
        ArrayList<HashMap<String, Integer>> badSegments = new ArrayList<HashMap<String, Integer>>();
        int i=0;
        int j=1;
        boolean prevNear = Math.abs(this.get(i).value - d) <= tolerance;
        while (j<this.size()) {
            Point currentPoint = this.get(j);
            if (!prevNear && Math.abs(currentPoint.value- d) <= tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j-1);
                goodSegments.add(tempIndices);
                i = j;
            } else if(prevNear && Math.abs(currentPoint.value - d) > tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j-1);
                badSegments.add(tempIndices);
                i = j;
            }
            j++;
            prevNear = Math.abs(currentPoint.value - d) <= tolerance;
        }
        if (i!=j) {
            HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
            tempIndices.put("start", i);
            tempIndices.put("end", j-1);
            if (Math.abs(this.get(i).value - d) > tolerance) {
                goodSegments.add(tempIndices);
            } else {
                badSegments.add(tempIndices);
            }
        }
        for(HashMap<String, Integer> seg: goodSegments){
            segments.add(new Window(this.get(seg.get("start")).timestamp, this.get(seg.get("end")).timestamp, name));
        }
        return segments;
    }

    public ArrayList<Window> getSegmentsNear(double d, double tolerance, String name) {
        if (this.size() < 1) {
            return new ArrayList<Window>();
        }
        ArrayList<Window> segments = new ArrayList<Window>();
        ArrayList<HashMap<String, Integer>> goodSegments = new ArrayList<HashMap<String, Integer>>();
        ArrayList<HashMap<String, Integer>> badSegments = new ArrayList<HashMap<String, Integer>>();
        int i=0;
        int j=1;
        boolean prevNear = Math.abs(this.get(i).value - d) <= tolerance;
        while (j<this.size()) {
            Point currentPoint = this.get(j);
            if (prevNear && Math.abs(currentPoint.value- d) > tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j-1);
                goodSegments.add(tempIndices);
                i = j;
            } else if(!prevNear && Math.abs(currentPoint.value - d) <= tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j-1);
                badSegments.add(tempIndices);
                i = j;
            }
            j++;
            prevNear = Math.abs(currentPoint.value - d) <= tolerance;
        }
        if (i!=j) {
            HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
            tempIndices.put("start", i);
            tempIndices.put("end", j-1);
            if (Math.abs(this.get(i).value - d) <= tolerance) {
                goodSegments.add(tempIndices);
            } else {
                badSegments.add(tempIndices);
            }
        }
        for(HashMap<String, Integer> seg: goodSegments){
            segments.add(new Window(this.get(seg.get("start")).timestamp, this.get(seg.get("end")).timestamp, name));
        }
        return segments;
    }

    public ArrayList<Window> getSegmentsGreater(double d, double tolerance, String name) {
        if (this.size() < 1) {
            return new ArrayList<Window>();
        }
        ArrayList<Window> segments = new ArrayList<Window>();
        ArrayList<HashMap<String, Integer>> goodSegments = new ArrayList<HashMap<String, Integer>>();
        ArrayList<HashMap<String, Integer>> badSegments = new ArrayList<HashMap<String, Integer>>();
        int i=0;
        int j=1;
        boolean prevLess = this.get(i).value <= d-tolerance;
        while (j<this.size()) {
            Point currentPoint = this.get(j);
            if (!prevLess && currentPoint.value <= d-tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j-1);
                goodSegments.add(tempIndices);
                i = j;
            } else if(prevLess && currentPoint.value > d-tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j-1);
                badSegments.add(tempIndices);
                i = j;
            }
            j++;
            prevLess = currentPoint.value <= d-tolerance;
        }
        if (i!=j) {
            HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
            tempIndices.put("start", i);
            tempIndices.put("end", j-1);
            if (this.get(i).value > d-tolerance) {
                goodSegments.add(tempIndices);
            } else {
                badSegments.add(tempIndices);
            }
        }
        for(HashMap<String, Integer> seg: goodSegments){
            segments.add(new Window(this.get(seg.get("start")).timestamp, this.get(seg.get("end")).timestamp, name));
        }
        return segments;
    }

    public ArrayList<Window> getSegmentsLess(double d, double tolerance, String name) {
        if (this.size() < 1) {
            return new ArrayList<Window>();
        }
        ArrayList<Window> segments = new ArrayList<Window>();
        ArrayList<HashMap<String, Integer>> goodSegments = new ArrayList<HashMap<String, Integer>>();
        ArrayList<HashMap<String, Integer>> badSegments = new ArrayList<HashMap<String, Integer>>();
        int i=0;
        int j=1;
        boolean prevGreater = this.get(i).value >= d+tolerance;
        while (j<this.size()) {
            Point currentPoint = this.get(j);
            if (!prevGreater && currentPoint.value >= d+tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j-1);
                goodSegments.add(tempIndices);
                i = j;
            } else if(prevGreater && currentPoint.value < d+tolerance) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j-1);
                badSegments.add(tempIndices);
                i = j;
            }
            j++;
            prevGreater = currentPoint.value >= d+tolerance;
        }
        if (i!=j) {
            HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
            tempIndices.put("start", i);
            tempIndices.put("end", j-1);
            if (this.get(i).value < d+tolerance) {
                goodSegments.add(tempIndices);
            } else {
                badSegments.add(tempIndices);
            }
        }
        for(HashMap<String, Integer> seg: goodSegments){
            segments.add(new Window(this.get(seg.get("start")).timestamp, this.get(seg.get("end")).timestamp, name));
        }
        return segments;
    }

    //segment data on intervals that are good and bad
    public ArrayList<Window> getSegments() {
        if (this.size() < 1) {
            return new ArrayList<Window>();
        }
        ArrayList<Window> segments = new ArrayList<Window>();
        ArrayList<HashMap<String, Integer>> goodSegments = new ArrayList<HashMap<String, Integer>>();
        ArrayList<HashMap<String, Integer>> badSegments = new ArrayList<HashMap<String, Integer>> ();
        int i=0;
        int j=1;
        boolean prevFlag = this.get(i).flag;
        while (j<this.size()) {
            Point currentPoint = this.get(j);
            if (prevFlag && !currentPoint.flag) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j-1);
                goodSegments.add(tempIndices);
                i = j;
            } else if (!prevFlag && currentPoint.flag) {
                HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
                tempIndices.put("start", i);
                tempIndices.put("end", j-1);
                badSegments.add(tempIndices);
                i = j;
            }
            j++;
            prevFlag = currentPoint.flag;
        }
        if (i!=j) {
            HashMap<String, Integer> tempIndices = new HashMap<String, Integer>();
            tempIndices.put("start", i);
            tempIndices.put("end", j-1);
            if (this.get(i).flag) {
                goodSegments.add(tempIndices);
            } else {
                badSegments.add(tempIndices);
            }
        }
        for(HashMap<String, Integer> seg: goodSegments){
            segments.add(new Window(this.get(seg.get("start")).timestamp, this.get(seg.get("end")).timestamp));
        }
        return segments;
    }

    public double getLeftDerivative(int index) {
        return 1000*(this.get(index).value - this.get(index-1).value)/
                ((double)(this.get(index).timestamp.getTime()- this.get(index-1).timestamp.getTime()));
    }

    public double getRightDerivative(int index) {
        return 1000*(this.get(index+1).value - this.get(index).value)/
                ((double)(this.get(index+1).timestamp.getTime()- this.get(index).timestamp.getTime()));
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

    public PointList getLeftDifferences() {
        PointList points = new PointList();
        for (int i=1; i<this.size(); i++) {
            double diff = this.get(i).value - this.get(i-1).value;
            Point tempPoint = new Point(this.get(i));
            tempPoint.name = tempPoint.name + "_left_diff";
            tempPoint.value = diff;
            points.add(tempPoint);
        }
        return points;
    }

    public PointList getRightDifferences() {
        PointList points = new PointList();
        for (int i=0; i<this.size() -1; i++) {
            double diff = this.get(i+1).value - this.get(i).value;
            Point tempPoint = new Point(this.get(i));
            tempPoint.name = tempPoint.name + "_righgt_diff";
            tempPoint.value = diff;
            points.add(tempPoint);
        }
        return points;
    }

    public PointList getLeftDerivatives() {
        PointList points = new PointList();
        for (int i=1; i<this.size(); i++) {
            double deriv = getLeftDerivative(i);
            Point tempPoint = new Point(this.get(i));
            tempPoint.name = tempPoint.name + "_left_deriv";
            tempPoint.value = deriv;
            points.add(tempPoint);
        }
        return points;
    }

    public PointList getRightDerivatives() {
        PointList points = new PointList();
        for (int i=0; i<this.size()-1; i++) {
            double deriv = getRightDerivative(i);
            Point tempPoint = new Point(this.get(i));
            tempPoint.name = tempPoint.name + "_right_deriv";
            tempPoint.value = deriv;
            points.add(tempPoint);
        }
        return points;
    }

    public PointList interpolate(int milliseconds) {
        if (this.size() < 2) {
            return new PointList(this);
        }
        long interval = (long)milliseconds;
        // mod divide interval
        long start = this.get(0).timestamp.getTime() - (this.get(0).timestamp.getTime() % interval);
        long end = this.get(this.size()-1).timestamp.getTime() + interval - (this.get(this.size()-1).timestamp.getTime() % interval);
        PointList temp = new PointList();
        temp.add(new Point(this.get(0).name, new Date(start), this.get(0).value, this.get(0).flag));
        for(long t=start+interval; t< end; t+=interval) {
            temp.add(this.get(new Date(t)));
        }
        temp.add(new Point(this.get(this.size()-1).name, new Date(end), this.get(this.size()-1).value, this.get(this.size()-1).flag));
        return temp;
    }

    public PointList getDerivatives() {
        PointList points = new PointList();
        for (int i=0; i<this.size(); i++) {
            double deriv = getDerivative(i);
            Point tempPoint = new Point(this.get(i));
            tempPoint.name = tempPoint.name + "_deriv";
            tempPoint.value = deriv;
            points.add(tempPoint);
        }
        return points;
    }

    public PointList merge(PointList d2) {
        PointList copy = new PointList(this);
        copy.addAll(d2);
        Collections.sort(copy);
        PointList average_copy = new PointList();
        Point prevPoint = copy.get(0);
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

    public void removeDuplicates() {
        ArrayList<Integer> indicesToRemove = new ArrayList<Integer>();
        for (int i=1; i < this.size(); i++) {
            if (this.get(i).timestamp.getTime() == this.get(i-1).timestamp.getTime()) {
                indicesToRemove.add(i);
            }
        }
        for (int i=indicesToRemove.size()-1; i >= 0; i--) {
            this.remove(indicesToRemove.get(i).intValue());
        }
    }
}
