package org.niksi.rai.controller

import model.Vec2Int
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

fun test_transit() {
    for (i in 0..100) {
        val pointA = Vec2Int(Random.nextInt(1000),Random.nextInt(1000))
        val pointB = Vec2Int(Random.nextInt(1000),Random.nextInt(1000))
        val expDistance = Random.nextInt(15)
        val pointC = (pointA to pointB).transitToDistance(expDistance)
        val distance = distance(pointA, pointC)
        assertEquals("Fail for (${pointA.x}, ${pointA.y}), (${pointB.x}, ${pointB.y}) $expDistance;  pointD = (${pointC.x}, ${pointC.y}), $distance",
        expDistance, distance)
    }
}

