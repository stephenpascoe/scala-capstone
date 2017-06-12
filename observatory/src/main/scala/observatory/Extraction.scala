package observatory

import java.time.LocalDate

import scala.io.Source
import scala.util.{Failure, Success, Try}


/**
  * The course grade will load input files from the classpath.  This object implements a suitable
  * DataExtractor.
  */
object DataSource {
  type Lookup = ((String) => Source)

  /* Lookup data source from class resources */
  val resourceFileLookup: Lookup = (path: String) => Source.fromInputStream(getClass.getResourceAsStream(path))
}

/**
  * 1st milestone: data extraction
  */
object Extraction extends DataExtractor(DataSource.resourceFileLookup)


/**
  * Data extraction routines with an abstract interface to input streams
  *
  * @param dataSource: A function for getting an IO Source from a relative path.
  */
class DataExtractor(dataSource: DataSource.Lookup) extends Serializable {

  /**
    * @param year             Year number
    * @param stationsFile     Path of the stations resource file to use (e.g. "/stations.csv")
    * @param temperaturesFile Path of the temperatures resource file to use (e.g. "/1975.csv")
    * @return A sequence containing triplets (date, location, temperature)
    */
  def locateTemperatures(year: Int,
                         stationsFile: String,
                         temperaturesFile: String): Iterable[(LocalDate, Location, Double)] = {
    val stationsMap = parseStationsFile(stationsFile)

    parseTempFile(temperaturesFile).map(toLocatedTemperature(year, stationsMap)).flatten
  }

  def toLocatedTemperature(year: Int,
                           stationsMap: Map[StationKey, Location]
                          )(rec: TempsLine): Option[(LocalDate, Location, Double)] = {
    def toCelcius(f: Double): Double = (f - 32.0) * (5.0/9.0)

    Try((LocalDate.of(year, rec.month, rec.day), stationsMap(rec.key), toCelcius(rec.temp))).toOption
  }

  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Iterable[(LocalDate, Location, Double)]): Iterable[(Location, Double)] = {
    case class Acc(count: Int, total: Double)

    val totalsMap = records.foldLeft[Map[Location, Acc]](Map.empty) { (acc, rec) =>
      rec match {
        case (date, location, temp) => {
          val accLoc = acc.getOrElse(location, Acc(0, 0.0))
          acc.updated(location, Acc(accLoc.count + 1, accLoc.total + temp))
        }
      }
    }
    totalsMap.mapValues(acc => acc.total / acc.count).toIterable
  }


  /**
    * Any Non-numeric input results in the key's component  being None
    *
    * @param stnStr   STN number as string or the empty string
    * @param wbanStr  WBAN number as string or the empty string
    * @return         StationKey
    */
  def parseStationKey(stnStr: String, wbanStr: String) = StationKey(Try(stnStr.toInt).toOption,
    Try(wbanStr.toInt).toOption)

  /**
    * Parse a line from the stations file
    *
    * @param str      String of STN,WBAN,LAT,LON
    * @return         Parsed line
    */
  def parseStationsLine(str: String): Option[(StationKey, Location)] = {
    str.split(",") match {
      case Array(stn, wban, lat, lon) => Some((parseStationKey(stn, wban), Location(lat.toDouble, lon.toDouble)))
      case _ => None
    }
  }

  /**
    * Parse a line from a temperatures file
    * @param str      String of STN,WBAN,MONTH,DAY,TEMP
    * @return         Parsed line
    */
  def parseTempsLine(str: String): Option[TempsLine] = {
    val tryRecord = str.split(",") match {
      case Array(stn, wban, month, day, temp) => {
        val skey = parseStationKey(stn, wban)
        Try(TempsLine(skey, month.toInt, day.toInt, temp.toDouble))
      }
      case _ => Failure(new RuntimeException("Parse failed"))
    }
    tryRecord.toOption
  }

  /**
    * Parsing whole files
    */

  def parseStationsFile(stationsFile: String): Map[StationKey, Location] = {
    val lineStream = dataSource(stationsFile).getLines

    lineStream.map(parseStationsLine).flatten.toMap
  }

  def parseTempFile(temperaturesFile: String) : Iterable[TempsLine] = {
    val lineStream = dataSource(temperaturesFile).getLines

    lineStream.map(parseTempsLine).flatten.toIterable
  }

}
