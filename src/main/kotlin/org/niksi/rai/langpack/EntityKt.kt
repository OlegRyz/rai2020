package org.niksi.rai.langpack

import model.*
import org.niksi.rai.controller.*

fun Entity?.stopProduction() = when(this) {
    null -> mutableMapOf()
    else ->mutableMapOf(
        id to EntityAction(
            null,
            null,
            null,
            null
        )
    )
}

fun Entity.gatePosition(fieldState: FieldState): Vec2Int {
    val size = fieldState.properties(this.entityType).size
    return Vec2Int(position.x + size, position.y + size - 1)
}

fun Entity.repair(state: FieldState, entities: List<Entity>) = mutableMapOf(
    id to when (val closest = entities.closest(position)) {
        null -> EntityAction(
            null,
            null,
            null,
            null
        )
        else -> {
            EntityAction(
                MoveAction(closest.position.coerce(globalSettings.mapSize), true, true),
                null,
                null,
                RepairAction(closest.id)

            )
        }
    }
)

fun Entity.moveAsap(point: Vec2Int) =  EntityAction(
    MoveAction(point.coerce(globalSettings.mapSize), false, false),
    null,
    null,
    null
)

fun Entity.move(point: Vec2Int?) = mutableMapOf(
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

fun Entity.attackClosestToYou(fieldState: FieldState, targets: List<Entity>): EntityAction {
    val closest = targets.closest(this.position)
    return when (closest) {
        null -> EntityAction(null, null, null, null)
        else -> {
            val distance = fieldState.properties(this.entityType).attack?.attackRange ?: 0
            var position = (closest.position to this.position).transitToDistance(distance)
            if (fieldState.myInfantry.any { it.position.isSame(position) }) {
                position = position.randomShift(1, 1)
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

fun Entity.produce(
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