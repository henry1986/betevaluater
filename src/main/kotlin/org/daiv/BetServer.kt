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
import io.ktor.html.respondHtml
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.html.*
import org.daiv.immutable.utils.persistence.annotations.DatabaseWrapper

fun main(args: Array<String>) {
    val d = BetDatabase(DatabaseWrapper.create("bet.db"))
    d.open()
    val server = embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                call.respondHtml {
                    head {
                        title("WM Tipps Kunz Familie")
                    }
                    unsafe {
                        raw("""
                            <style>
table {
    border-collapse: collapse;
    width: 100%;
}

table, td, th {
    height: 50px;
    border: 1px solid black;
}
</style>
                            """)
                    }
                    body {
                        h1 { +"WM Tipps Kunz Familie" }
                        p {
                            table {

                                val users = d.getUsers()
                                val readAll = d.readAll()
                                tr {
                                    td { +"Begegnung" }
                                    users.forEach {
                                        td { +it.name }
                                        td {}
                                    }
                                }
                                tr {
                                    td { }
                                    users.forEach {
                                        td { +"Tipp" }
                                        td { +"Punkte" }
                                    }
                                }
                                val matches = d.getMatches()
                                matches.forEach { match ->
                                    tr {
                                        td { +match.toString() }
                                        users.forEach { user ->
                                            readAll
                                                .find { bet -> bet.betKey.user == user && match == bet.betKey.match }?.let {
                                                    td { +"${it.result} " }
                                                    td { +"${it.points} " }

                                                } ?: run {
                                                td { }
                                                td { }
                                            }
                                        }
                                    }
                                }
                                tr{
                                    td { +"Zwischenstand: " }
                                    users.forEach { user ->
                                        val sum = readAll.filter { it.betKey.user == user }
                                            .map { it.points }
                                            .filter { it != Points.NONE }
                                            .map(Points::int)
                                            .sum()
                                        td { +"${user.name}" }
                                        td { +"$sum" }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    server.start(wait = true)
}