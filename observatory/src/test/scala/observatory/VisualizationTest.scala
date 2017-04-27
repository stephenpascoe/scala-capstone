package observatory


import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalacheck.Gen

@RunWith(classOf[JUnitRunner])
class VisualizationTest extends FunSuite with GeneratorDrivenPropertyChecks {

  val london = Location(51.508530, -0.076132)
  val paris = Location(48.864716, 2.349014)
  val newyork = Location(40.730610, -73.935242)

  val locationGen = for {
    lat <- Gen.choose(-90.0, 90.0)
    lon <- Gen.choose(-180.0, 180.0)
  } yield Location(lat, lon)

  val sampleGen = {
    val gen = for {
      loc <- locationGen
      value <- Gen.choose(-50.0, 50.0)
    } yield (loc, value)

    Gen.listOfN(10, gen)
  }

  test("dist london->paris") {
    assert(Visualization.dist(london, paris) - 344.0 < 0.2)
    assert(Visualization.dist(london, paris) === Visualization.dist(paris, london))
  }

  test("dist swap inputs") {
    // TODO : Check how this should be done.  Should we assert?
    forAll (locationGen, locationGen) { (loc1: Location, loc2: Location) =>
      assert(Visualization.dist(loc1, loc2) === Visualization.dist(loc2, loc1))
    }
  }

  test("dist london->new york") {
    assert(Visualization.dist(london, newyork) - 5585.0 < 0.2)
  }

  test("Symetric idw") {
    val sample: List[(Location, Double)] =
      List((Location(30, 0), 5.0), (Location(0, 30), 5.0), (Location(-30, 0), 5.0), (Location(0, -30), 5.0))
    val loc = Location(0, 0)

    assert(Visualization.idw(sample, loc, Visualization.P) === 5.0)
  }

  test("At point idw") {
    val sample: List[(Location, Double)] =
      List((Location(30, 0), 10.0), (Location(0, 30), 5.0), (Location(-30, 0), 15.0), (Location(0, -30), 20.0))
    val loc = Location(-30, 0)

    assert(Visualization.idw(sample, loc, Visualization.P) === 15.0)
  }

  test("idw random samples") {
    forAll (sampleGen, locationGen) { (sample: List[(Location, Double)], loc: Location) => {
      val result = Visualization.idw(sample, loc, Visualization.P)
      assert(result <= 50.0)
      assert(result >= -50.0)
    }

    }
  }

}
