package observatory

import scala.io.Source

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import com.sksamuel.scrimage.{Image, Pixel}

import Manipulation._


object Main {
  def main(args: Array[String]): Unit = {
    // First argument is the resources directory
    val resourceDir = args(0)

    // Note: The climatalogical term for what the course describes as temperature deviations is "anomalies"
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

