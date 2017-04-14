package observatory

import java.time.LocalDate

import monix.execution.Scheduler.Implicits.global
import monix.reactive._

import scala.io.Source
import scala.util.{Try, Success, Failure}
// import scala.collection.mutable.HashMap

case class StationKey(stn: Option[Int], wban: Option[Int])
case class GeoPos(lat: Double, lon: Double)

/**
  * 1st milestone: data extraction
  */
object Extraction {

  /**
    * @param year             Year number
    * @param stationsFile     Path of the stations resource file to use (e.g. "/stations.csv")
    * @param temperaturesFile Path of the temperatures resource file to use (e.g. "/1975.csv")
    * @return A sequence containing triplets (date, location, temperature)
    */
  def locateTemperatures(year: Int, stationsFile: String, temperaturesFile: String): Iterable[(LocalDate, Location, Double)] = {
    ???
  }

  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Iterable[(LocalDate, Location, Double)]): Iterable[(Location, Double)] = {
    ???
  }

  def parseStationsStream(stationsFile: String): Map[StationKey, GeoPos] = {
    def optInt(a: String) = Try(a.toInt) match {
      case Success(v) => Some(v)
      case Failure(_) => None
    }

    val lineStream = Source.fromInputStream(getClass.getResourceAsStream(stationsFile)).getLines
    val optKvStream = lineStream.map((str: String) => str.split(",")).map {
      case Array(stn, wban, lat, lon) => Some(StationKey(optInt(stn), optInt(wban)) -> GeoPos(lat.toDouble, lon.toDouble))
      case _ => None
    }

    optKvStream.flatten.toMap

  }

}

