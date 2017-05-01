package observatory

import scala.io.Source
import java.time.LocalDate

import Extraction._

object Main extends App {
  val FIRST_YEAR = 1975
  val LAST_YEAR = 2015

  val colours: List[(Double, Color)] = List(
    (60.0, Color(255, 255, 255)),
    (32.0, Color(255, 0, 0)),
    (12.0, Color(255, 255, 0)),
    (0.0, Color(0, 255, 255)),
    (-15.0, Color(0, 0, 255)),
    (-27.0, Color(255, 0, 255)),
    (-50.0, Color(33, 0, 107)),
    (-60.0, Color(0, 0, 0))
  )

  // println("Loading stations")
  // val stations = Extraction.parseStationsFile("/stations.csv")
  // println("Loading done")
  // println(stations.size)

  // TODO Convert temps and stations data to yearly data
  /*
  val temps: Map[Int, Iterable[(Location, Double)]] = {
    for {
      year <- FIRST_YEAR until LAST_YEAR
      println(s"Loading year $year")
    } yield (year, locationYearlyAverageRecords(locateTemperatures(year, "/stations.csv", s"/$year.csv")))
  }.toMap
  */

  val temps1975 = locationYearlyAverageRecords(locateTemperatures(1975, "/stations.csv", "/1975.csv"))
  val image = Visualization.visualize(temps1975, colours)
  image.output("./map_1975.png")
}
