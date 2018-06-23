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

import io.ktor.application.call
import io.ktor.content.default
import io.ktor.content.files
import io.ktor.content.resources
import io.ktor.content.static
import io.ktor.html.respondHtml
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.experimental.launch
import kotlinx.html.*
import org.daiv.bet.getResults
import org.daiv.immutable.utils.persistence.annotations.DatabaseWrapper
import java.text.SimpleDateFormat
import java.util.*

fun toDate(string: String): Date {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
    return simpleDateFormat.parse(string)
}

fun toLong(string: String): Long {
    return toDate(string).time
}

fun toOtherFormat(string: String): String {
    val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm")
    simpleDateFormat.timeZone = TimeZone.getTimeZone("CET")
    return simpleDateFormat.format(toDate(string))
}

fun TR.head(users: List<UserPoints>) {
    td("head") { +"Anstoßzeit" }; td("head") { +"Begegnung" }; td("head") { +"Ergebnis" }
    users.forEach { td("head") { +it.user.name }; td("head") }
}

fun TR.subHead(users: List<UserPoints>) {
    td("column1"); td("column1"); td("result")
    users.forEach { td { +"Tipp" }; td { +"Punkte" } }
}

fun TR.bets(result: BetData, users: List<UserPoints>, bets: List<PointData>) {
    val match = result.betKey.match
    td("column1") { +toOtherFormat(match.date) }; td("column1") { +match.toString() }
    td("result") { +result.result.toString() }
    users.forEach { user ->
        bets.find { bet -> bet.betData.betKey.user == user.user && match.equalsSameDayAndTeam(bet.betData.betKey.match) }?.let {
            td { +"${it.betData.result} " }
            if (it.betData.betKey.user != User.RESULT) {
                td { +"${it.points} " }
            }
        } ?: run { td { }; td { } }
    }
}

fun TR.footer(users: List<UserPoints>, bets: List<PointData>) {
    val className = "head footer"
    td(className); td(className); td(className) { +"Zwischenstand: " }
    users.forEach { user ->
        td(className) { +user.user.name }; td(className) { +user.points.toString() }
    }
}

fun TABLE.bet(users: List<UserPoints>, bets: List<PointData>, results: List<BetData>) {
    tr { head(users) }
    tr { subHead(users) }
    results.forEach { result -> tr { bets(result, users, bets) } }
    tr { footer(users, bets) }
}

fun TABLE.ranking(users: List<UserPoints>) {
    val userPoints = users.sortedWith(compareByDescending { it.points })
    tr("headRanking") { td { +"Platzierung" };td("playerColumn") { +"Spieler" }; td { +"Punkte" } }
    (1..userPoints.size).forEach {
        tr {
            val userPoint = userPoints[it - 1]
            td { +it.toString() }
            td("playerColumn") { +userPoint.user.name }
            td { +userPoint.points.toString() }
        }
    }
}

data class UserPoints(val user: User, val points: Int)

fun BODY.thisBody(d: BetDatabase, head: String) {
    val (res, bets) = getBets(d.readAll())
    val results = res.sortedWith(compareBy { toLong(it.betKey.match.date) })
    val userPoints = d.getUsers()
        .filter { it != User.RESULT }
        .map { user ->
            val sum = bets.filter { it.betData.betKey.user == user }
                .map { it.points }
                .filter { it != Points.NONE }
                .map(Points::int)
                .sum()
            UserPoints(user, sum)
        }
    h1 { +head }
    p {
        table("tableBet") {
            bet(userPoints, bets, results)
        }
    }
    p("ranking") {
        br
        table("tableRanking ranking") {
            ranking(userPoints)
        }

    }
}

fun main(args: Array<String>) {
    val d = BetDatabase(DatabaseWrapper.create("bet.db"))
    d.open()
    launch { getResults(d) }
    val server = embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                call.respondHtml {
                    val head = "WM Tipps Familie Kunz"
                    head {
                        title(head)
                        styleLink("static/styles.css")
                    }
                    body {
                        thisBody(d, head)
                    }
                }
            }
            static("static") {
                resources("css")
                files("css")
                default("index.html")
            }
        }
    }
    server.start(wait = true)
}