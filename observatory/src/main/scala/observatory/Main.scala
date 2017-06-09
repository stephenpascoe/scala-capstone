package observatory

import scala.io.Source
import java.time.LocalDate
import java.io.File
import scala.math.{pow}

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import com.sksamuel.scrimage.{Image, Pixel}

import Extraction._
import Manipulation._

import scala.concurrent.Await

object Main {
  def main(args: Array[String]): Unit = {
    // First argument is the resources directory
    val resourceDir = args(0)

    doWeek5(resourceDir)

  }

  def doWeek1(): Unit = {
    val temps: Map[Int, Iterable[(Location, Double)]] = {
      for {
        year <- Config.FIRST_YEAR until Config.LAST_YEAR
      } yield (year, locationYearlyAverageRecords(locateTemperatures(year, "/stations.csv", s"/$year.csv")))
    }.toMap
  }

  def doWeek2(): Unit = {
    val temps1975 = locationYearlyAverageRecords(locateTemperatures(1975, "/stations.csv", "/1975.csv"))
    val image = Visualization.visualize(temps1975, Colors.temperatures)
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
        val tile = Interaction.tile(data, Colors.temperatures, zoom, x, y)
        println(s"Done tile $zoom:$x:$y for $year")
        tile.output(tileFile)
      }

      ()
    }

    Interaction.generateTiles(List((1975, temps)), generateTile)
  }


  // Note: The climatalogical term for what the course describes as temperature deviations is "anomalies"
  def doWeek5(resourceDir: String): Unit = {
    // Setup Spark environment
    val conf: SparkConf = new SparkConf().setAppName("Scala-Capstone")
    val sc: SparkContext = new SparkContext(conf)


    val lookupResource: DataSource.Lookup = (path: String) => {
      Source.fromFile(s"${resourceDir}/${path}")
    }
    val sparkExtractor = new DataExtractor(lookupResource)

    // Load data into RDDs, caching the resulting grids
    val years: RDD[Int] = sc.parallelize(Config.FIRST_YEAR until Config.LAST_YEAR + 1, 32)
    val temps: RDD[(Int, Iterable[(Location, Double)])] = years.map( (year: Int) => {
      println(s"OBSERVATORY: Loading data for year ${year}")
      (year, sparkExtractor.locationYearlyAverageRecords(sparkExtractor.locateTemperatures(year, "/stations.csv", s"/${year}.csv")))
    })
    val grids: RDD[(Int, Grid)] = temps.map({
      case (year: Int, temps: Iterable[(Location, Double)]) => {
        println(s"OBSERVATORY: Generating grid for year ${year}")
        (year, Grid.fromIterable(temps))
      }
    }).cache

    // Calculate normals from 1975-1989
    // Broadcast result to all nodes
    val normalGridVar = sc.broadcast(averageGridRDD(grids.filter(_._1 < Config.NORMALS_BEFORE).map(_._2)))

    // Calculate anomalies for 1990-2015
    val anomalies: RDD[(Int, Grid)] = grids.filter({
      case (year: Int, g: Grid) => year >= Config.NORMALS_BEFORE
    }).map({
      case (year: Int, g: Grid) => (year, g.diff(normalGridVar.value))
    })

    // Create anomaly tiles
    makeTiles(anomalies, Colors.temperatures, s"${resourceDir}/deviations")

    // Create non-anomaly tiles
    makeTiles(grids, Colors.anomalies, s"${resourceDir}/temperatures")
  }


}

