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
        val currentActions = thoughtfulActions().takeBest(state).log().DecodeToAction(state)
        val reccurentActions = executeOrders(state)
        currentActions.putAll(reccurentActions)
        bestAction = Action(currentActions)
    }

    private fun executeOrders(state: FieldState): Map<out Int, EntityAction> {
        return state.ordersCache.executeOrders()
    }

    private fun thoughtfulActions(): Iterable<MetaAction> = listOf(
        DO_NOTHING, COLLECT_RESOURCES, ATTACK_ENEMY, BUILD_UNIT_BUILDER, BUILD_UNIT_RANGED, GEATHER_ARMY,
        BUILD_HOUSE, REPAIR_BUILDINGS_ALL, STOP_MAD_PRINTER, UNLEASH_MAD_PRINTER,
        DEFEND_BUILDINGS)

    fun Iterable<MetaAction>.takeBest(state: FieldState) = maxByOrNull { predictor.predict(it, state) } ?: DO_NOTHING
}

