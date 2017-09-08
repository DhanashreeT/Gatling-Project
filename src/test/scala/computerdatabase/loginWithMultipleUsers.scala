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
import io.gatling.http.config.HttpProtocolBuilder
import com.typesafe.config._
import io.gatling.core.feeder.{Record, RecordSeqFeederBuilder}
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}

class loginWithMultipleUsers extends Simulation {

  val conf: Config = ConfigFactory.load("data/application.properties")
  val base_url: String = conf.getString("baseUrl")
  println(s"My secret value is $base_url")
  val httpConf: HttpProtocolBuilder = http.baseURL(base_url)
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

  val feeder: RecordSeqFeederBuilder[String] = csv("loginMultipleUsers.csv").queue
  val queueSize: IndexedSeq[Record[String]] = feeder.records
  println("Processing " + queueSize.toString + " records from loginMultipleUsers.csv file ")

  val count = new java.util.concurrent.atomic.AtomicInteger(0)

  val headers_1 = Map("Content-Type" -> "application/json") // Note the headers specific to a given request

  object Login {

    val scn1: ChainBuilder =
      feed(feeder).exec(http("login - multiple users") // Here's an example of a POST request
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


  val scn: ScenarioBuilder = scenario("Scenario1").exec(Login.scn1)

  setUp(scn.inject(rampUsers(10) over 20)).protocols(httpConf)

//  setUp(scn.inject(atOnceUsers(10)).protocols(httpConf))
}
