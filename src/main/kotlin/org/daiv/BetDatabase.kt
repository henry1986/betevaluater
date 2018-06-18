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
import org.daiv.reflection.database.SimpleDatabase
import org.daiv.reflection.persister.Persister

class BetDatabase(databaseWrapper: DatabaseWrapper) : SimpleDatabase by databaseWrapper {
    private val persister by lazy { Persister(databaseWrapper) }
    private val table by lazy {
        persister.persist(BetData::class)
        persister.Table(BetData::class)
    }

    fun store(list: List<BetData>) {

        list.forEach {
            if (table.exists(it.betKey)) {
                if (it.result != Result.NONE) {
                    table.update(it.betKey, BetData::result.name, it.result)
                }
                if (it.points != Points.NONE) {
                    table.update(it.betKey, BetData::points.name, it.points)
                }
            } else {
                table.insert(it)
            }
        }
    }

    fun getUsers(): List<User> {
        val distinctValues = table.distinctValues("betKey_user", User::class)
        val result = User.RESULT
        return listOf(result).plus(distinctValues.minus(result))
    }

    fun getMatches():List<Match>{
        val distinctValues = table.distinctValues("betKey_match", Match::class)
        return distinctValues
    }

    fun readAll(): List<BetData> {
        return table.readAll()
    }
}