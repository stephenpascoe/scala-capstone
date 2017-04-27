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

}
