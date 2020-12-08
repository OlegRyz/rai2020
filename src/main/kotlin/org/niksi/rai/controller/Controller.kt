package org.niksi.rai.controller

import model.*
import kotlin.collections.*

class Controller(
        val myId: Int,
        val mapSize: Int,
        val maxPathfindNodes: Int,
        val maxTickCount: Int,
        val fogOfWar: Boolean) {
    var bestAction = Action()
    val predictor = Predictor()

    fun tick(currentTick: Int, entities: Array<Entity>, entityProperties: MutableMap<EntityType, EntityProperties>, players: Array<Player>) {
        val state = FieldState(entities, entityProperties, players, myId)
        bestAction = thoughtfulActions().takeBest().DecodeToAction(state)
    }

    private fun thoughtfulActions(): Iterable<MetaAction> = listOf(COLLECT_RESOURCES)

    fun Iterable<MetaAction>.takeBest() = maxByOrNull { predictor.predict() } ?: DO_NOTHING
}

