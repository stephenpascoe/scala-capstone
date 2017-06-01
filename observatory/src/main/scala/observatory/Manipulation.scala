package observatory

import org.apache.spark.rdd.RDD

/**
  * 4th milestone: value-added information
  */
object Manipulation {

  /**
    * @param temperatures Known temperatures
    * @return A function that, given a latitude in [-89, 90] and a longitude in [-180, 179],
    *         returns the predicted temperature at this location
    */
  def makeGrid(temperatures: Iterable[(Location, Double)]): (Int, Int) => Double = {
    val grid = new Grid(360, 180, temperatures)
    grid.asFunction()
  }

  /**
    * @param temperaturess Sequence of known temperatures over the years (each element of the collection
    *                      is a collection of pairs of location and temperature)
    * @return A function that, given a latitude and a longitude, returns the average temperature at this location
    */
  def average(temperaturess: Iterable[Iterable[(Location, Double)]]): (Int, Int) => Double = {
    // TODO : Average over all years
    // TODO : Parallelisation with reduce makes sense here or maybe Spark
    // Generate a grid for each year
    val gridPairs: Iterable[(Grid, Int)] = for {
      temps <- temperaturess
    } yield (new Grid(360, 180, temps), 1)

    val reduced = gridPairs.reduce(mergeArrayPairs)

    // TODO : Make grids mappable to avoid uggly casting
    val meanGrid: Grid = reduced match {
      case (grid, count) => new Grid(360, 180, grid.asArray().map(_ / count))
    }

    meanGrid.asFunction()
  }

  def mergeArrayPairs(p1: (Grid, Int), p2: (Grid, Int)): (Grid, Int) = {
    (p1, p2) match {
      case ((g1, c1), (g2, c2)) => {
        val a1 = g1.asArray()
        val a2 = g2.asArray()
        val a3 = new Array[Double](a1.length)
        for (i <- 0 until a1.length) {
          a3(i) = a1(i) + a2(i)
        }
        (new Grid(360, 180, a3), c1 + c2)
      }
    }
  }

  /**
    * @param temperatures Known temperatures
    * @param normals A grid containing the “normal” temperatures
    * @return A sequence of grids containing the deviations compared to the normal temperatures
    */
  def deviation(temperatures: Iterable[(Location, Double)], normals: (Int, Int) => Double): (Int, Int) => Double = {
    val grid = makeGrid(temperatures)
    (x: Int, y: Int) => {
      grid(x, y) - normals(x, y)
    }
  }

  /**
    * Spark implementation of the averaging function
    */
  def averageGridRDD(temperatures: RDD[Grid]): Grid = {
    val reduced: (Grid, Int) = temperatures.map((_, 1)).reduce(
      (p1: (Grid, Int), p2: (Grid, Int)) => mergeArrayPairs(p1, p2)
    )
    val meanArray: Array[Double] = reduced match {
      case (grid, count) => grid.asArray().map(_ / count)
    }
    new Grid(360, 180, meanArray)
  }


}

// TODO : This is very similar to Visualization.Visualizer.  We could generalise.
class Grid(width: Int, height: Int, buffer: Array[Double]) {

  def this(width: Int, height: Int, temperatures: Iterable[(Location, Double)]) {
    this(width, height, new Array[Double](width * height))

    for (y <- 0 until height) {
      for (x <- 0 until width) {
        buffer(y * width + x) = Visualization.idw(temperatures, xyToLocation(x, y), Visualization.P)
      }
    }
  }

  def xyToLocation(x: Int, y: Int): Location = Location((height / 2) - y, x - (width / 2))
  def asFunction(): (Int, Int) => Double = {
    (lat: Int, lon: Int) => {
      val x = lon + 180
      val y = 90 - lat
      buffer(y * width + x)
    }
  }

  def asArray(): Array[Double] = buffer


}

