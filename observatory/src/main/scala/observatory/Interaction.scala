package observatory

import com.sksamuel.scrimage.{Image, Pixel}

import scala.math._
import Visualization.Visualizer

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * 3rd milestone: interactive visualization
  */
object Interaction {

  class TileVisualizer(zoom: Int, colors: Iterable[(Double, Color)], x: Int, y: Int) extends Visualizer {
    val alpha = 127
    val width = 256
    val height = 256
    val colorMap = colors.toList.sortWith(_._1 < _._1).toArray

    // Tile offset of this tile in the zoom+8 coordinate system
    val x0 = pow(2.0, zoom + (8 - 1)).toInt * x
    val y0 = pow(2.0, zoom + (8 - 1)).toInt * y

    def xyToLocation(x: Int, y: Int): Location = {
      tileLocation(zoom + 8, x0 + x, y0 + y)
    }
  }


  /**
    * @param zoom Zoom level
    * @param x X coordinate
    * @param y Y coordinate
    * @return The latitude and longitude of the top-left corner of the tile, as per http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
    */
  def tileLocation(zoom: Int, x: Int, y: Int): Location = {
    val n = pow(2.0, zoom)
    val lon = (x.toDouble / n) * 360.0 - 180.0
    val lat = atan(sinh(Pi * (1.0 - 2.0 * y / n))).toDegrees

    Location(lat, lon)
  }

  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @param zoom Zoom level
    * @param x X coordinate
    * @param y Y coordinate
    * @return A 256×256 image showing the contents of the tile defined by `x`, `y` and `zooms`
    */
  def tile(temperatures: Iterable[(Location, Double)], colors: Iterable[(Double, Color)], zoom: Int, x: Int, y: Int): Image = {
    val vis = new TileVisualizer(zoom, colors, x, y)

    vis.visualize(temperatures)
  }

  /**
    * Generates all the tiles for zoom levels 0 to 3 (included), for all the given years.
    * @param yearlyData Sequence of (year, data), where `data` is some data associated with
    *                   `year`. The type of `data` can be anything.
    * @param generateImage Function that generates an image given a year, a zoom level, the x and
    *                      y coordinates of the tile and the data to build the image from
    */
  def generateTiles[Data](
    yearlyData: Iterable[(Int, Data)],
    generateImage: (Int, Int, Int, Int, Data) => Unit
  ): Unit = {
    val tileTasks = for {
      (year, data) <- yearlyData
      zoom <- 0 until 4
      y <- 0 until pow(2.0, zoom).toInt
      x <- 0 until pow(2.0, zoom).toInt
    } yield Future { generateImage(year, zoom, x, y, data) }

    Await.result(Future.sequence(tileTasks), Duration.Inf)
    ()
  }
}
