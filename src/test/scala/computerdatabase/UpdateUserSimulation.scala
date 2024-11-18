package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.util.Random

class UpdateUserSimulation extends Simulation{


  // --------- HTTP Configuration ---------
  val httpProtocol = http
    .baseUrl("http://localhost:8080") // Base URL for the backend
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")



  val randomUserDataFeeder = Iterator.continually {
    Map(
      "newName" -> s"UpdatedName_${Random.alphanumeric.take(10).mkString}",
      "newEmail" -> s"updated_${Random.alphanumeric.take(10).mkString}@foo.com"
    )
  }

  // --------- Scenario: Fetch All Users and Update ---------
  val scn = scenario("Update All Users")
    // Step 1: Fetch all user IDs
    .exec(
      http("Fetch User IDs")
        .get("/api/v2/users")
        .check(jsonPath("$[*].id").findAll.saveAs("userIds")) // Save all IDs to the session
    )
    // Step 2: Loop through each user and update their data
    .exec(session => {
      val userIds = session("userIds").as[Seq[String]]
      session.set("userIdsIterator", userIds.iterator) // Store an iterator for user IDs
    })
    .asLongAs(session => session("userIdsIterator").as[Iterator[String]].hasNext) {
      exec(session => {
        val userId = session("userIdsIterator").as[Iterator[String]].next()
        session.set("currentUserId", userId) // Set the current user ID
      })
        .feed(randomUserDataFeeder) // Generate new name and email for each user
        .exec(
          http("Update User")
            .patch("/api/v2/users/#{currentUserId}")
            .body(StringBody(
              """{
              "user_name": "#{newName}",
              "user_email": "#{newEmail}"
            }"""
            )).asJson
            .check(status.is(200)) // Verify successful update
        )
        .exec(session => {
          val updatedName = session("newName").as[String]
          val updatedEmail = session("newEmail").as[String]
          val userId = session("currentUserId").as[String]
          println(s"Updated User ID: $userId to Name: $updatedName and Email: $updatedEmail")
          session
        })
    }

  // --------- SetUp ---------
  setUp(
    scn.inject(atOnceUsers(1)) // Run with 1 virtual user or Update to match Ids count
  ).protocols(httpProtocol)
}
