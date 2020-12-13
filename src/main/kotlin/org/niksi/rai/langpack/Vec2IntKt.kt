package org.niksi.rai.langpack

import model.Vec2Int
import org.niksi.rai.controller.distance
import org.niksi.rai.controller.globalSettings
import kotlin.math.roundToInt
import kotlin.random.Random

fun Vec2Int.limitToMap() = Vec2Int(
    x.coerceIn(0..globalSettings.mapSize),
    y.coerceIn(0..globalSettings.mapSize)
)

fun Vec2Int.coerce(mapSize: Int): Vec2Int = Vec2Int(this.x.coerceIn(0..mapSize), this.y.coerceIn(0..mapSize))


fun Pair<Vec2Int, Vec2Int>.transitToDistance(expDistance: Int):Vec2Int {
    val curDistance = distance(first, second)
    return if (curDistance == 0) {
        Vec2Int(first.x, first.y + expDistance)
    } else {
        val dx = (expDistance * (second.x - first.x)).toFloat() / curDistance
        val dy = (expDistance * (second.y - first.y)).toFloat() / curDistance
        Vec2Int((first.x + dx).roundToInt(), (first.y + dy).roundToInt())
    }
}

fun Vec2Int.randomShift(dx: Int, dy: Int) = Vec2Int(
    Random.nextInt(x - dx, x + dx),
    Random.nextInt(y - dy, y + dy)
)

fun Vec2Int.shift(x: Int, y: Int) = Vec2Int(this.x + x, this.y + y)

fun Vec2Int.isSame(position: Vec2Int) = this.x == position.x && this.y == position.y


fun Vec2Int.toStr() = "($x , $y)"


fun notIntersect(a: Vec2Int, b: Vec2Int, sizeA: Int, sizeB: Int) =
    a.x + sizeA - 1 < b.x ||
            b.x + sizeB - 1 < a.x ||
            a.y + sizeA - 1 < b.y ||
            b.y + sizeB - 1 < a.y
