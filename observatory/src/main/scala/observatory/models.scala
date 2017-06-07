package observatory

import scala.io.Source

case class Location(lat: Double, lon: Double)

case class Color(red: Int, green: Int, blue: Int)

case class StationKey(stn: Option[Int], wban: Option[Int])

case class TempsLine(key: StationKey, month: Int, day: Int, temp: Double)

object DataSource {
  type Lookup = ((String) => Source)

  /* Lookup data source from class resources */
  val resourceFileLookup: Lookup = (path: String) => Source.fromInputStream(getClass.getResourceAsStream(path))
}