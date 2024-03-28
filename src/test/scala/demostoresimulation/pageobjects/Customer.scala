package demostoresimulation.pageobjects

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Customer {

  private val csvFeederLoginDetails = csv("data/loginDetails.csv").circular

  def login = {
    feed(csvFeederLoginDetails)
      .exec(http("Load Login Page")
        .get("/login")
        .check(status.is(200)))
//        .exec { session => println(session); session }
      .exec(http("Customer Login Action")
        .post("/login")
        .formParam("_csrf", "#{csrfValue}")
        .formParam("username", "#{username}")
        .formParam("password", "#{password}")
        .check(status.is(200)))
      .exec(session => session.set("customerLoggedIn", true))
//        .exec { session => println(session); session }
  }
}
