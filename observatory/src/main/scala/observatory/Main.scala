package observatory

import scala.io.Source

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import com.sksamuel.scrimage.{Image, Pixel}

import Manipulation._

/**
  * Generate Google-projection map tiles from point station data for the Coursera course
  * "Functional Programming in Scala Capstone" (https://www.coursera.org/learn/scala-capstone/home/welcome)
  *
  * This application runs on apache spark but uses a shared filesystem, rather than HDFS, to load resources.
  * This happens to match the infrastructure I had available.  The structure of the source is influenced by
  * the courses initial template and the requirement to maintain compatibility with the course grader which
  * expects certain simplified type signatures.
  *
  * usage: spark-submit ... observatory.Main <resource-dir>
  *
  *   resource-dir is a filesystem path accessible to all spark slaves under which the input CSV files are visible.
  *
  * output tiles are written to <resource-dir>/temperatures and <resource-dir>/deviations
  *
  */
object Main {
  def main(args: Array[String]): Unit = {
    /**
      * Setup resources and spark environment.
      */
    val resourceDir = args(0)

    val conf: SparkConf = new SparkConf().setAppName("Scala-Capstone")
    val sc: SparkContext = new SparkContext(conf)

    /**
      * For compatibility with the course grader we use an abstract loading function to load data from resourceDir.
     */
    val lookupResource: DataSource.Lookup = (path: String) => {
      Source.fromFile(s"${resourceDir}/${path}")
    }
    val sparkExtractor = new DataExtractor(lookupResource)

    /**
      * Load data into RDDs
      */
    val years: RDD[Int] = sc.parallelize(Config.FIRST_YEAR until Config.LAST_YEAR + 1, 32)
    val temps: RDD[(Int, Iterable[(Location, Double)])] = years.map( (year: Int) => {
      println(s"OBSERVATORY: Loading data for year ${year}")
      (year, sparkExtractor.locationYearlyAverageRecords(sparkExtractor.locateTemperatures(year, "/stations.csv", s"/${year}.csv")))
    })

    /**
      * Use spatial interpolation to calculate 1-degree resolution grids of the temperature field for each year.
      * Cache these grids for future calculations.
      */
    val grids: RDD[(Int, Grid)] = temps.map({
      case (year: Int, temps: Iterable[(Location, Double)]) => {
        println(s"OBSERVATORY: Generating grid for year ${year}")
        (year, Grid.fromIterable(temps))
      }
    }).cache

    /**
      * Calculate a normal (average) grid for the years 1975-1989.  This will be our baseline for devation calculations.
      * The result is broadcast to all nodes.
      */
    val normalGridVar = sc.broadcast(averageGridRDD(grids.filter(_._1 < Config.NORMALS_BEFORE).map(_._2)))

    /**
      * Calculate deviation grids for the years 1990-2015
      */
    val deviations: RDD[(Int, Grid)] = grids.filter({
      case (year: Int, g: Grid) => year >= Config.NORMALS_BEFORE
    }).map({
      case (year: Int, g: Grid) => (year, g.diff(normalGridVar.value))
    })

    /**
      * Calculate map tiles for the temperature grids.
      */
    makeTiles(deviations, Colors.deviations, s"${resourceDir}/deviations")

    /**
      * Calculate map tiles for the devation grids
      */
    makeTiles(grids, Colors.temperatures, s"${resourceDir}/temperatures")
  }
}

