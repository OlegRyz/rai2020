package org.niksi.rai.controller

import model.Entity
import model.EntityProperties
import model.EntityType
import model.EntityType.*
import model.Player

class FieldState(entities: Array<Entity>, entityProperties: MutableMap<EntityType, EntityProperties>, players: Array<Player>, myId: Int) {
    val resources = entities.filterType(RESOURCE)
    val nonResources = entities.filterNotType(RESOURCE)
    val my = nonResources.filterPlayerId(myId)
    val myBuilders = my.filterType(BUILDER_UNIT)
    val myMelee = my.filterType(MELEE_UNIT)
    val myRanged = my.filterType(RANGED_UNIT)
    val enemies = nonResources.filterNotPlayerId(myId)
}

private fun List<Entity>.filterPlayerId(playerId: Int) = filter { it.playerId == playerId}
private fun List<Entity>.filterNotPlayerId(playerId: Int) = filterNot { it.playerId == playerId}

fun Array<Entity>.filterType(entityType: EntityType) = filter { it.entityType == entityType }
fun Array<Entity>.filterNotType(entityType: EntityType) = filterNot { it.entityType == entityType }

fun List<Entity>.filterType(entityType: EntityType) = filter { it.entityType == entityType }
fun List<Entity>.filterNotType(entityType: EntityType) = filterNot { it.entityType == entityType }