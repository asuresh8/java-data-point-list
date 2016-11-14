# java-data-point-manipulation

 This package is used to manipulate a vector of data with varying time intervals. The typical use case for this type of
 data is in the iot data. Each iot device processes gigabytes of data every year. The storage capacity for all this data
 simply does not exist. In order to compress this data, the data is only stored when there is a change in the signal. For
 example, while an Amazon echo is off, we would not store a 0 for every millisecond the device is off and a 1 for every
 millisecond the echo is off. Instead we record 0 for the first millisecond the device is off and the last millisecond
 the device is off - likewise for when the device is on.

 The major classes involved in this implementation are DataPoint, DataPointList, and DateWindow

 DataPoint has fields for the name of the data point collected, the timestamp, the value, and a boolean flag for if the
 data is reliable.

 DataPointList is an ArrayList<DataPoint>. Included in data point list are several functions, such as interpolation to
 data at consistent time intervals, derivatives, which where manipulations to subset the data

 DateWindow is a windows between two data ranges. A window consists of a start, an end, and an identifier.


