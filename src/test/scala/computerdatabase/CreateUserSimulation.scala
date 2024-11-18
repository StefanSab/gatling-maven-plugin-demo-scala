package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.util.Random
import scala.concurrent.duration._

// can be omitted if you don't use jdbcFeeder
import io.gatling.jdbc.Predef._
// used for specifying durations with a unit, eg "5 minutes"
import scala.concurrent.duration._
/**
 * This sample is based on our official tutorials:
 *
 *   - [[https://docs.gatling.io/tutorials/recorder/ Gatling quickstart tutorial]]
 *   - [[https://docs.gatling.io/tutorials/advanced/ Gatling advanced tutorial]]
 */
class CreateUserSimulation extends Simulation {

  //-----------HTTP---------------

  val httpProtocol = http
    .baseUrl("http://localhost:8080") // Base URL for the web app
    .acceptHeader("application/json") // Common headers
    .contentTypeHeader("application/json")

  http.maxConnectionsPerHost(10)
  //http.disableCaching //might be necessary

  //------------User Data-------------

  val numUsers = 20  // Define how many name-email pairs to generate

  // Create a feeder that generates unique user data (name and email)
  val feeder = Iterator.continually {
    Map(
      "name" -> s"nameT${Random.alphanumeric.take(20).mkString}",
      "email" -> s"nameT${Random.alphanumeric.take(20).mkString}@mail.com"
    )
  }


  //-------------Scenarios---------------
  val scn = scenario("User Creation")
    .feed(feeder) // Inject unique data for each user

    .exec(
      http("Create User")
        .post("/api/v2/users") // The API endpoint for creating a user
        .body(StringBody(
          """{
        "user_name": "#{name}",
        "user_email": "#{email}"
      }"""
        )).asJson
        .check(status.in(200 to 299)) // Check for successful status codes
        .check(bodyString.saveAs("responseBody"))  // Save the response body
    )
    .exec(session => {
      val responseBody = session("responseBody").as[String]
      println(s"Response: $responseBody")  // Print full response for debugging
      session
    })
    .exec(session => {
      val name = session("name").as[String]
      val email = session("email").as[String]
      println(s"Creating user with name: $name and email: $email") // Print name and email
      session
    })
    //.pause(3)

  setUp(
    scn.inject(
      atOnceUsers(numUsers) // Simulate numUsers user at once
    ).protocols(httpProtocol)
  )
   /* .throttle(
      reachRps(numUsers) in (30 seconds),  // Allow 10 requests per second over 30 seconds
      holdFor(1.minute)              // Hold that rate for 1 minute
    )
*/
}
