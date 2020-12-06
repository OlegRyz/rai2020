package org.niksi.rai.controller

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