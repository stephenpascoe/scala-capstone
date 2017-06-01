package observatory

import scala.io.Source
import java.time.LocalDate
import java.io.File

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import Extraction._
import Manipulation._

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

  val anomalyColors: List[(Double, Color)] = List(
    (7.0, Color(0, 0, 0)),
    (4.0, Color(255, 0, 0)),
    (2.0, Color(255, 255, 0)),
    (0.0, Color(255, 255, 255)),
    (-2.0, Color(0, 255, 255)),
    (-7.0, Color(0, 0, 255))
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

  // Can be run on high CPU machine as follows:
  // ~/opt/jre/bin/java -Dscala.concurrent.context.numThreads=20 -Dscala.concurrent.context.maxThreads=8 -cp $jars:capstone-observatory_2.11-0.1-SNAPSHOT.jar observatory.Main
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


  // Note: The climatalogical term for what the course descrives as temperature deviations is "anomalies"
  def doWeek5(): Unit = {
    // Setup Spark environment
    val conf: SparkConf = new SparkConf().setAppName("Scala-Capstone")
    val sc: SparkContext = new SparkContext(conf)

    // Load data into RDDs
    val years: RDD[Int] = sc.parallelize(1975 until 2016)
    val temps: RDD[(Int, Iterable[(Location, Double)])] = years.map( (year: Int) => {
      (year, locationYearlyAverageRecords(locateTemperatures(year, "/stations.csv", s"/${year}.csv")))
    })
    val grids: RDD[(Int, Grid)] = temps.map({
      case (year: Int, temps: Iterable[(Location, Double)]) => (year, new Grid(360, 180, temps))
    })

    // Calculate normals from 1975-1989
    val normalGrid: Grid = averageGridRDD(grids.filter(_._1 < 1990).map(_._2))
    // TODO : Calculate anomalies for 1990-2015

    // TODO : Create tiles
  }

  doWeek5()
  
}

