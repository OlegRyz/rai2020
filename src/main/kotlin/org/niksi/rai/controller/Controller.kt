package org.niksi.rai.controller

import model.*
import org.niksi.rai.strategies.*
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
    private val THRESHOLD = 0.05
    var bestAction = Action()
    val dsl = FastBuilding
    val predictor = Predictor(dsl)
    val ordersCache = OrdersCache()

    init {
        globalSettings = GlobalSettings(mapSize)
    }

    fun tick(currentTick: Int, entities: Array<Entity>, entityProperties: MutableMap<EntityType, EntityProperties>, players: Array<Player>) {
        val state = FieldState(entities, entityProperties, players, myId, ordersCache)
        val currentActions = thoughtfulActions()
            .takeBest(state)
            .addReccurent(state)
            .log(currentTick)
            .fold(mutableMapOf<Int, EntityAction>()) { acc, item ->
                acc.putAll(item.DecodeToAction(state))
                acc
            }
        val reccurentActions = executeOrders(state)
        currentActions.putAll(reccurentActions)
        bestAction = Action(currentActions)
    }

    private fun executeOrders(state: FieldState): Map<out Int, EntityAction> {
        return state.ordersCache.executeOrders()
    }

    private fun thoughtfulActions(): Iterable<MetaAction> = listOf(
        F_PRODUCE_BUILDER,
        F_BUILD_HOUSE,
        REPAIR_BUILDINGS_ALL,
        BUILD_BASE_RANGED,
        BUILD_UNIT_RANGED,
        ATTACK_ENEMY,
        ATTACK_DIAGONAL,
        ATTACK_NEIGHBOR,
        DEFEND_BUILDINGS,
    )

    fun Iterable<MetaAction>.takeBest(state: FieldState) = map { it to predictor.predict(it, state) }
        .sortedByDescending { it.second }
        .mapIndexed { i, (item, value) ->
            if (i < 2 && value > THRESHOLD) {
                item
            } else {
                item.opposite
            }
        }.filterNotNull()

    fun Iterable<MetaAction>.addReccurent(state: FieldState) = this.toList().toMutableList().apply {
        this.addAll(reccurent())
    }

    private fun reccurent(): List<MetaAction> = listOf(
        F_FREE_WORKERS_COLLECT_RESOURCES,
        CLEANUP_ORDERS,
        F_CLEANUP_GATES,
        ACTIVATE_TURRETS,
        RUN_AWAY_BUILDERS,
        ATTACK_NEIGHBOR_CLEANUP,
        BUILDERS_ATTACK_ENEMY_BUILDERS,
        BUILDERS_ATTACK_ENEMY_CLOSE
    )

    fun <T> T.log(currentTick: Int): T {
        println()
        println(currentTick)
        println(this)
        return this
    }
}

