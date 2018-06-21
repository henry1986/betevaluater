/*
 * Copyright (c) 2018 Martin Heinrich
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package org.daiv.bet

import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.daiv.*
import org.daiv.immutable.utils.persistence.annotations.DatabaseWrapper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ScheduledExecutorService

class BetRestClient {

}

data class MatchResult(val ResultID: Int,
                       val ResultName: String,
                       val PointsTeam1: Int,
                       val PointsTeam2: Int,
                       val ResultOrderID: Int,
                       val ResultTypeID: Int,
                       val ResultDescription: String)

fun getBetsFromJson(get: String): List<BetData> {
    return JSONArray(get)
        .map { json ->
            val obj = json as JSONObject
            val date = obj["MatchDateTimeUTC"] as String
            val team1 = (obj["Team1"] as JSONObject)["TeamName"] as String
            val team2 = (obj["Team2"] as JSONObject)["TeamName"] as String
            val match = Match(date, "", Team(team1), Team(team2))
            val results = obj["MatchResults"] as JSONArray

            val any = obj["MatchIsFinished"] as Boolean

            val find = results.map { res ->
                Gson().fromJson(res.toString(), MatchResult::class.java)
            }
                .find { it.ResultName == "Endergebnis" }

            val result = if (!any || find == null) Result.NONE else Result(find.PointsTeam1, find.PointsTeam2)
            BetData(BetKey(match, User.RESULT), result)
//        println(match.date)
        }
}

suspend fun getResults(d: BetDatabase) {
    val httpClient = HttpClient(Apache)
    while (true) {
        try {
            val get = httpClient.get<String>("https://www.openligadb.de/api/getMatchData/wm2018ru/2018")
//        println(jsonObject)
            d.store(getBetsFromJson(get))
        } catch (ex:Exception){
            println(ex)
        }
        delay(60 * 60 * 1000)
    }
}

fun main(args: Array<String>) {
    val get = File("out/production/resources/wm2018ru.json").bufferedReader()
        .readText()
    val betsFromJson = getBetsFromJson(get)
    val betDatabase = BetDatabase(DatabaseWrapper.create("bet.db"))
    betDatabase.open()
    val dataMachtches = betDatabase.getMatches()
    val matches = dataMachtches.map { it.team2 } + dataMachtches.map { it.team1 }

    val distinct = (betsFromJson.map { it.betKey.match.team2 } + betsFromJson.map { it.betKey.match.team1 }).distinct()
    val filter = matches.filter { !distinct.contains(it) }
    filter.forEach(::println)

//    while (true) {
//        Thread.sleep(1000)
//    }
}