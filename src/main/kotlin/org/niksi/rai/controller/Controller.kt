package org.niksi.rai.controller

import model.*
import org.niksi.rai.strategies.Balanced
import kotlin.collections.*

data class GlobalSettings(val mapSize: Int) {
    val center = Vec2Int(mapSize / 2, mapSize / 2)
}

lateinit var globalSettings: GlobalSettings

class Controller(
        val myId: Int,
        val mapSize: Int,
        val maxPathfindNodes: Int,
        val maxTickCount: Int,
        val fogOfWar: Boolean) {
    var bestAction = Action()
    val dsl = Balanced
    val predictor = Predictor(dsl)
    val ordersCache = OrdersCache()

    init {
        globalSettings = GlobalSettings(mapSize)
    }

    fun tick(currentTick: Int, entities: Array<Entity>, entityProperties: MutableMap<EntityType, EntityProperties>, players: Array<Player>) {
        val state = FieldState(entities, entityProperties, players, myId, ordersCache)
        bestAction = thoughtfulActions().takeBest(state).log().DecodeToAction(state)
    }

    private fun thoughtfulActions(): Iterable<MetaAction> = listOf(
        DO_NOTHING, COLLECT_RESOURCES, ATTACK_ENEMY, BUILD_UNIT_BUILDER, BUILD_UNIT_MELEE, BUILD_UNIT_RANGED, GEATHER_ARMY,
        BUILD_HOUSE, REPAIR_BUILDINGS)

    fun Iterable<MetaAction>.takeBest(state: FieldState) = maxByOrNull { predictor.predict(it, state) } ?: DO_NOTHING
}

