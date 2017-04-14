# Functional Programming in Scala Capstone project

https://www.coursera.org/learn/scala-capstone/home/welcome

## Milestone 1: Data extraction

### Architecture

The source data is a small csv file (<1Mb) mapping station identifiers to locations and a series of medium-sized csv files (one for each year, <100Mb) mapping station identifiers to dates and temperatures.  The grader requires we implement a function to parse each combination of year file and the stations file.  We will need to join this data together to produce a set of temperatures by location and date.

We could use Spark to join this data.  However, the data is fairly small and the grader interface requires each date file be processed separately.  We could cache the stations dataset as an RDD but it is only 712K so could easily be read into memory.  Each year file will also fit comfortably in memory when processed sequentially.  Therefore the Spark overhead would not be worth it.

The project suggests other libraries we could use: Akka Streams, Monix and fs2.  Both Akka Streams and Monix are implementations of [Reactive Streams](http://reactive-streams.org/).  This milestone is not a naturally reactive task (since lines will be read from files in a predictable manner) but they could be useful as a way to structure parallelism.  Fs2 looks like a good way to read the CSV files.  Both fs2 and Monix have interfaces to the pure-functional libraries Scalaz and Catz.

For this milestone I will start with Fs2 and pure Scala data structures.  Depending on how this performs I will consider parallalism which might use some of these other libraries.
