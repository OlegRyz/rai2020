package org.niksi.rai.controller

import model.*
import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random

val DO_NOTHING = MetaAction("DO_NOTHING") {
    mutableMapOf()
}

val COLLECT_RESOURCES = MetaAction("COLLECT_RESOURCES") {
    it.myFreeBuilders.run {
        collect(it)
    }
}

val ACTIVATE_TURRETS = MetaAction("Activate") {
    it.myTurrets.act {
        EntityAction(
            null, null, AttackAction(null, AutoAttack(50, arrayOf(
                        EntityType.BUILDER_UNIT, EntityType.MELEE_UNIT, EntityType.RANGED_UNIT
                    )
                )
            ), null
        )
    }
}

val ATTACK_ENEMY = MetaAction("ATTACK_ENEMY") {
    it.myFreeInfantry.run {
        it.recordOrder(this, this@MetaAction)
        attackClosestToYou(it, it.enemies)
    }
}

val ATTACK_NEIGHBOR = MetaAction("ATTACK_NEIGHBOR") {
    val target = Vec2Int(5, globalSettings.mapSize - 25)
    val portion = it.myFreeInfantry.count() / 2
    it.myFreeInfantry
        .sortedBy { warrior -> distance(warrior.position, target) }
        .take(portion)
        .move(target)
}

val RUN_AWAY_BUILDERS = MetaAction("RUN_AWAY_BUILDERS") {
    it.myBuilders.near(it.enemies, 6).act { surrender ->
        val closest = it.enemyInfantry.closest(surrender.position)
        if  (closest != null) {
            surrender.moveAsap(
                Vec2Int(
                    (surrender.position.x + (surrender.position.x - closest.position.x).sign),
                    surrender.position.y + (surrender.position.y - closest.position.y).sign
                )
            )
        } else {
            EntityAction()
        }
    }
}

val CLEANUP_ORDERS = MetaAction("CLEANUP_ORDERS") {
    val attackers = it.myEntityBy(it.ordersCache.getId(ATTACK_ENEMY))
    attackers.near(it.enemies, 20).run {
        attackers.forEach { previous ->
            it.canceldOrder(previous)
        }
        it.recordOrder(this, ATTACK_ENEMY)
        attackClosestToYou(it, it.enemies)
    }
}

val CLEANUP_GATE = MetaAction("CLEANUP_GATE") {
    val gate = it.myRangedBase?.gatePosition(it)
    if (gate == null) {
        mutableMapOf()
    } else {
        it.myInfantry.firstOrNull { warior -> warior.position.isSame(gate) }?.move(Vec2Int(18, 18))
    }
}

private fun Vec2Int.isSame(position: Vec2Int) = this.x == position.x && this.y == position.y


val DEFEND_BUILDINGS = MetaAction("DEFEND_BUILDINGS") {
    val targets = it.enemyInfantry.near(it.myBuildings, 20)
    if (targets.isEmpty()) {
        mutableMapOf()
    } else {
        it.myInfantry.near(it.myBuildings, 150).attackClosestToClosestDefendable(it, targets, it.myBuildings)
    }
}

fun List<Entity>.near(targets: List<Entity>, range: Int) =
    filter { origin ->
        targets.any {
                target -> distance(target.position, origin.position)< range
        }
    }


val GEATHER_ARMY = MetaAction("GEATHER_ARMY") {
    it.myFreeInfantry.move(Vec2Int(18,18))
}

fun Pair<Vec2Int, Vec2Int>.transit(share: Double) = Vec2Int(
    ((second.x - first.x) * share + first.x).toInt(),
    ((second.y - first.y) * share + first.y).toInt(),
)

val BUILD_UNIT_BUILDER = MetaAction("BUILD_UNIT_BUILDER") {
    it.myBuilderBase?.produce(it, EntityType.BUILDER_UNIT)
}

val BUILD_BASE_RANGED = MetaAction("BUILD_BASE_RANGED") {
    val position = Vec2Int(0, 0)
    it.myBuilders.closest(position)?.produce(it, EntityType.RANGED_BASE, position) ?: mutableMapOf()
}


val BUILD_UNIT_MELEE = MetaAction("BUILD_UNIT_MELEE") {
    it.myBuilderBase?.stopProduction()
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
        null,
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
    val spot = findEmptySpot(it.properties(EntityType.HOUSE).size, it)

    if (spot != null) {
        val  builderSpot = Vec2Int(spot.x + 3, spot.y + 2)
        it
            .myBuilders
            .closest(builderSpot)
            ?.also { builder ->
                println("Spot = ${builderSpot.toStr()}; ${builder.position.toStr()}; Id: ${builder.id}")
                it.recordOrder(builder, this)
            }
            ?.build(it, EntityType.HOUSE, builderSpot)
            .also { result ->
            it.myFreeInfantry.gaters(it).move((it.myBuildings.middlePoint() to globalSettings.center).transit(0.2))
        }
    } else {
        mutableMapOf()
    }
}

private fun Vec2Int.toStr() = "($x , $y)"

val SpotChoice = listOf(
    List(10){it -> Vec2Int(0,it * 3)},
    List(9){it -> Vec2Int(it * 3 + 4, 0)},
    List(9){it -> Vec2Int(it * 4 + 4, 11)},
    listOf(Vec2Int(11,4),Vec2Int(11,7), Vec2Int(11, 19), Vec2Int(11, 15)),
    List(7){it -> Vec2Int(it * 5 + 5, 24)},
    List(7){it -> Vec2Int(24,it * 5 + 5)},
).flatten()

private fun findEmptySpot(spotSize: Int, state: FieldState): Vec2Int? {
    return SpotChoice.firstOrNull { available(it, state, spotSize) }
}

fun available(spot: Vec2Int, state: FieldState, size: Int) =
    state.entities.all { notIntersect(spot, it.position, size, state.properties(it).size) }

fun notIntersect(a: Vec2Int, b: Vec2Int, sizeA: Int, sizeB: Int) =
    a.x + sizeA - 1 < b.x ||
            b.x + sizeB - 1 < a.x ||
            a.y + sizeA - 1 < b.y ||
            b.y + sizeB - 1 < a.y

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


val REPAIR_BUILDINGS = MetaAction("REPAIR_BUILDINGS") { state ->
    state
        .myBuilders
        .choose(state, this, state.myUnhealthyBuildings.firstOrNull()?.position)
        ?.also {
            state.recordOrder(it, this@MetaAction)
        }
        ?.repair(state, state.myUnhealthyBuildings)

        .run {
            state
                .myFreeInfantry
                .gaters(state)
                .move((state.myBuildings.middlePoint() to globalSettings.center).transit(0.2))
        }
}

val REPAIR_BUILDINGS_ALL = MetaAction("REPAIR_BUILDINGS_ALL") { state ->
    val buildingsNumber =  state.myUnhealthyBuildings.count()
    val portion = (state.myBuilders.size / 2).coerceAtMost(buildingsNumber * 3) / buildingsNumber
    if (buildingsNumber > 0) {
        state
            .myUnhealthyBuildings
            .sortedBy { it.health }
            .foldIndexed(mutableMapOf()) { i, acc, building ->

                state.myBuilders
                    .sortedBy { distance(it.position, building.position) }
                    .take(portion * (buildingsNumber - i))
                    .also { state.recordOrder(it, this) }
                    .repair(state, building, acc)
            }
    } else {
        mutableMapOf()
    }
}


private fun List<Entity>.gaters(fieldState: FieldState): List<Entity> {
    val gates = listOf(fieldState.myRangedBase?.position?.x, fieldState.myMeleeBase?.position?.x) to
    listOf(fieldState.myRangedBase?.position?.y, fieldState.myMeleeBase?.position?.y)
    return filter { it.position.x in gates.first && it.position.y in gates.second}

}
private fun List<Entity>.repair(state: FieldState, building: Entity, acc: MutableMap<Int, EntityAction>): MutableMap<Int, EntityAction> {
    acc.putAll(map {
        it.id to
                EntityAction(
                    MoveAction(building.position, true, true),
                    null,
                    null,
                    RepairAction(building.id)
                )
    })
    return acc
}

private fun Entity.repair(state: FieldState, entities: List<Entity>) = mutableMapOf(
    id to when (val closest = entities.closest(position)) {
        null -> EntityAction(
            null,
            null,
            null,
            null
        )
        else -> {
            println("RPEAIR: ${closest.position.toStr()} ${closest.id}")
            EntityAction(
                MoveAction(closest.position.coerce(globalSettings.mapSize), true, true),
                null,
                null,
                RepairAction(closest.id)

            )
        }
    }
)


fun List<Entity>.choose(state: FieldState,
                        metaAction: MetaAction,
                        position: Vec2Int? = null
): Entity? {
    val id = state.ordersCache.getId(metaAction).firstOrNull()
    val ordered = state.myEntityBy(id)
    return ordered ?: closest(position ?: state.myBuilderBase?.position ?: state.myBuilders.middlePoint())
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
            MoveAction(position.limitToMap(), false, true),
            BuildAction(type, Vec2Int(position.x - size, position.y - size + 1).coerce(globalSettings.mapSize)),
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

fun List<Entity>.closest(position: Vec2Int?) = if (position != null) {
    minByOrNull { manhDistance(it.position, position) }
} else {
    null
}

private fun List<Entity>.attackClosestToYou(fieldState: FieldState, targets: List<Entity>) = act {
    val closest = targets.closest(it.position)
    when (closest) {
        null -> EntityAction(null, null, null, null)
        else -> {
            val distance = fieldState.properties(it.entityType).attack?.attackRange ?: 0
            var position = (closest.position to it.position).transitToDistance(distance)
            if (fieldState.myInfantry.any { it.position.isSame(position) }) {
                position = position.randomShift(1,1)
            }
            EntityAction(
                MoveAction(position.coerce(globalSettings.mapSize), true, true),
                null,
                AttackAction(closest.id, null),
                null
            )
        }
    }
}

private fun List<Entity>.attackClosestToClosestDefendable(fieldState: FieldState, targets: List<Entity>, defendable: List<Entity>) = act {
    val closest = targets.closest(defendable.closest(it.position)?.position)
    when (closest) {
        null -> EntityAction(null, null, null, null)
        else -> {
            val distance = fieldState.properties(it.entityType).attack?.attackRange ?: 0
            val position = (closest.position to it.position).transitToDistance(distance)
            EntityAction(
                MoveAction(position.coerce(globalSettings.mapSize), true, true),
                null,
                AttackAction(closest.id, null),
                null
            )
        }
    }
}

private fun Pair<Vec2Int, Vec2Int>.transitToDistance(expDistance: Int):Vec2Int {
    val curDistance = distance(first, second)
    return if (curDistance == 0) {
        Vec2Int(first.x, first.y + expDistance)
    } else {
        val dx = expDistance * (second.x - first.x) / curDistance
        val dy = expDistance * (second.y - first.y) / curDistance

        Vec2Int((first.x + dx).toInt(), (first.y + dy).toInt())
    }
}

private fun List<Entity>.move(point: Vec2Int) = act {
    EntityAction(
        MoveAction(point.coerce(globalSettings.mapSize), true, true),
        null,
        AttackAction(null, AutoAttack(10, arrayOf(EntityType.MELEE_UNIT, EntityType.RANGED_UNIT))),
        null)
}

private fun Vec2Int.coerce(mapSize: Int): Vec2Int = Vec2Int(this.x.coerceIn(0..mapSize), this.y.coerceIn(0..mapSize))

private fun Entity.move(point: Vec2Int?) = mutableMapOf(
    id to when (point) {
        null -> EntityAction()
        else -> EntityAction(
            MoveAction(point.coerce(globalSettings.mapSize), true, true),
            null,
            AttackAction(null, AutoAttack(10, arrayOf(EntityType.MELEE_UNIT, EntityType.RANGED_UNIT))),
            null
        )
    }
)

private fun Entity.moveAsap(point: Vec2Int) =  EntityAction(
            MoveAction(point.coerce(globalSettings.mapSize), false, false),
            null,
            null,
    null
        )


fun List<Entity>.middlePoint(): Vec2Int {
    val x = this.map { it.position.x }.average()
    val y = this.map { it.position.y }.average()
    return Vec2Int(x.toInt(), y.toInt())
}

//fun distance(position: Vec2Int, target: Vec2Int) = Point2D.distance(position.x.toDouble(),
//    position.y.toDouble(), target.x.toDouble(), target.y.toDouble())
fun distance(position: Vec2Int, target: Vec2Int) = manhDistance(position.x, position.y, target.x, target.y)
fun manhDistance(position: Vec2Int, target: Vec2Int) = manhDistance(position.x, position.y, target.x, target.y)

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
            BuildAction(type, gatePosition.coerce(globalSettings.mapSize)),
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
            MoveAction(closest.coerce(globalSettings.mapSize), true, true),
            null,
            AttackAction(null, AutoAttack(10, arrayOf(EntityType.RESOURCE))),
            null
        )
    }

}

fun List<Entity>.allInRadius(radius:Int, center: Vec2Int) = filter { distance(it.position, center) < radius }

fun List<Entity>.randomInRadius(radius:Int, center: Vec2Int) = allInRadius(radius, center).randomOrNull()

class MetaAction(val name: String = "", val decoder: MetaAction.(FieldState) -> MutableMap<Int, EntityAction>?) {
    fun DecodeToAction(state: FieldState) = decoder(state) ?: mutableMapOf()


    override fun toString() = name
    fun isSame(metaAction: MetaAction) = name == metaAction.name
}