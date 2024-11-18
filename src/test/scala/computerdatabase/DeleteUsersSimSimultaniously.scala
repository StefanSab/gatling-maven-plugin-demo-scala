package computerdatabase


  import io.gatling.core.Predef._
  import io.gatling.http.Predef._
  import scala.concurrent.duration._

  class DeleteUsersSimSimultaniously extends Simulation {

    //-----------HTTP---------------

    val httpProtocol = http
      .baseUrl("http://localhost:8080")
      .acceptHeader("application/json")
      .contentTypeHeader("application/json")

    //-----------Fetch Users Scenario---------------
      val fetchUsersScenario = scenario("Fetch User IDs")
      .exec(
        http("Get Users")
          .get("/api/v2/users")
          .check(jsonPath("$[*].id").findAll.saveAs("userIds")) // Save all IDs into session
      )
      .exec(session => {
        val userIds = session("userIds").as[Seq[String]]
        println(s"Fetched User IDs: $userIds")
        session.set("userIds", userIds) // Store the fetched IDs in the session for later use
      })

    //-----------Delete Users Scenario---------------
    val deleteUsersScenario = scenario("Delete User")
      .exec(session => {
        // Get the list of user IDs from session
        val userIds = session("userIds").as[Seq[String]]
        val userIdToDelete = userIds.headOption.getOrElse("") // Take the first ID from the list
        session.set("userIdToDelete", userIdToDelete) // Set the ID to delete in the session
      })
      .exec(
        http("Delete User")
          .delete("/api/v2/users/${userIdToDelete}") // Use the user ID to delete
          .check(status.is(204)) // Verify successful deletion
      )
      .exec(session => {
        // Remove the deleted user ID from the list and update the session with remaining IDs
        val remainingUserIds = session("userIds").as[Seq[String]].tail
        session.set("userIds", remainingUserIds) // Update session with remaining IDs
      })

    //-----------SetUp---------------
    setUp(
      fetchUsersScenario.inject(atOnceUsers(1)).protocols(httpProtocol), // Fetch user IDs first
      deleteUsersScenario.inject(atOnceUsers(20)).protocols(httpProtocol) // Simulate 20 users deleting users concurrently
    )
}
