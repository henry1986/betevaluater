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

import java.io.File

class Evaluater {

}

val landShorts = mapOf("Rußland" to "RU",
                       "Saudi" to "SA",
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
                       "Peru" to "PER", "Dänemark" to "DEN", "Kroatien" to "CRO", "Nigeria" to "NIG")

data class Team(val name: String) {
    val shortName: String = landShorts[name] ?: name

    override
    fun toString(): String {
        return "$shortName"
    }
}

data class Match(val team1: Team, val team2: Team) {
    override fun toString(): String {
        return "$team1 - $team2"
    }

    companion object {
        fun create(string: String): Match {
            val split = string.split("-")
            return Match(Team(split[0].trim()), Team(split[1].trim()))
        }
    }
}

data class Bet(val date: String, val match: Match, val bet: Result) {
    override fun toString(): String {
        return bet.toString()
    }

    private fun calculatePoints(ref: Result): Int {
        val diffHit = ref.getDiff() == bet.getDiff()
        return when {
            ref.home == bet.home && diffHit -> 3
            diffHit -> 2
            else -> 1
        }
    }

    fun points(ref: Result): Points {
        return Points(this, if (bet.isSame(ref)) calculatePoints(ref) else 0)
    }
}

data class Points(val bet: Bet, val points: Int) {
    override fun toString(): String {
        return "${bet.bet} ->     $points"
    }
}

data class Result(val home: Int, val remote: Int) {
    override fun toString(): String {
        return " $home : $remote "
    }

    fun isHomeWin() = home > remote
    fun isTie() = home == remote
    fun isRemoteWin() = home < remote

    fun isSame(result: Result): Boolean {
        return listOf.none { this.it() != result.it() }
    }

    fun getDiff() = Math.abs(home - remote)

    companion object {
        val listOf: List<Result.() -> Boolean> = listOf(Result::isHomeWin, Result::isRemoteWin, Result::isTie)
        private fun getNumber(string: String): Int {
            if (string == "") {
                return Int.MIN_VALUE
            }
            return Integer.valueOf(string.trim())
        }

        private fun get(split: List<String>): Result {
            return Result(getNumber(split[0]), getNumber(split[1]))
        }

        private fun transform(string: String, sign: List<String>): Result {
            return sign.find { string.contains(it) }?.let {
                get(string.split(it))
            } ?: run {
                throw RuntimeException("did not find a matching pattern for: $string")
            }
        }

        fun create(string: String): Result {
            return transform(string, listOf(":", "-", "–"))
        }
    }
}

data class Player(val name: String, val allPoints: Int, val bets: Map<Match, Bet>, val pointsPerBet: List<Points>) {
    fun add(matchBet: Bet): Player {
        return Player(name, allPoints, bets + (matchBet.match to matchBet), pointsPerBet)
    }

    fun compareResults(player: Player): Player {
        val points = player.bets.map { bets[it.key]!!.points(it.value.bet) }
        return Player(name, points.map { it.points }.sum(), bets, points)
    }

    override fun toString(): String {
        return "$name: $allPoints $pointsPerBet"
    }
}


fun main(args: Array<String>) {
    val inputstream = File("/home/mheinrich/Downloads/gruppenphase.csv").inputStream()
    var list: List<String> = listOf()
    val use = inputstream.bufferedReader()
        .useLines {
            list = it.toList()
        }
    val payers = list.first()
        .trim(',')
        .split(",")
        .asSequence()
        .filter { it != "" }
        .map { Player(it, 0, mapOf(), listOf()) }
        .toList()
//    println(payers)
    val toList = list.drop(2)
        .dropLast(33)
        .flatMap {
            val toMutableList = it.split(",")
                .toMutableList()
//            toMutableList.removeAt(3)
            val filter = toMutableList
                .asSequence()
                .filter { it != "" }
                .toList()
//            filter.forEach { println(it) }
            val date = filter[0]
            val match = "${filter[1]} - ${filter[2]}"
//            println(match)
            val bets = filter.drop(3)
            val toList = (0..(payers.size - 1)).map {
                payers[it].add(Bet(date,
                                   Match.create(match),
                                   Result.create(bets[it])))
            }
                .toList()
//            toList.forEach { println(it) }
            toList
        }
        .toList()

    val map1 = payers.map { player ->
        val toList1 = toList.filter { it.name == player.name }
            .flatMap {
                val map = it.bets.toList()
                    .map { it.second }
                    .filter { it.bet.home != Int.MIN_VALUE }
                map
            }
            .associateBy { it.match }
        Player(player.name, player.allPoints, toList1, player.pointsPerBet)
    }
    val results = map1.first()
    val playerWithPoints = map1.drop(1)
        .map { player -> player.compareResults(results) }
    println(results.bets)
    playerWithPoints.forEach { println(it) }
//    toList
//        .forEach { println(it) }
}