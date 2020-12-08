package org.niksi.rai.controller

import model.*
import org.niksi.rai.strategies.Balanced
import kotlin.collections.*

class Controller(
        val myId: Int,
        val mapSize: Int,
        val maxPathfindNodes: Int,
        val maxTickCount: Int,
        val fogOfWar: Boolean) {
    var bestAction = Action()
    val dsl = Balanced
    val predictor = Predictor(dsl)

    fun tick(currentTick: Int, entities: Array<Entity>, entityProperties: MutableMap<EntityType, EntityProperties>, players: Array<Player>) {
        val state = FieldState(entities, entityProperties, players, myId)
        bestAction = thoughtfulActions().takeBest(state).log().DecodeToAction(state)
    }

    private fun thoughtfulActions(): Iterable<MetaAction> = listOf(
        DO_NOTHING, COLLECT_RESOURCES, ATTACK_ENEMY, BUILD_UNIT_BUILDER, BUILD_UNIT_MELEE, BUILD_UNIT_RANGED)

    fun Iterable<MetaAction>.takeBest(state: FieldState) = maxByOrNull { predictor.predict(it, state) } ?: DO_NOTHING
}

