package observatory

import scala.math.{sqrt}

case class Location(lat: Double, lon: Double)

case class Color(red: Int, green: Int, blue: Int)

case class StationKey(stn: Option[Int], wban: Option[Int])

case class TempsLine(key: StationKey, month: Int, day: Int, temp: Double)