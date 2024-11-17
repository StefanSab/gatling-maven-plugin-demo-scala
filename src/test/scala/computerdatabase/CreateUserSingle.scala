package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class CreateUserSingle extends Simulation{

  val httpProtocol = http
    .baseUrl("http://localhost:8080") // Base URL for the web app
    .acceptHeader("application/json") // Common headers
    .contentTypeHeader("application/json")

  http.maxConnectionsPerHost(10)
  //http.disableCaching //might be necessary

  val scn = scenario("User Creation")
    //.feed(userFeeder) // Inject unique data for each user

    .exec(
      http("Create User")
        .post("/api/v2/users") // The API endpoint for creating a user
        .body(StringBody(
          """{
            "user_name": "name5",
            "user_email": "name5@email.com}"
          }"""
        )).asJson
        .check(status.is(201)) // Verify that the response is 200 OK
    )
    .pause(3)
  setUp(
    scn.inject(
      atOnceUsers(1) // Simulate 10 users at once
    ).protocols(httpProtocol)
  )
}
