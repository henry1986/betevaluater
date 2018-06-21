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

data class Team(val name: String) {
    val shortName: String = landShorts[name] ?: name

    override
    fun toString(): String {
        return "$name"
    }
}

data class Match(val date: String, val header: String, val team1: Team, val team2: Team) {
    override fun toString(): String {
        return "$team1 - $team2"
    }

    fun equalsSameDayAndTeam(other:Match):Boolean{
        return other.team1 == team1 && other.team2 == team2
    }

    companion object {
        fun create(date: String, header: String, string: String): Match {
            val split = string.split("-")
            return Match(date, header, Team(split[0].trim()), Team(split[1].trim()))
        }
    }
}

data class User(val name: String) {
    companion object {
        val RESULT = User("Result")
    }
}

data class Result(val home: Int, val remote: Int) {
    override fun toString(): String {
        if (home == -1) {
            return ""//""not delivered"
        }
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
        val NONE = none()
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

        fun none(): Result {
            return Result(-1, -1)
        }

        fun create(string: String): Result {
            if (string == "") {
                return none()
            }
            return transform(string, listOf(":", "-", "–"))
        }
    }
}

data class Points(val int: Int) {
    override fun toString(): String {
        if (this == NONE) {
            return ""//""NONE"
        }
        return "$int"
    }

    companion object {
        val NONE = Points(-1)
    }
}


data class BetKey(val match: Match, val user: User)

data class PointData(val betData: BetData, val points: Points){

    companion object {
        private fun calculatePoints(bet: Result, ref: Result): Int {
            if (bet.isSame(ref)) {
                val diffHit = ref.getDiff() == bet.getDiff()
                return when {
                    ref.home == bet.home && diffHit -> 3
                    diffHit -> 2
                    else -> 1
                }
            }
            return 0
        }

        fun create(betData: BetData, result: Result): PointData {
            if (result == Result.NONE) {
                return PointData(betData, Points.NONE)
            }
            return PointData(betData, Points(calculatePoints(betData.result, result)))
        }
    }
}


data class BetData(val betKey: BetKey, val result: Result) {
//    private fun calculatePoints(bet: Result, ref: Result): Int {
//        if (bet.isSame(ref)) {
//            val diffHit = ref.getDiff() == bet.getDiff()
//            return when {
//                ref.home == bet.home && diffHit -> 3
//                diffHit -> 2
//                else -> 1
//            }
//        }
//        return 0
//    }
//
//    fun evaluate(result: Result): BetData {
//        if (result == Result.NONE) {
//            return this
//        }
//        return BetData(betKey, this.result, Points(calculatePoints(this.result, result)))
//    }
}
