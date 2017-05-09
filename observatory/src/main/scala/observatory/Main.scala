package observatory

import scala.io.Source
import java.time.LocalDate

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
  }

  def doWeek3(): Unit = {
    val temps1975 = locationYearlyAverageRecords(locateTemperatures(1975, "/stations.csv", "/1975.csv"))

    println("tile1")
    val tile1 = Interaction.tile(temps1975, colors, 1, 0, 0)
    tile1.output("./tile1.png")
    println("tile2")
    val tile2 = Interaction.tile(temps1975, colors, 1, 1, 0)
    tile2.output("./tile2.png")
    println("tile3")
    val tile3 = Interaction.tile(temps1975, colors, 1, 1, 1)
    tile3.output("./tile3.png")
    println("tile4")
    val tile4 = Interaction.tile(temps1975, colors, 1, 1, 1)
    tile4.output("./tile4.png")


  }

  doWeek3()
  
}

