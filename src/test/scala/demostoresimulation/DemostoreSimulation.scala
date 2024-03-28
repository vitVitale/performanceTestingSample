package demostoresimulation

import demostoresimulation.pageobjects._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

class DemostoreSimulation extends Simulation {

  private val domain = "demostore.gatling.io"

  private val httpProtocol = http
    .baseUrl("https://" + domain)

  def userCount: Int = getProperty("USERS", "5").toInt
  def rampDuration: Int = getProperty("RAMP_DURATION", "10").toInt
  def testDuration: Int = getProperty("DURATION", "60").toInt

  private def getProperty(propertyName: String, defaultValue: String): String = {
    Option(System.getenv(propertyName))
      .orElse(Option(System.getProperty(propertyName)))
      .getOrElse(defaultValue)
  }

  val rnd = new Random()

  def randomString(length: Int): String = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }

  before {
    println(s"Running test with ${userCount} users")
    println(s"Ramping users over ${rampDuration} seconds")
    println(s"Total test duration ${testDuration} seconds")
  }

  after {
    println("Stress testing complete!")
  }

  val initSession = exec(flushCookieJar)
    .exec(session => session.set("randomNumber", rnd.nextInt))
    .exec(session => session.set("customerLoggedIn", false))
    .exec(session => session.set("cartTotal", 0.00))
    .exec(addCookie(Cookie("sessionId", randomString(10)).withDomain(domain)))
//    .exec { session => println(session); session }

  private val scn = scenario("DemostoreSimulation")
    .exec(initSession)
    .exec(
      CmsPages.homePage,
      pause(2),
      CmsPages.aboutUs,
      pause(2),
      Catalog.Category.view,
      pause(2),
      Catalog.Product.add,
      pause(2),
      Checkout.viewCart,
      pause(2),
//      Customer.login
//      pause(2),
      Checkout.completeCheckout
    )

  object UserJourneys {
    def minPause = 100.milliseconds
    def maxPause = 500.milliseconds

    def browseStore = {
      exec(initSession)
        .exec(CmsPages.homePage)
        .pause(maxPause)
        .exec(CmsPages.aboutUs)
        .pause(minPause, maxPause)
        .repeat(5) {
          exec(Catalog.Category.view)
            .pause(minPause, maxPause)
            .exec(Catalog.Product.view)
        }
    }

    def abandonCart = {
      exec(initSession)
        .exec(CmsPages.homePage)
        .pause(maxPause)
        .exec(Catalog.Category.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.add)
    }

    def completePurchase = {
      exec(initSession)
        .exec(CmsPages.homePage)
        .pause(maxPause)
        .exec(Catalog.Category.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.add)
        .pause(minPause, maxPause)
        .exec(Checkout.viewCart)
        .pause(minPause, maxPause)
        .exec(Checkout.completeCheckout)
    }
  }

  object Scenarios {
    def default = scenario("Default Load Test")
      .during(testDuration.seconds) {
        randomSwitch(
          75d -> exec(UserJourneys.browseStore),
          15d -> exec(UserJourneys.abandonCart),
          10d -> exec(UserJourneys.completePurchase)
        )
    }

    def highPurchase = scenario("High Purchase Load Test")
      .during(testDuration.seconds) {
        randomSwitch(
          25d -> exec(UserJourneys.browseStore),
          25d -> exec(UserJourneys.abandonCart),
          50d -> exec(UserJourneys.completePurchase)
        )
    }
  }

//  Run in terminal command:    mvn gatling:test -DUSERS=10 -DRAMP_DURATION=20 -DDURATION=30
//  setUp(Scenarios.default
//    .inject(rampUsers(userCount) during (rampDuration.seconds))
//    .protocols(httpProtocol))

//  Sequental run scenarios
//  setUp(
//    Scenarios.default
//      .inject(rampUsers(userCount) during (rampDuration.seconds)).protocols(httpProtocol)
//      .andThen(
//        Scenarios.highPurchase
//          .inject(rampUsers(5) during (10.seconds)).protocols(httpProtocol))
//  )

//  Parallel run scenarios
  setUp(
    Scenarios.default
      .inject(rampUsers(userCount) during (rampDuration.seconds)).protocols(httpProtocol),
    Scenarios.highPurchase
      .inject(rampUsers(5) during (10.seconds)).protocols(httpProtocol)
  )


//  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)  // DEBUG variant

//  setUp(                                                     // REGULAR / OPEN simulation
//    scn.inject(
//      atOnceUsers(3),
//      nothingFor(5.seconds),
//      rampUsers(10) during(20.seconds),
//      nothingFor(10),
//      constantUsersPerSec(1) during(20.seconds)
//    )
//  ).protocols(httpProtocol)

//  setUp(                                                     // CLOSED MODEL simulation
//    scn.inject(
//      constantConcurrentUsers(10) during(20.seconds),
//      rampConcurrentUsers(10) to (20) during(20.seconds)
//    )
//  ).protocols(httpProtocol)

                                                               // THROTTLE simulation
//  setUp(scn.inject(constantUsersPerSec(1) during(3.minutes))).protocols(httpProtocol).throttle(
//    reachRps(10) in (30.seconds),
//    holdFor(60.seconds),
//    jumpToRps(20),
//    holdFor(60.seconds)
//  ).maxDuration(3.minutes)


}
