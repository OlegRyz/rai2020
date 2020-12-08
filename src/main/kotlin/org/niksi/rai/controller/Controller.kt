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
    val dsl = StrategicDsl()
    val predictor = Predictor(dsl)

    fun tick(currentTick: Int, entities: Array<Entity>, entityProperties: MutableMap<EntityType, EntityProperties>, players: Array<Player>) {
        val state = FieldState(entities, entityProperties, players, myId)
        bestAction = thoughtfulActions().takeBest(state).feedbackTo(predictor).DecodeToAction(state)
    }

    private fun thoughtfulActions(): Iterable<MetaAction> = listOf(COLLECT_RESOURCES, ATTACK_ENEMY, BUILD_UNIT_BUILDER)

    fun Iterable<MetaAction>.takeBest(state: FieldState) = maxByOrNull { predictor.predict(it, state) } ?: DO_NOTHING
}

