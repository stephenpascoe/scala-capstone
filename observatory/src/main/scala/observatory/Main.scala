package observatory

import scala.io.Source

object Main extends App {
  val FIRST_YEAR = 1975
  val LAST_YEAR = 2015

  println("Loading stations")
  val stations = Extraction.parseStationsFile("/stations.csv")
  println("Loading done")
  println(stations.size)
  val temps = (FIRST_YEAR until LAST_YEAR) map {(year) => {
    println(s"Loading Year $year")
    (year, Extraction.parseTempFile(s"/$year.csv"))
  }}

  println(temps.length)
}
