package observatory

import scala.io.Source
import java.time.LocalDate
import java.io.File

import Extraction._

import scala.concurrent.Await

object Main extends App {
  val FIRST_YEAR = 1975
  val LAST_YEAR = 2015

  val colors: List[(Double, Color)] = List(
    (60.0, Color(255, 255, 255)),
    (32.0, Color(255, 0, 0)),
    (12.0, Color(255, 255, 0)),
    (0.0, Color(0, 255, 255)),
    (-15.0, Color(0, 0, 255)),
    (-27.0, Color(255, 0, 255)),
    (-50.0, Color(33, 0, 107)),
    (-60.0, Color(0, 0, 0))
  )

  def doWeek1(): Unit = {
    val temps: Map[Int, Iterable[(Location, Double)]] = {
      for {
        year <- FIRST_YEAR until LAST_YEAR
      } yield (year, locationYearlyAverageRecords(locateTemperatures(year, "/stations.csv", s"/$year.csv")))
    }.toMap
  }

  def doWeek2(): Unit = {
    val temps1975 = locationYearlyAverageRecords(locateTemperatures(1975, "/stations.csv", "/1975.csv"))
    val image = Visualization.visualize(temps1975, colors)
    image.output("./map_1975.png")

    ()
  }

  def doWeek3(): Unit = {
    val temps = locationYearlyAverageRecords(locateTemperatures(2015, "/stations.csv", "/1975.csv"))

    def generateTile(year: Int, zoom: Int, x: Int, y: Int, data: Iterable[(Location, Double)]): Unit = {
      val tileDir = new File(s"./target/temperatures/2015/$zoom")
      tileDir.mkdirs()
      val tileFile = new File(tileDir, s"$x-$y.png")

      if (tileFile.exists ){
        println(s"Tile $zoom:$x:y already exists")
      }
      else {
        println(s"Generating tile $zoom:$x:$y for $year")
        val tile = Interaction.tile(data, colors, zoom, x, y)
        println(s"Done tile $zoom:$x:$y for $year")
        tile.output(tileFile)
      }

      ()
    }

    Interaction.generateTiles(List((1975, temps)), generateTile)
  }

  doWeek3()
  
}

