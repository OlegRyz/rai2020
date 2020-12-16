package org.niksi.rai.strategies

import model.*
import org.niksi.rai.controller.*
import org.niksi.rai.langpack.coerce
import org.niksi.rai.langpack.stopProduction

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
    if (state.myBuilderBase == null) {
        mutableMapOf()
    } else {
        state.myBuilderBase.run {
            val gate = getGates(state)
                .filter { !(it in usedGates) }
                .closest(state.resources
                    .map { it.position })
            usedGates.add(gate)
            produce2(state, EntityType.BUILDER_UNIT, gate)
        }
    }
}.doAlwaysWhenNotChosen {
    it.myBuilderBase.stopProduction()
}

fun Entity.produce2(
    fieldState: FieldState,
    type: EntityType,
    gatePosition: Vec2Int
): MutableMap<Int, EntityAction> {
    val properties = fieldState.properties(this)
    val size = properties.size
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
    minByOrNull { point -> targets.map { target -> distance(point, target) }.minOrNull() ?: 1000 } ?: first()

private fun Entity.getGates(state: FieldState): List<Vec2Int> = with(state.properties(this).size) {
    listOf(
        List(this) { Vec2Int(-1, it) },
        List(this) { Vec2Int(it, this) },
        List(this) { Vec2Int(this, it) },
        List(this) { Vec2Int(it, -1) },
    )
        .flatten()
        .map { Vec2Int(it.x + position.x, it.y + position.y) }
}

private fun noResourcesHere(state: FieldState, builder: Entity) =
    state.resources.none { isConnectedPoints(it.position, builder.position) }

fun isConnectedPoints(position: Vec2Int, position1: Vec2Int): Boolean {
    return manhDistance(position, position1) == 1
}

fun List<Pair<Int, EntityAction?>?>.toAction() =
    filterNotNull()
        .filter { it.second != null }
        .map { it.first to it.second!! }
        .toMap()
        .toMutableMap()

val FastBuilding = StrategicDsl {
    F_FREE_WORKERS_COLLECT_RESOURCES.rule("F_FREE_WORKERS_COLLECT_RESOURCES") { state ->
        applicableFor(state.myBuilders.count() < 20)
    }

    F_PRODUCE_BUILDER.rule("F_PRODUCE_BUILDER") { state ->
        applicableFor(
            state.myBuilders.count() < 20 && state.me.resource >
                    state.properties(EntityType.BUILDER_UNIT).initialCost + state.myBuilders.count()
        )
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
