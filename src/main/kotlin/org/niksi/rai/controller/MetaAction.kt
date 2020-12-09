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

val GEATHER_ARMY = MetaAction {
    it.myInfantry.move(it.myInfantry.middlePoint())
}

val BUILD_UNIT_BUILDER = MetaAction {
    it.myBuilderBase?.build(it, EntityType.BUILDER_UNIT)
}

val BUILD_BASE_RANGED = MetaAction {
    val position = Vec2Int(0, 0)
    it.myBuilders.closest(position)?.build(it, EntityType.MELEE_BASE, position) ?: mutableMapOf()
}


val BUILD_UNIT_MELEE = MetaAction {
    it.myMeleeBase?.build(it, EntityType.MELEE_UNIT)
}

val BUILD_UNIT_RANGED = MetaAction {
    it.myRangedBase?.build(it, EntityType.RANGED_UNIT)
}

fun List<Entity>.closest(position: Vec2Int) = minByOrNull { distance(it.position, position) }

private fun List<Entity>.attackClosestToYou(targets: List<Entity>) = act {
    val closest = targets.closest(it.position)
    when (closest) {
        null -> EntityAction(null, null, null, null);
        else -> EntityAction(
            MoveAction(closest.position, false, false),
            null,
            AttackAction(closest.id, null),
            null
        )
    }
}

private fun List<Entity>.move(middlePoint: Vec2Int) = act {
    EntityAction(
        MoveAction(middlePoint, true, false),
        null,
        AttackAction(null, AutoAttack(10, arrayOf(EntityType.MELEE_UNIT, EntityType.RANGED_UNIT))),
        null
    )
}

fun List<Entity>.middlePoint(): Vec2Int {
    val x = this.map { it.position.x }.average()
    val y = this.map { it.position.y }.average()
    return Vec2Int(x.toInt(), y.toInt())
}

fun runningAverage(i: Int, acc: Int, item: Int) = acc + (item - 1) / i


fun distance(position: Vec2Int, target: Vec2Int) = manhDistance(position.x, position.y, target.x, target.y)

fun manhDistance(x: Int, y: Int, x1: Int, y1: Int) = abs(x - x1) + abs(y - y1)

fun List<Entity>.act(action: (Entity) -> EntityAction) = associateBy({ it.id }, action).toMutableMap()

private fun Entity.build(
    fieldState: FieldState,
    type: EntityType,
    position: Vec2Int = this.position
): MutableMap<Int, EntityAction> {
    val properties = fieldState.properties(this)
    val size = properties.size
    var gatePosition = Vec2Int(position.x + size, position.y + size - 1)
    return mutableMapOf(
        id to EntityAction(
            null,
            BuildAction(type, gatePosition),
            null,
            null
        )
    )
}

private fun List<Entity>.collect() = act {
    EntityAction(
        null,
        null,
        AttackAction(null, AutoAttack(10, arrayOf(EntityType.RESOURCE))),
        null
    )
}

class MetaAction(val decoder: (FieldState) -> MutableMap<Int, EntityAction>?) {
    fun DecodeToAction(state: FieldState) = Action(decoder(state)?: mutableMapOf())
    fun log(): MetaAction {
        println(this)
        return this
    }
}