package org.niksi.rai.controller

import model.Vec2Int
import org.niksi.rai.langpack.closestBorder
import org.niksi.rai.langpack.toStr
import org.niksi.rai.langpack.transitToDistance
import kotlin.random.Random

fun Controller.checkConsistency(currentTick: Int, myId: Int, mapSize: Int, maxPathfindNodes: Int, maxTickCount: Int, fogOfWar: Boolean) {
    if (this.myId != myId ||
        this.mapSize != mapSize ||
        this.maxPathfindNodes != maxPathfindNodes ||
        this.maxTickCount != maxTickCount ||
        this.fogOfWar != fogOfWar) {
        throw(Exception("Variable changed (${this.myId}, ${this.mapSize}, ${this.maxPathfindNodes}, ${this.maxTickCount}, ${this.fogOfWar}) vs" +
                        "(${myId}, ${mapSize},  ${maxPathfindNodes}, ${maxTickCount}, ${fogOfWar}). Tick: $currentTick"))
    }
}
var testsFailed = false
fun runTests() {
    test_transit()
    test_closest_border()
    if (testsFailed) {
        throw java.lang.Exception("Tests failed")
    } else {
        println("Tests are good")
    }
}

fun failTest(s: String) {
    testsFailed = true
    println(s)
}

fun assertEquals(msg: String, expected: Any?, actual: Any?) {
    if (expected != actual) {
        failTest(msg + "; expected $expected but received $actual")
    }
}

fun assertTrue(msg: String, actual: Boolean) {
    if (!actual) {
        failTest(msg + "; expected True but received $actual")
    }
}

fun test_transit() {
    repeat(100) {
        val pointA = Vec2Int(Random.nextInt(1000),Random.nextInt(1000))
        val pointB = Vec2Int(Random.nextInt(1000),Random.nextInt(1000))
        val expDistance = Random.nextInt(15)
        val pointC = (pointA to pointB).transitToDistance(expDistance)
        val distance = distance(pointA, pointC)
        assertEquals("Fail for (${pointA.x}, ${pointA.y}), (${pointB.x}, ${pointB.y}) $expDistance;  pointD = (${pointC.x}, ${pointC.y}), $distance",
        expDistance, distance)
    }
}

fun test_closest_border() {
    test(Vec2Int(2,2), Vec2Int(3,4), 3, Vec2Int(3,5))
    test(Vec2Int(2,2), Vec2Int(3,7), 3, Vec2Int(3,5))
    test(Vec2Int(2,2), Vec2Int(4,7), 3, Vec2Int(4,5))
    test(Vec2Int(2,2), Vec2Int(2,10), 3, Vec2Int(2,5))
    test(Vec2Int(2,2), Vec2Int(1,10), 3, Vec2Int(1,4))
    test(Vec2Int(2,2), Vec2Int(5,13), 3, Vec2Int(5,4))
    test(Vec2Int(2,2), Vec2Int(7,13), 3, Vec2Int(5,4))
    test(Vec2Int(2,2), Vec2Int(13,13), 3, Vec2Int(5,4))
    test(Vec2Int(2,2), Vec2Int(13,7), 3, Vec2Int(5,4))
    test(Vec2Int(4,4), Vec2Int(1,1), 3, Vec2Int(3,4))
    test(Vec2Int(4,4), Vec2Int(5,1), 3, Vec2Int(5,3))
    test(Vec2Int(5,5), Vec2Int(1,1), 2, Vec2Int(4,5))
    test(Vec2Int(5,5), Vec2Int(5,1), 2, Vec2Int(5,4))
    test(Vec2Int(5,5), Vec2Int(1,7), 2, Vec2Int(4,6))
    test(Vec2Int(5,5), Vec2Int(5,9), 2, Vec2Int(5,7))
}

private fun test(startPoint: Vec2Int, endPoint: Vec2Int, size: Int, expected: Vec2Int) {
    val finalPoint = startPoint.closestBorder(endPoint, size)
    assertEquals("Final X point for ${startPoint.toStr()} and  ${endPoint.toStr()} is ${finalPoint.toStr()} for size: ${size}", expected.x, finalPoint.x)
    assertEquals("Final Y point for ${startPoint.toStr()} and  ${endPoint.toStr()} is ${finalPoint.toStr()} for size: ${size}", expected.y, finalPoint.y)
}


