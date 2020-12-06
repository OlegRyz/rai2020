package org.niksi.rai.controller

import model.*

class Controller(
        val myId: Int,
        val mapSize: Int,
        val maxPathfindNodes: Int,
        val maxTickCount: Int,
        val fogOfWar: Boolean) {
    var bestAction = Action()

    fun tick(currentTick: Int, entities: Array<Entity>, entityProperties: MutableMap<EntityType, EntityProperties>, players: Array<Player>) {

    }

}
