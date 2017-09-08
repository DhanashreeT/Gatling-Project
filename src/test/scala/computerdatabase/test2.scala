/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class test2 extends Simulation {

  val httpConf = http
    .baseURL("http://lovenserver:8000") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers

  val feeder1 = csv("login.csv").queue
  val queueSize1 = feeder1.records
  println("Processing " + queueSize1.toString + " records from login.csv file ")

  val feeder2 = csv("questions.csv").queue
  val queueSize2 = feeder2.records
  println("Processing " + queueSize2.toString + " records from questions.csv file ")

  val count = new java.util.concurrent.atomic.AtomicInteger(0)

  val headers_1 = Map("Content-Type" -> "application/json") // Note the headers specific to a given request

  object Login {

    val scn1 = foreach(queueSize1, "record") {
      feed(feeder1).exec(http("login") // Here's an example of a POST request
        .post("/api/login")
        .headers(headers_1)
        //      .body(StringBody("""{ "username": "root" , "password": "root" }""")).asJSON
        .body(StringBody("""{ "username": "${username}" , "password": "${password}" }""")).asJSON
        .check(status is 200)
        .check(jsonPath("$.access_token").saveAs("myresponseToken"))
      ).exec(session => {
        val accessToken = session.get("myresponseToken").asOption[String]
        println(accessToken.getOrElse("COULD NOT FIND ACCESS TOKEN"))
        session
      }).pause(1)
    }
  }

  object Ask {

    val scn2 = foreach(queueSize2, "record") {

      feed(feeder2).
        exec(http("ask")
          .get("/api/ask/${questions}")
          .headers(Map("accessToken" -> "${myresponseToken}"))
          .check(status is 200)
          .check(jsonPath("$.sender").saveAs("responseSender"))
          .check(jsonPath("$.text").saveAs("responsetext"))
          .check(regex("sender"))
        )
        .exec(session => {
          val sendervalue = session.get("responseSender").asOption[String]
          val textSpeech = session.get("responsetext").asOption[String]

          println(sendervalue.getOrElse("COULD NOT FIND SENDER"))
          println(textSpeech.getOrElse("COULD NOT FIND TEXT"))
          session
        })
        .pause(2)
    }

  }

//  val scn = scenario("Test Scenario").exec(Login.scn1, Ask.scn2)

  val scn = scenario("Scenario1").exec(Login.scn1).exec(Ask.scn2)

//  setUp(scn.inject(atOnceUsers(2)).protocols(httpConf))

  setUp(scn.inject(rampUsers(1) over 20)).protocols(httpConf)
}
