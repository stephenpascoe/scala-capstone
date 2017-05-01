package observatory

import scala.math.{acos, cos, pow, sin, Pi}
import com.sksamuel.scrimage.{Image, Pixel}

import scala.annotation.tailrec

/**
  * 2nd milestone: basic visualization
  */
object Visualization {

  val EARTH_RADIUS = 6371.0
  val P = 4.0
  val MIN_ARC_DISTANCE = 1.0
  val TO_RADIANS = Pi / 180.0

  /**
    * @param temperatures Known temperatures: pairs containing a location and the temperature at this location
    * @param location Location where to predict the temperature
    * @return The predicted temperature at `location`
    */
  def predictTemperature(temperatures: Iterable[(Location, Double)], location: Location): Double = {
    idw(temperatures, location, P)
  }

  def dist(x: Location, xi: Location): Double = {
    val deltaLambda = ((x.lon max xi.lon) - (x.lon min xi.lon)) * TO_RADIANS
    val sigma = acos(sin(x.lat*TO_RADIANS)*sin(xi.lat*TO_RADIANS) + cos(x.lat*TO_RADIANS)*cos(xi.lat*TO_RADIANS) * cos(deltaLambda))
    // Convert to arc distance in km
    EARTH_RADIUS * sigma
  }

  // TODO : generalise idw algorithm
  // def idw[A,B](sample: Iterable[(A, B)], x: A, p: Double): B = {
  def idw(sample: Iterable[(Location, Double)], x: Location, p: Double): Double = {

    @tailrec
    def recIdw(values: Iterator[(Location, Double)], sumVals: Double, sumWeights: Double): Double = {
      values.next match {
        case (xi, ui) => {
          val arc_distance = dist(x, xi)
          if (arc_distance < MIN_ARC_DISTANCE)
            ui
          else {
            val w = 1.0 / pow(arc_distance, p)
            if (values.hasNext) recIdw(values, sumVals + w*ui, sumWeights + w)
            else (sumVals + w*ui) / (sumWeights + w)
          }
        }
      }
    }

    recIdw(sample.toIterator, 0.0, 0.0)
  }

  /**
    * @param points Pairs containing a value and its associated color
    * @param value The value to interpolate
    * @return The color that corresponds to `value`, according to the color scale defined by `points`
    */
  def interpolateColor(points: Iterable[(Double, Color)], value: Double): Color = {
    // TODO : sort array
    val sortedPoints = points.toList.sortWith(_._1 < _._1).toArray
    interpolateColor(sortedPoints, value)
  }

  def interpolateColor(sortedPoints: Array[(Double, Color)], value: Double): Color = {

    for (i <- 0 until sortedPoints.length - 1) {
      (sortedPoints(i), sortedPoints(i + 1)) match {
        case ((v1, Color(r1, g1, b1)), (v2, Color(r2, g2, b2))) => {
          if (v1 > value) {
            return Color(r1, g1, b1)
          }
          else if (v2 > value) {
            val ratio = (value - v1) / (v2 - v1)
            return Color(
              math.round(r1 + (r2 - r1) * ratio).toInt,
              math.round(g1 + (g2 - g1) * ratio).toInt,
              math.round(b1 + (b2 - b1) * ratio).toInt
            )
          }
        }
      }
    }
    // Value is not within the colourmap.  Return maximum color
    sortedPoints(sortedPoints.length-1)._2
  }

  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @return A 360×180 image where each pixel shows the predicted temperature at its location
    */
  def visualize(temperatures: Iterable[(Location, Double)], colors: Iterable[(Double, Color)]): Image = {
    val colourMap = colors.toList.sortWith(_._1 < _._1).toArray

    def colorToPixel(c: Color): Pixel = {
      Pixel.apply(c.red, c.green, c.blue, 255)
    }

    val buffer = new Array[Pixel](360 * 180)

    for (y <- 0 until 180) {
      for (x <- 0 until 360) {
        val temp = idw(temperatures, Location(90-y, x-180), P)
        buffer(y*360 + x) = colorToPixel(interpolateColor(colourMap, temp))
      }
    }

    Image.apply(360, 180, buffer)
  }

}

