package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.util.Random

class DeleteUsersSimulation extends Simulation{

  //-----------HTTP---------------

  val httpProtocol = http
    .baseUrl("http://localhost:8080") // Base URL for the web app
    .acceptHeader("application/json") // Common headers
    .contentTypeHeader("application/json")

  //http.maxConnectionsPerHost(10)
   val userCount = 0

  //---------Scenario-------------------
  val scn = scenario("Fetch and Delete Users")
    .exec(
      http("Get Users")
        .get("/api/v2/users")
        .check(jsonPath("$[*].id").findAll.saveAs("ids")) // Extract all IDs
    )
    .exec(session => {
      val userIds = session("ids").as[Seq[String]]
      println(s"Fetched User IDs: $userIds")
      session.set("userIdsIterator", userIds.iterator)
    })
    .asLongAs(session => session("userIdsIterator").as[Iterator[String]].hasNext) { // Loop until all IDs are processed
      exec(session => {
        val userId = session("userIdsIterator").as[Iterator[String]].next()
        session.set("currentUserId", userId)
      })
        .exec(
          http("Delete User")
            .delete("/api/v2/users/#{currentUserId}")
            .check(status.is(204)) // Verify successful deletion
        )
    }

  // Setup
  setUp(
    scn.inject(atOnceUsers(1)).protocols(httpProtocol) //one at a time or update to match User count
  )

}
