package org.niksi.rai.controller

import model.*
import kotlin.math.abs
import kotlin.random.Random

val DO_NOTHING = MetaAction("DO_NOTHING") {
    mutableMapOf()
}

val COLLECT_RESOURCES = MetaAction("COLLECT_RESOURCES") {
    it.myBuilders.run {
        it.recordOrder(this, this@MetaAction)
        collect()
    }
}

val ATTACK_ENEMY = MetaAction("ATTACK_ENEMY") {
    it.myInfantry.attackClosestToYou(it.enemies)
}

val GEATHER_ARMY = MetaAction("GEATHER_ARMY") {
    it.myInfantry.move(it.myInfantry.middlePoint())
}

val BUILD_UNIT_BUILDER = MetaAction("BUILD_UNIT_BUILDER") {
    it.myBuilderBase?.produce(it, EntityType.BUILDER_UNIT)
}

val BUILD_BASE_RANGED = MetaAction("BUILD_BASE_RANGED") {
    val position = Vec2Int(0, 0)
    it.myBuilders.closest(position)?.produce(it, EntityType.MELEE_BASE, position) ?: mutableMapOf()
}


val BUILD_UNIT_MELEE = MetaAction("BUILD_UNIT_MELEE") {
    it.myMeleeBase?.produce(it, EntityType.MELEE_UNIT)
}

val BUILD_UNIT_RANGED = MetaAction("BUILD_UNIT_RANGED") {
    it.myRangedBase?.produce(it, EntityType.RANGED_UNIT)
}

val BUILD_HOUSE = MetaAction("BUILD_HOUSE") {
    it.myBuilders.choose(it)?.build(it, EntityType.HOUSE)
}

fun List<Entity>.choose(state: FieldState) = state.myHouseBuilder
    ?:closest(state.myBuilderBase?.position ?: state.my.middlePoint())

fun Entity.build(
    fieldState: FieldState,
    type: EntityType,
    position: Vec2Int = this.position
): MutableMap<Int, EntityAction> {
    val size = fieldState.properties(type).size
    return mutableMapOf(
        id to EntityAction(
            MoveAction(position.randomShift(3*size, 3*size).limitToMap(), true, true),
            BuildAction(type, position),
            null,
            null
        )
    )
}

private fun Vec2Int.randomShift(dx: Int, dy: Int) = Vec2Int(
    Random.nextInt(x - dx, x + dx),
    Random.nextInt(y - dy, y + dy)
)

private fun Vec2Int.limitToMap() = Vec2Int(
    x.coerceIn(0..globalSettings.mapSize),
    y.coerceIn(0..globalSettings.mapSize))

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

private fun Entity.produce(
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

class MetaAction(val name: String = "", val decoder: MetaAction.(FieldState) -> MutableMap<Int, EntityAction>?) {
    fun DecodeToAction(state: FieldState) = Action(decoder(state) ?: mutableMapOf())
    fun log(): MetaAction {
        println(this)
        return this
    }

    override fun toString() = name
}