package org.niksi.rai.strategies

import model.*
import org.niksi.rai.controller.*
import org.niksi.rai.langpack.*

val F_FREE_WORKERS_COLLECT_RESOURCES = MetaAction("F_FREE_WORKERS_COLLECT_RESOURCES") { state ->
    state.ordersCache.getEntities(this, state.myBuilders)
        .plus(state.myFreeBuilders)
        .filter { builder -> noResourcesHere(state, builder) }
        .map { builder ->
            state.resources.closest(builder.position)?.run {
                state.recordOrder(builder, this@MetaAction)
                builder.id to builder.attack(this)
            }
        }.toAction()
}

val usedGates: MutableList<Vec2Int> = mutableListOf()
val F_PRODUCE_BUILDER = MetaAction("F_PRODUCE_BUILDER") { state ->
    if (usedGates.size > 19) {
        usedGates.clear()
    }
    if (state.myBuilderBase == null || state.myPopulationLimit <= state.myPopulation) {
        mutableMapOf()
    } else {
        state.myBuilderBase.run {
            val gate = getGates(state, state.properties(this).size, position)
                .filter { !(it in usedGates) }
                .closest(state.resources
                    .map { it.position })
            if (gate == null) {
                mutableMapOf()
            } else {
                usedGates.add(gate)
                produce2(state, EntityType.BUILDER_UNIT, gate)
            }
        }
    }
}.doAlwaysWhenNotChosen {
    it.myBuilderBase.stopProduction()
}

val F_BUILD_HOUSE = MetaAction("F_BUILD_HOUSE") {
    build2(it, EntityType.HOUSE)
}

private fun MetaAction.build2(state: FieldState, buildingType: EntityType): MutableMap<Int, EntityAction>? {
    val spotSize = state.properties(buildingType).size
    val spots = findEmptySpots(spotSize, state, buildingType)
    val spot  = spots.closest(state.myBuilders.map { it.position })

    return if (spot != null) {
        if (buildingType == EntityType.RANGED_BASE) {
            println("build ranged")
        }
        state.myBuilders
            .closest(spot.shift(2,2))
            ?.also { builder ->
                println("Spot = ${spot.toStr()}; ${builder.position.toStr()}; Id: ${builder.id}")
                state.recordOrder(builder, this)
            }
            ?.build2(state, buildingType, spot)
    } else {
        mutableMapOf()
    }
}

fun findEmptySpots(spotSize: Int, state: FieldState, type: EntityType) = when(type) {
    EntityType.RANGED_BASE -> RangedBaseSpots.filter { available(it, state, spotSize)}
    else -> HouseSpots.filter { available(it, state, spotSize) }
}

fun Entity.build2(
    state: FieldState,
    type: EntityType,
    position: Vec2Int,
): MutableMap<Int, EntityAction> {
    val size = state.properties(type).size
    val buildPosition = getGates(state, size, position)
        .filter { available(it, state, 1)}
        .closest(listOf(this.position))
    return if (buildPosition != null) {
        id.build2(state, type, position, buildPosition)
    } else {
        mutableMapOf()
    }
}

fun Int.build2(
    state: FieldState,
    type: EntityType,
    position: Vec2Int,
    buildPosition: Vec2Int,
): MutableMap<Int, EntityAction> {

    return mutableMapOf(
        this to EntityAction(
            MoveAction(buildPosition, false, true),
            BuildAction(type, position.limitToMap()),
            null,
            null
        )
    )
}
fun Entity.produce2(
    fieldState: FieldState,
    type: EntityType,
    gatePosition: Vec2Int
): MutableMap<Int, EntityAction> {
    return mutableMapOf(
        id to EntityAction(
            null,
            BuildAction(type, gatePosition.coerce(globalSettings.mapSize)),
            null,
            null
        )
    )
}

private fun List<Vec2Int>.closest(targets: List<Vec2Int>) =
    minByOrNull { point -> targets.map { target -> distance(point, target) }.minOrNull() ?: 1000 } ?: firstOrNull()

private fun getGates(state: FieldState, size: Int, position: Vec2Int): List<Vec2Int> = with(size) {
    listOf(
        List(this) {Vec2Int(-1, it)},
        List(this) {Vec2Int(it, this)},
        List(this) {Vec2Int(this, it)},
        List(this) {Vec2Int(it, -1)},
    )
        .flatten()
        .map { Vec2Int(it.x + position.x, it.y + position.y) }
}

private fun noResourcesHere(state: FieldState, builder: Entity) =
    state.resources.none { isConnectedPoints(it.position, builder.position) }

fun isConnectedPoints(position: Vec2Int, position1: Vec2Int): Boolean {
    return manhDistance(position,position1) == 1
}

fun List<Pair<Int, EntityAction?>?>.toAction() =
    filterNotNull()
    .filter {it.second != null}
    .map{it.first to  it.second!!}
    .toMap()
    .toMutableMap()

val FastBuilding = StrategicDsl {
    F_FREE_WORKERS_COLLECT_RESOURCES.rule("F_FREE_WORKERS_COLLECT_RESOURCES")  { state ->
        applicableFor(state.myBuilders.count() < 20)
    }

    F_PRODUCE_BUILDER.rule("F_PRODUCE_BUILDER") { state ->
        applicableFor(state.myBuilders.count() < 20 && state.me.resource >
                state.properties(EntityType.BUILDER_UNIT).initialCost + state.myBuilders.count())
    }

    F_BUILD_HOUSE.rule("Build a house if food is low") {
        true.isGood()
        (it.myPopulationLimit - it.myPopulation < 3).isAlwaysNeeded()
        (it.me.resource + it.myFreeBuilders.count() < it.properties(EntityType.HOUSE).initialCost).isNotAcceptable()
        (it.myPopulation == 0 || it.myPopulationLimit > 2 * it.myPopulation).isNotAcceptable()
        if (it.myBuildings.any{!it.active}) {
            it.canceldOrder(BUILD_HOUSE)
        }
    }

    REPAIR_BUILDINGS_ALL.rule("") {
        (it.myUnhealthyBuildings.any()).isAlwaysNeeded()
        (it.myUnhealthyBuildings.isEmpty())
            .alsoCancelOrder(REPAIR_BUILDINGS_ALL).isNotAcceptable()
    }

    BUILD_BASE_RANGED.rule("Builders are Limited") {
        true.isNotAcceptable()
        (it.myRangedBase == null && it.me.resource + it.myBuilders.count() * 3 > 500).isAlwaysNeeded()
    }
}

private fun RulesContext.applicableFor(condition: Boolean) {
    when (condition) {
        true -> true.isAlwaysNeeded()
        else -> true.isNotAcceptable()
    }
}

private fun Entity.attack(target: Entity) = EntityAction(
    MoveAction(target.position, true, false),
    null,
    AttackAction(null, AutoAttack(0, arrayOf(EntityType.RESOURCE))),
    null
)
