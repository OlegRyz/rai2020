package org.niksi.rai.controller

import model.*
import kotlin.math.abs

val DO_NOTHING = MetaAction {
    mutableMapOf()
}

val COLLECT_RESOURCES = MetaAction {
    it.myBuilders.collect()
}

val ATTACK_ENEMY = MetaAction {
    it.myInfantry.attackClosestToYou(it.enemyUnits)
}

val BUILD_UNIT_BUILDER = MetaAction {
    it.myBuilderBase.createBuilder(it)
}

private fun List<Entity>.attackClosestToYou(targets: List<Entity>) = act {
    val closest = targets.minByOrNull { target -> distance(it.position, target.position) }
    when (closest) {
        null -> EntityAction(null, null, null, null);
        else -> EntityAction(
                MoveAction(closest.position, false, false),
                null,
                AttackAction(closest.id, null),
                null)
    }
}

fun distance(position: Vec2Int, target: Vec2Int) = manhDistance(position.x, position.y, target.x, target.y)

fun manhDistance(x: Int, y: Int, x1: Int, y1: Int) = abs(x - x1) + abs(y - y1)

fun List<Entity>.act(action: (Entity) -> EntityAction) = associateBy({ it.id }, action).toMutableMap()


private fun Entity.createBuilder(fieldState: FieldState): MutableMap<Int, EntityAction> {
    val size = fieldState.properties(this).size
    var gatePosition = Vec2Int(position.x + size, position.y + size - 1)
    return mutableMapOf(id to EntityAction(
        null,
        BuildAction(EntityType.BUILDER_UNIT, gatePosition),
        null,
        null))
}

private fun List<Entity>.collect() = act {
    EntityAction(
            null,
            null,
            AttackAction(null, AutoAttack(10, arrayOf(EntityType.RESOURCE))),
            null)
}

class MetaAction(val decoder: (FieldState) -> MutableMap<Int, EntityAction>) {
    fun DecodeToAction(state: FieldState) = Action(decoder(state))
}