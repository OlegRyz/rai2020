package org.niksi.rai.controller

import model.*
import org.niksi.rai.langpack.*
import kotlin.math.abs
import kotlin.math.sign

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

val ATTACK_NEIGHBOR = MetaAction("ATTACK_NEIGHBOR") { state->
    val target = Vec2Int(5, globalSettings.mapSize - 5)
    val portion = state.myFreeInfantry.count() / 2
    state.myFreeInfantry
        .sortedBy { warrior -> distance(warrior.position, target) }
        .take(portion)
        .apply {
            state.recordOrder(this, this@MetaAction)
        }
        .move(target)
}

val ATTACK_NEIGHBOR_CLEANUP = MetaAction("ATTACK_NEIGHBOR_CLEANUP") { state ->
    state
        .myInfantry
        .inZone(0,50,globalSettings.mapSize - 50, globalSettings.mapSize)
        .forEach { state.canceldOrderIf(it, ATTACK_NEIGHBOR) }

    state
        .myInfantry
        .inZone(globalSettings.mapSize - 50,globalSettings.mapSize,globalSettings.mapSize - 50, globalSettings.mapSize)
        .forEach { state.canceldOrderIf(it, ATTACK_DIAGONAL) }
    state
        .myInfantry
        .inZone(globalSettings.mapSize - 50,globalSettings.mapSize,0, 50)
        .forEach { state.canceldOrderIf(it, ATTACK_DIAGONAL) }
    mutableMapOf()
}

fun List<Entity>.inZone(x1: Int, x2: Int, y1: Int, y2: Int) = filter {it.position.x in x1..x2 && it.position.y in y1..y2}

val ATTACK_DIAGONAL = MetaAction("ATTACK_NEIGHBOR") {
    val target = Vec2Int(globalSettings.mapSize, globalSettings.mapSize)
    val portion = it.myFreeInfantry.count() / 2

    val lead = it.myFreeInfantry.closest(target)
    if (lead != null) {

        it.myFreeInfantry
            .sortedBy { warrior -> distance(warrior.position, lead.position) }
            .take(portion)
            .move(target)
    } else {
        mutableMapOf()
    }
}

val ATTACK_NEIGHBOR_RIGHT = MetaAction("ATTACK_NEIGHBOR_RIGHT") {state ->
    val target = Vec2Int(globalSettings.mapSize - 5, 5)
    val portion = state.myFreeInfantry.count() / 2
    state.myFreeInfantry
        .sortedBy { warrior -> distance(warrior.position, target) }
        .take(portion)
        .apply {
            state.recordOrder(this, this@MetaAction)
        }
        .move(target)
}

val DEFENSIVE_WALL_RIGHT = MetaAction("FORMATION") { state ->
    val center = state.enemyInfantry.inZone(25, 40, 0, 40).middlePoint()
    val head = if (center.y == 0) {
        Vec2Int(25, 12)
    } else {
        Vec2Int(25, center.y - 4)
    }
    val form = List(8) {
        Vec2Int(0, it)
    }
    makeFormation(state, form, head)
}

fun MetaAction.makeFormation(
    state: FieldState,
    form: List<Vec2Int>,
    head: Vec2Int
): MutableMap<Int, EntityAction> {
    val units = state.ordersCache.getEntities(this, state.myInfantry)
        .refill(form.size, state.myFreeInfantry, head, state, this)

    return units
        .zip(form) { entity, position ->
            entity.id to entity.move(head.shift(position), attackRange = 0)
        }
        .toMap()
        .toMutableMap()
}

fun List<Entity>.refill(
    requiredNumber: Int,
    reserve: List<Entity>,
    head: Vec2Int,
    state: FieldState,
    metaAction: MetaAction,
) = let {
    if (it.count() < requiredNumber) {
        val refill = reserve.closest(head, requiredNumber - it.count())
        state.ordersCache.record(refill, metaAction)
        it.plus(refill)
    } else {
        it
    }
}


val SNAKE = MetaAction("SNAKE") { state ->
    val head = Vec2Int(18, 20)

    makeFormation(state, List(5) { Vec2Int(0, -it) }, head)
}
val SNAKE_MOVE = MetaAction("SNAKE_MOVE") { state ->
    val snakeUnits = state.ordersCache.getEntities(SNAKE, state.myInfantry).
            plus(state.ordersCache.getEntities(this, state.myInfantry))

    state.recordOrder(snakeUnits, this)
    snakeUnits.map{
        it.id to it.moveOneStep(0, 1)
    }
        .toMap()
        .toMutableMap()
}

private fun Entity.moveOneStep(x: Int, y: Int): EntityAction =
    EntityAction(
        MoveAction(position.shift(x, y).limitToMap(), false, true),
        null,
        null,
        null
    )

val RUN_AWAY_BUILDERS = MetaAction("RUN_AWAY_BUILDERS") {
    it.myBuilders.near(it.enemyInfantry, 8)
        .plus(it.myBuilders.near(it.enemyTurrets, it.properties(EntityType.TURRET).attack?.attackRange?:8))
        .act { surrender ->
            val closest = it.enemyInfantry.closest(surrender.position)
            if (closest != null) {
                surrender.retreatFrom(closest)
            } else {
                null
            }
        }
}

val BUILDERS_ATTACK_ENEMY_BUILDERS = MetaAction("ATTACK_ENEMY") { state ->
    state.myBuilders.near(state.enemyBuilders, 4)
        .run {
            state.recordOrder(this, this@MetaAction)
            attackClosestToYou(state, state.enemyBuilders)
        }
}


fun Entity.retreatFrom(enemy: Entity) = retreatFrom(enemy.position)
fun Entity.retreatFrom(enemyPosition: Vec2Int) = moveAsap(
    Vec2Int(
        (this.position.x + (this.position.x - enemyPosition.x).sign),
        this.position.y + (this.position.y - enemyPosition.y).sign
    )
)

val RETREAT_RANGED_UNITS = MetaAction("RETREAT_RANGED_UNITS") {
    it.myRanged
        .near(it.enemies, 6)
        .map { surrender ->
            val closeEnemy = it.enemyInfantry.allInRadius(7, surrender.position)
            val closeMy = it.myInfantry.allInRadius(7, surrender.position)
            if (closeEnemy.sumBy { it.health } > closeMy.sumBy { it.health }) {
                it.canceldOrder(surrender)
                surrender.id to surrender.retreatFrom(closeEnemy.middlePoint())
            } else {
                val action = surrender.attackClosestToYou(it, it.enemies)
                if (action != null) {
                    surrender.id to action
                } else {
                    null
                }
            }
        }
        .filterNotNull()
        .toMap()
        .toMutableMap()
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

val CLEANUP_GATE = MetaAction("CLEANUP_GATE") { state ->
//    state.myInfantry.gaters(state).move((state.myBuildings.middlePoint() to globalSettings.center).transit(0.2))
    val actions = listOf(moveFrom(state.myRangedBase?.gatePosition(state), state),
        moveFrom(state.myMeleeBase?.gatePosition(state), state))
        .filterNotNull()
    actions
        .toMap()
        .toMutableMap()
}

private fun moveFrom(
    gate: Vec2Int?,
    it: FieldState
) = if (gate == null) {
    null
} else {
    it.myInfantry
        .firstOrNull { warior -> warior.position.isSame(gate) }
    ?.let {
        it.id to it.move(Vec2Int(18, 18))
    }
}

val DEFEND_BUILDINGS = MetaAction("DEFEND_BUILDINGS") { state->
    val targets = state.enemyInfantry.near(state.myBuildings, 20)
    if (targets.isEmpty()) {
        mutableMapOf()
    } else {
        state.myInfantry.near(state.myBuildings, 150)
            .sortedBy {
                distance(it.position, targets.closest(it.position)?.position ?: it.position) }
            .take(targets.count() * 2)
            .run{
            forEach { state.canceldOrder(it) }
            attackClosestToClosestDefendable(state, targets, state.myBuildings)
        }
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
    if (it.me.resource >= it.properties(EntityType.BUILDER_UNIT).initialCost) {
        println("build builder")
        it.myBuilderBase?.produce(it, EntityType.BUILDER_UNIT)
    } else {
        println("not enough resource to build builder ${it.me.resource}")
        useOtherwiseAction(it)
    }
}.doAlwaysWhenNotChosen {
    it.myBuilderBase.stopProduction()
}

val BUILD_BASE_RANGED = MetaAction("BUILD_BASE_RANGED") {
    build(it, EntityType.RANGED_BASE)
}


val BUILD_UNIT_MELEE = MetaAction("BUILD_UNIT_MELEE") {
    if (it.me.resource >= it.properties(EntityType.MELEE_UNIT).initialCost) {
        it.myMeleeBase?.produce(it, EntityType.MELEE_UNIT)
    } else {
        useOtherwiseAction(it)
    }
}.doAlwaysWhenNotChosen {
    it.myMeleeBase.stopProduction()
}

val BUILD_UNIT_RANGED = MetaAction("BUILD_UNIT_RANGED") {
    if (it.me.resource >= it.properties(EntityType.RANGED_UNIT).initialCost) {
        println("build ranged")
        it.myRangedBase?.produce(it, EntityType.RANGED_UNIT)
    } else {
        println("not enough resource to build ranged ${it.me.resource}")
        useOtherwiseAction(it)
    }
}.doAlwaysWhenNotChosen {
    it.myRangedBase.stopProduction()
}

val BUILD_HOUSE = MetaAction("BUILD_HOUSE") {
    build(it, EntityType.HOUSE)
}

fun MetaAction.build(it: FieldState, buildingType: EntityType): MutableMap<Int, EntityAction>? {
    val spotSize = it.properties(buildingType).size
    val spot = findEmptySpot(spotSize, it, buildingType)

    return if (spot != null) {
        if (buildingType == EntityType.RANGED_BASE) {
            println("build ranged")
        }
        it
            .myBuilders
            .closest(spot.shift(2,2))
            ?.also { builder ->
                println("Spot = ${spot.toStr()}; ${builder.position.toStr()}; Id: ${builder.id}")
                it.recordOrder(builder, this)
            }
            ?.build(it, buildingType, spot)
    } else {
        mutableMapOf()
    }
}

val HouseSpots = listOf(
    List(6){it -> Vec2Int(0,it * 3)},
    List(6){it -> Vec2Int(it * 3 + 11, 0)},
    List(6){it -> Vec2Int(it * 4 + 4, 11)},
    listOf(Vec2Int(11,4),Vec2Int(11,7), Vec2Int(11, 19), Vec2Int(11, 15)),
    List(6){it -> Vec2Int(it * 5 + 5, 24)},
    List(6){it -> Vec2Int(24,it * 5 + 5)},
    List(6){it -> Vec2Int(0,it * 3 + 15)},
    List(6){it -> Vec2Int(it * 3 + 26, 0)},
).flatten()

val RangedBaseSpots = List(3) { x -> List(3) { y -> Vec2Int(4 + x, 0 + y) }}.flatten()

fun findEmptySpot(spotSize: Int, state: FieldState, type: EntityType) = when(type) {
    EntityType.RANGED_BASE -> RangedBaseSpots.firstOrNull { available(it, state, spotSize)}
    else -> HouseSpots.firstOrNull { available(it, state, spotSize) }
}

fun available(spot: Vec2Int, state: FieldState, size: Int) =
    state.entities.all { notIntersect(spot, it.position, size, state.properties(it).size) }

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

val REPAIR_BUILDINGS_ALL = MetaAction("REPAIR_BUILDINGS_ALL") { state ->
    val buildingsNumber =  state.myUnhealthyBuildings.count()
    val workersNeeded = state.myUnhealthyBuildings.sumBy { state.properties(it).size }
    val portion = (state.myBuilders.size / 2).coerceAtMost(workersNeeded) / buildingsNumber
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


private fun List<Entity>.repair(state: FieldState, building: Entity, acc: MutableMap<Int, EntityAction>): MutableMap<Int, EntityAction> {
    acc.putAll(map {
        it.id to
                EntityAction(
                    MoveAction(building.position.limitToMap(), true, true),
                    null,
                    null,
                    RepairAction(building.id)
                )
    })
    return acc
}

fun List<Entity>.choose(state: FieldState,
                        metaAction: MetaAction,
                        position: Vec2Int? = null
): Entity? {
    val id = state.ordersCache.getId(metaAction).firstOrNull()
    val ordered = state.myEntityBy(id)
    return ordered ?: closest(position ?: state.myBuilderBase?.position ?: state.myBuilders.middlePoint())
}

fun List<Entity>.closest(position: Vec2Int?) = if (position != null) {
    minByOrNull { manhDistance(it.position, position) }
} else {
    null
}

fun List<Entity>.closest(position: Vec2Int?, n: Int) = if (position != null) {
    sortedBy { manhDistance(it.position, position) }.take(n)
} else {
    listOf()
}

private fun List<Entity>.attackClosestToYou(fieldState: FieldState, targets: List<Entity>) = act {
    it.attackClosestToYou(fieldState, targets)
}

private fun List<Entity>.attackClosestToClosestDefendable(fieldState: FieldState, targets: List<Entity>, defendable: List<Entity>) = act {
    val closest = targets.closest(defendable.closest(it.position)?.position)
    when (closest) {
        null -> null
        else -> {
            val distance = fieldState.properties(it.entityType).attack?.attackRange ?: 0
            val position = (closest.position to it.position).transitToDistance(distance)
            EntityAction(
                MoveAction(position.limitToMap(), true, true),
                null,
                AttackAction(closest.id, null),
                null
            )
        }
    }
}

fun List<Entity>.move(point: Vec2Int, attackRange:Int = 10) = act {
    EntityAction(
        MoveAction(point.limitToMap(), true, true),
        null,
        AttackAction(null, autoAttack(attackRange)),
        null)
}

fun autoAttack(attackRange: Int) =
    AutoAttack(attackRange, arrayOf(
        EntityType.WALL,
        EntityType.HOUSE,
        EntityType.BUILDER_BASE,
        EntityType.BUILDER_UNIT,
        EntityType.MELEE_BASE,
        EntityType.MELEE_UNIT,
        EntityType.RANGED_BASE,
        EntityType.RANGED_UNIT,
        EntityType.TURRET)
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

fun List<Entity>.act(action: (Entity) -> EntityAction?): MutableMap<Int, EntityAction> {
    return map {
        val res = action(it)
        if (res == null) {
            null
        } else {
            it.id to res
        }
    }
        .filterNotNull()
        .toMap()
        .toMutableMap()

}
fun List<Entity>.collect(fieldState: FieldState) = act {
    val target = fieldState.resources.randomInRadius(100, it.position) ?:
        fieldState.resources.randomInRadius(150, it.position) ?:
        fieldState.resources.randomInRadius(300, it.position)
    when (val closest = target?.position) {
        null -> null
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

data class MetaAction(val name: String = "", val decoder: MetaAction.(FieldState) -> MutableMap<Int, EntityAction>?) {
    var opposite: MetaAction? = null
    fun DecodeToAction(state: FieldState) = decoder(state) ?: mutableMapOf()


    override fun toString() = name
    fun isSame(metaAction: MetaAction) = name == metaAction.name
    fun doAlwaysWhenNotChosen(decoder: MetaAction.(FieldState) -> MutableMap<Int, EntityAction>?): MetaAction {
        opposite = MetaAction("NOT_TO_$name", decoder)
        return this
    }

    fun useOtherwiseAction(state: FieldState): MutableMap<Int, EntityAction>? {
        val action = opposite
        return if (action != null) {
            action.decoder(action, state)
        } else {
            mutableMapOf()
        }
    }
}