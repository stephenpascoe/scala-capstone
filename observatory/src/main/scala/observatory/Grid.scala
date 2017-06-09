package observatory


/**
  * Created by spascoe on 07/06/2017.
  */
class Grid extends Serializable {
  val width = 360
  val height = 180
  val buffer: Array[Double] = new Array[Double](width * height)

  def xyToLocation(x: Int, y: Int): Location = Location((height / 2) - y, x - (width / 2))
  def asFunction(): (Int, Int) => Double = {
    (lat: Int, lon: Int) => {
      val x = lon + 180
      val y = 90 - lat
      buffer(y * width + x)
    }
  }

  // TODO : remove if possible
  def asArray(): Array[Double] = buffer

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

  def map(f: (Double) => Double): Grid = {
    val newGrid = new Grid()
    for (i <- 0 until width * height) { newGrid.buffer(i) = f(this.buffer(i)) }
    newGrid
  }

}

object Grid {
  def fromIterable(temperatures: Iterable[(Location, Double)]): Grid = {
    val grid = new Grid()

    for (y <- 0 until grid.height) {
      for (x <- 0 until grid.width) {
        grid.buffer(y * grid.width + x) = Visualization.idw(temperatures, grid.xyToLocation(x, y), Visualization.P)
      }
    }
    grid
  }

}