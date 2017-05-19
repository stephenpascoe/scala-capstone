package observatory

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
    val gridPairs: Iterable[(Array[Double], Int)] = for {
      temps <- temperaturess
    } yield (new Grid(360, 180, temps).asArray(), 1)

    val reduced = gridPairs.reduce { (p1: (Array[Double], Int), p2: (Array[Double], Int)) => {
      (p1, p2) match {
        case ((g1, c1), (g2, c2)) => {
          val g3 = new Array[Double](g1.length)
          for (i <- 0 until g1.length) {
            g3(i) = g1(i) + g2(i)
          }
          (g3, c1 + c2)
        }
      }
    }}

    val meanGrid = reduced match {
      case (grid, count) => grid.map(_ / count)
    }

    new Grid(360, 180, meanGrid).asFunction()
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

