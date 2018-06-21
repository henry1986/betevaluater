/*
 * Copyright (c) 2018 Martin Heinrich
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package org.daiv

import org.daiv.immutable.utils.persistence.annotations.DatabaseWrapper
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset


val landShorts = mapOf("Russland" to "RU",
                       "Saudi-Arabien" to "SA",
                       "Ägypten" to "EGY",
                       "Uruguay" to "URU",
                       "Marokko" to "MAR",
                       "Iran" to "IRN",
                       "Portugal" to "PRT",
                       "Spanien" to "SPA",
                       "Frankreich" to "FR",
                       "Australien" to "AUS",
                       "Argentinien" to "ARG",
                       "Island" to "ISL",
                       "Peru" to "PER",
                       "Dänemark" to "DEN",
                       "Kroatien" to "CRO",
                       "Nigeria" to "NIG",
                       "Deutschland" to "GER",
                       "Costa Rica" to "CRC",
                       "Serbien" to "SRB",
                       "Mexiko" to "MEX")


fun getBets(toList: List<BetData>): Pair<List<BetData>, List<PointData>> {
    val (results, bets) = toList.partition { it.betKey.user == User.RESULT }
//    results.forEach { println(it) }
//    bets.forEach { println(it) }
    val evaluatedBets = bets.map {
        PointData.create(it, results.find { r -> it.betKey.match.equalsSameDayAndTeam(r.betKey.match) }?.result
                ?: Result.NONE)
    }
    return Pair(results, evaluatedBets)
}

fun main(args: Array<String>) {
    val inputstream = File("/home/mheinrich/Downloads/gruppenphase2.csv").inputStream()
    var list: List<String> = listOf()
    val use = inputstream.bufferedReader()
        .useLines {
            list = it.toList()
        }
    val users = list.first()
        .trim(',')
        .split(",")
        .asSequence()
        .filter { it != "" }
        .map { User(it) }
        .toList()

    //    println(payers)
    data class DateMatch(val date: String, val match: Match)

    var dateMatchList: List<DateMatch> = listOf()
    val toList = list.drop(2)
        .dropLast(1)
        .flatMap {
            val toMutableList = it.split(",")
                .toMutableList()
//            toMutableList.removeAt(3)
            val filter = (0..(toMutableList.size - 1))
                .asSequence()
//                .filter { it < 5 || toMutableList[it] != "" }
                .map { toMutableList[it] }
                .toList()
//            filter.forEach { println(it) }
            val date = filter[0]
            val match = Match(date, "", Team(filter[1]), Team(filter[2]))
//            println(match)
            dateMatchList += DateMatch(date, match)
            val bets = filter.drop(3)

            val toList = (0..(users.size - 1)).map {
                val betIt = if (it == 0) 0 else it * 2 - 1
                BetData(BetKey(match, users[it]),
                        if (it < users.size) Result.create(bets[betIt]) else Result.none())
            }
                .toList()
            toList
        }
        .toList()
        .filter { it.result != Result.NONE }
    val d = BetDatabase(DatabaseWrapper.create("bet.db"))
    d.open()
    d.store(toList)
    d.close()
    val (results, evaluatedBets) = getBets(toList)
    val plainUsers = users.filter { it != User.RESULT }
    File("/home/mheinrich/Downloads/wm_withPoints.csv").printWriter(Charset.defaultCharset())
        .use { writer ->
            writer.print(",,,,")
            plainUsers.forEach {
                writer.print("${it.name},,")
            }
            writer.println()
            writer.println(list[1])
            (0..(dateMatchList.size - 1)).forEach { i ->
                val (date, match) = dateMatchList[i]
                val result = results.find { it.betKey.match == match }?.result ?: Result.NONE
                writer.print("$date, ${match.team1},${match.team2}, $result")
                plainUsers.forEach { user ->
                    val find = evaluatedBets.find { it.betData.betKey.user == user && it.betData.betKey.match == match }
                    writer.print(",${find?.betData?.result ?: Result.NONE}, ${find?.points ?: Points.NONE}")
                }
                writer.println()
//            print(playerWithPoints[0].bets.g.date)
            }
            writer.print("Zwischensumme aus der Gruppenphase: ,,")
            plainUsers.forEach { user ->
                val sum = evaluatedBets.filter { it.betData.betKey.user == user }
                    .map { it.points }
                    .filter { it != Points.NONE }
                    .map(Points::int)
                    .sum()
                writer.print(",,$sum")
            }

            writer.println()
        }
}