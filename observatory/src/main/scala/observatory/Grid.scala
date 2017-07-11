package observatory


/**
  * Grid abstracts a 2-D array of doubles for a global grid at 1-degree resolution.
  * Methods are provided for point-wise arithmetic.  Only those methods required for the application are implemented.
  */
class Grid extends Serializable {
  val width = 360
  val height = 180
  val buffer: Array[Double] = new Array[Double](width * height)

  /**
    * Convert to a function between (lat, lon) and value.  This is required by the grader.
    */
  def asFunction(): (Int, Int) => Double = {
    (lat: Int, lon: Int) => {
      val x = lon + 180
      val y = 90 - lat
      buffer(y * width + x)
    }
  }

  // Add grids
  def add(grid: Grid): Grid = {
    val newGrid = new Grid()
    for (i <- 0 until width * height) { newGrid.buffer(i) = this.buffer(i) + grid.buffer(i)}
    newGrid
  }

  // diff grids
  def diff(grid: Grid): Grid = {
    val newGrid = new Grid()
    for (i <- 0 until width * height) { newGrid.buffer(i) = this.buffer(i) - grid.buffer(i)}
    newGrid
  }

  // scale grids
  def scale(factor: Double): Grid = {
    val newGrid = new Grid()
    for (i <- 0 until width * height) { newGrid.buffer(i) = this.buffer(i) * factor }
    newGrid
  }

  // map a function over all points
  def map(f: (Double) => Double): Grid = {
    val newGrid = new Grid()
    for (i <- 0 until width * height) { newGrid.buffer(i) = f(this.buffer(i)) }
    newGrid
  }

}

object Grid {
  /**
    * Generate a grid using spatial interpolation from an iterable of locations and values.
    */
  def fromIterable(temperatures: Iterable[(Location, Double)]): Grid = {
    val grid = new Grid()

    for (y <- 0 until grid.height) {
      for (x <- 0 until grid.width) {
        val loc = Location(y - (grid.height / 2), x - (grid.width / 2))
        grid.buffer(y * grid.width + x) = Visualization.idw(temperatures, loc, Visualization.P)
      }
    }
    grid
  }

}