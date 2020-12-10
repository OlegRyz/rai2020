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
        collect(it)
    }.also { res ->
        it.myTurrets.act { EntityAction(null,null, AttackAction(null, AutoAttack(50, arrayOf(
            EntityType.BUILDER_UNIT, EntityType.MELEE_UNIT, EntityType.RANGED_UNIT))), null
        ) }
    }
}

val ATTACK_ENEMY = MetaAction("ATTACK_ENEMY") {
    it.myFreeInfantry.attackClosestToYou(it.enemies)
}

val GEATHER_ARMY = MetaAction("GEATHER_ARMY") {
    it.myFreeInfantry.move((it.myBuildings.middlePoint() to globalSettings.center).transit(0.2))
}

fun Pair<Vec2Int, Vec2Int>.transit(share: Double) = Vec2Int(
    ((second.x - first.x) * share + first.x).toInt(),
    ((second.y - first.y) * share + first.y).toInt(),
)

val BUILD_UNIT_BUILDER = MetaAction("BUILD_UNIT_BUILDER") {
    it.myMeleeBase?.stopProduction()
    it.myRangedBase?.stopProduction()
    it.myBuilderBase?.produce(it, EntityType.BUILDER_UNIT)
}

val BUILD_BASE_RANGED = MetaAction("BUILD_BASE_RANGED") {
    val position = Vec2Int(0, 0)
    it.myBuilders.closest(position)?.produce(it, EntityType.MELEE_BASE, position) ?: mutableMapOf()
}


val BUILD_UNIT_MELEE = MetaAction("BUILD_UNIT_MELEE") {
    it.myBuilderBase?.stopProduction()
    it.myRangedBase?.stopProduction()
    it.myMeleeBase?.produce(it, EntityType.MELEE_UNIT)
}

val STOP_MAD_PRINTER = MetaAction("STOP_MAD_PRINTER") {
    it.myInfantry.choose(it, this)?.run {
        it.recordOrder(this, this@MetaAction)
        move(it.myBuilderBase?.gatePosition(it))
    }
}

val UNLEASH_MAD_PRINTER = MetaAction("UNLEASH_MAD_PRINTER") {
    it.myInfantry.choose(it, STOP_MAD_PRINTER)?.run {
        it.canceldOrder(this)
        move(this.position.shift(2,2))
    }
}

private fun Vec2Int.shift(x: Int, y: Int) = Vec2Int(this.x + x, this.y + y)

private fun Entity.gatePosition(fieldState: FieldState): Vec2Int {
    val size = fieldState.properties(this.entityType).size
    return Vec2Int(position.x + size, position.y + size - 1)
}

fun Entity.stopProduction() = mutableMapOf(
    id to EntityAction(
        null,
        BuildAction(EntityType.BUILDER_UNIT, Vec2Int(0,0)),
        null,
        null
    )
)


val BUILD_UNIT_RANGED = MetaAction("BUILD_UNIT_RANGED") {
    it.myBuilderBase?.stopProduction()
    it.myMeleeBase?.stopProduction()
    it.myRangedBase?.produce(it, EntityType.RANGED_UNIT)
}

val BUILD_HOUSE = MetaAction("BUILD_HOUSE") {
    val field = createField(it)
    val spot = field.findEmptySpot(it.properties(EntityType.HOUSE).size)
    if (spot != null) {
        it.myBuilders.closest(spot)?.build(it, EntityType.HOUSE, spot).also { result ->
            it.myFreeInfantry.gaters(it).move((it.myBuildings.middlePoint() to globalSettings.center).transit(0.2))
        }
    } else {
        mutableMapOf()
    }
}

private fun Array<BooleanArray>.findEmptySpot(spotSize: Int): Vec2Int? {
    return (0..100).map {
        Vec2Int(Random.nextInt(size - spotSize), Random.nextInt(size - spotSize))
    }.firstOrNull { vector ->
        val rows = this.slice(vector.y..(vector.y + spotSize))
        rows.all { it.slice(vector.x..(vector.x + spotSize)).all{!it}}
    }
}

fun createField(state: FieldState): Array<BooleanArray> {
    val start = 0
    val squareSize = 20
    val square = start..squareSize
    val entities = state.entities.filter {
        it.position.x in square && it.position.y in square
    }
    val field = Array(squareSize) { BooleanArray(squareSize) }

    entities.forEach {
        val entitySize = state.properties(it).size
        field.fillB(it.position.y, it.position.y + entitySize) { row -> row.fill( true, it.position.x,
            (it.position.x + entitySize).coerceAtMost(row.size))}
    }
    return field
}

private fun Array<BooleanArray>.fillB(fromIndex: Int, toIndex: Int, rowFiller: (BooleanArray)->Unit) {
    for(i in (fromIndex..toIndex.coerceAtMost(size - 1))) {
        rowFiller(this[i])
    }
}


val REPAIR_BUILDINGS = MetaAction("REPAIR_BUILDINGS") {
    it.myBuilders.choose(it, this)?.repair(it, it.myUnhealthyBuildings).also { result ->
        it.myFreeInfantry.gaters(it).move((it.myBuildings.middlePoint() to globalSettings.center).transit(0.2))
    }
}

private fun List<Entity>.gaters(fieldState: FieldState): List<Entity> {
    val gates = listOf(fieldState.myRangedBase?.position?.x, fieldState.myMeleeBase?.position?.x) to
    listOf(fieldState.myRangedBase?.position?.y, fieldState.myMeleeBase?.position?.y)
    return filter { it.position.x in gates.first && it.position.y in gates.second}

}

private fun Entity.repair(state: FieldState, entities: List<Entity>) = mutableMapOf(
    id to when (val closest = entities.closest(position)) {
        null -> EntityAction(
            null,
            null,
            null,
            null
        )
        else -> EntityAction(
            MoveAction(closest.position, true, true),
            null,
            null,
            RepairAction(closest.id)

        )
    }
)


fun List<Entity>.choose(state: FieldState, metaAction: MetaAction): Entity? {
    val id = state.ordersCache.find { it.order == metaAction }?.id
    val ordered = state.myEntityBy(id)
    return ordered ?: closest(state.myBuilderBase?.position ?: state.myBuilders.middlePoint())
}

fun Entity.build(
    fieldState: FieldState,
    type: EntityType,
    position: Vec2Int = this.position
) = id.build(fieldState, type, position)

fun Int.build(
    fieldState: FieldState,
    type: EntityType,
    position: Vec2Int
): MutableMap<Int, EntityAction> {
    val size = fieldState.properties(type).size
    return mutableMapOf(
        this to EntityAction(
            MoveAction(position.randomShift(3 * size, 3 * size).limitToMap(), true, true),
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
    y.coerceIn(0..globalSettings.mapSize)
)

fun List<Entity>.closest(position: Vec2Int) = minByOrNull { distance(it.position, position) }

private fun List<Entity>.attackClosestToYou(targets: List<Entity>) = act {
    val closest = targets.closest(it.position)
    when (closest) {
        null -> EntityAction(null, null, null, null)
        else -> EntityAction(
            MoveAction(closest.position, true, true),
            null,
            AttackAction(closest.id, null),
            null
        )
    }
}

private fun List<Entity>.move(point: Vec2Int) = act {
    EntityAction(
        MoveAction(point, true, true),
        null,
        AttackAction(null, AutoAttack(10, arrayOf(EntityType.MELEE_UNIT, EntityType.RANGED_UNIT))),
        null)
}

private fun Entity.move(point: Vec2Int?) = mutableMapOf(
    id to when (point) {
        null -> EntityAction()
        else -> EntityAction(
            MoveAction(point, true, true),
            null,
            AttackAction(null, AutoAttack(10, arrayOf(EntityType.MELEE_UNIT, EntityType.RANGED_UNIT))),
            null
        )
    }
)

fun List<Entity>.middlePoint(): Vec2Int {
    val x = this.map { it.position.x }.average()
    val y = this.map { it.position.y }.average()
    return Vec2Int(x.toInt(), y.toInt())
}

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
    val gatePosition = Vec2Int(position.x + size, position.y + size - 1)
    return mutableMapOf(
        id to EntityAction(
            null,
            BuildAction(type, gatePosition),
            null,
            null
        )
    )
}

private fun List<Entity>.collect(fieldState: FieldState) = act {
    val target = fieldState.resources.randomInRadius(100, it.position) ?:
        fieldState.resources.randomInRadius(150, it.position) ?:
        fieldState.resources.randomInRadius(300, it.position)
    when (val closest = target?.position) {
        null -> EntityAction(null, null, null, null)
        else -> EntityAction(
            MoveAction(closest, true, true),
            null,
            AttackAction(null, AutoAttack(10, arrayOf(EntityType.RESOURCE))),
            null
        )
    }

}

fun List<Entity>.allInRadius(radius:Int, center: Vec2Int) = filter { distance(it.position, center) < radius }

fun List<Entity>.randomInRadius(radius:Int, center: Vec2Int) = allInRadius(radius, center).randomOrNull()

class MetaAction(val name: String = "", val decoder: MetaAction.(FieldState) -> MutableMap<Int, EntityAction>?) {
    fun DecodeToAction(state: FieldState) = Action(decoder(state) ?: mutableMapOf())
    fun log(): MetaAction {
        println(this)
        return this
    }

    override fun toString() = name
}