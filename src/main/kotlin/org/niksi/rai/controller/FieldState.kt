package org.niksi.rai.controller

import model.Entity
import model.EntityProperties
import model.EntityType
import model.EntityType.*
import model.Player

class FieldState(entities: Array<Entity>, val entityProperties: MutableMap<EntityType, EntityProperties>, players: Array<Player>, myId: Int) {
    fun properties(entity: Entity) = entityProperties[entity.entityType]!!
    fun simulate(metaAction: MetaAction) = ImaginaryState(this, metaAction)

    val resources = entities.filterType(RESOURCE)
    val nonResources = entities.filterNotType(RESOURCE)

    val my = nonResources.filterPlayerId(myId)
    val myBuilders = my.filterType(BUILDER_UNIT)
    val myMelee = my.filterType(MELEE_UNIT)
    val myRanged = my.filterType(RANGED_UNIT)
    val myInfantry = listOf(myMelee, myRanged).flatten()

    val myBuilderBase = my.first{ it.entityType == BUILDER_BASE}
    val myMeleeBase = my.first{ it.entityType == MELEE_BASE}
    val myRangedBase = my.first{ it.entityType == RANGED_BASE}

    val enemies = nonResources.filterNotPlayerId(myId)
    val enemyBuilders = enemies.filterType(BUILDER_UNIT)
    val enemyMelee = enemies.filterType(MELEE_UNIT)
    val enemyRanged = enemies.filterType(RANGED_UNIT)
    val enemyInfantry = listOf(enemyMelee, enemyRanged).flatten()
    val enemyUnits = listOf(enemyMelee, enemyRanged, enemyBuilders).flatten()
}

private fun List<Entity>.filterPlayerId(playerId: Int) = filter { it.playerId == playerId}
private fun List<Entity>.filterNotPlayerId(playerId: Int) = filterNot { it.playerId == playerId}

fun Array<Entity>.filterType(entityType: EntityType) = filter { it.entityType == entityType }
fun Array<Entity>.filterNotType(entityType: EntityType) = filterNot { it.entityType == entityType }

fun List<Entity>.filterType(entityType: EntityType) = filter { it.entityType == entityType }