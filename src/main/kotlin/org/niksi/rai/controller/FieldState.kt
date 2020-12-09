package org.niksi.rai.controller

import model.Entity
import model.EntityProperties
import model.EntityType
import model.EntityType.*
import model.Player

class FieldState(
    entities: Array<Entity>,
    val entityProperties: MutableMap<EntityType, EntityProperties>,
    players: Array<Player>,
    myId: Int,
    val ordersCache: OrdersCache
) {
    fun properties(entity: Entity) = entityProperties[entity.entityType]!!
    fun properties(entityType: EntityType) = entityProperties[entityType]!!
    fun simulate(metaAction: MetaAction) = ImaginaryState(this, metaAction)
    fun recordOrder(entities: List<Entity>) {
        ordersCache.addAll(entities.map { it.id })
    }

    val resources = entities.filterType(RESOURCE)
    val nonResources = entities.filterNotType(RESOURCE)

    val my = nonResources.filterPlayerId(myId)
    val myBuilders = my.filterType(BUILDER_UNIT)
    val myFreeBuilders = myBuilders.free()
    val myMelee = my.filterType(MELEE_UNIT)
    val myRanged = my.filterType(RANGED_UNIT)
    val myInfantry = listOf(myMelee, myRanged).flatten()

    val myBuilderBase = my.firstOrNull { it.entityType == BUILDER_BASE }
    val myMeleeBase = my.firstOrNull { it.entityType == MELEE_BASE }
    val myRangedBase = my.firstOrNull { it.entityType == RANGED_BASE }

    val myPopulation = my.fold(0) { acc, entity -> acc + properties(entity).populationUse }
    val myPopulationLimit = my.fold(0) { acc, entity -> acc + properties(entity).populationProvide }

    val enemies = nonResources.filterNotPlayerId(myId)
    val enemyBuilders = enemies.filterType(BUILDER_UNIT)
    val enemyMelee = enemies.filterType(MELEE_UNIT)
    val enemyRanged = enemies.filterType(RANGED_UNIT)
    val enemyInfantry = listOf(enemyMelee, enemyRanged).flatten()
    val enemyUnits = listOf(enemyMelee, enemyRanged, enemyBuilders).flatten()
    fun List<Entity>.free() = filterNot { ordersCache.contains(it.id) }
}

class OrdersCache: MutableList<Int> by mutableListOf()

private fun List<Entity>.filterPlayerId(playerId: Int) = filter { it.playerId == playerId }
private fun List<Entity>.filterNotPlayerId(playerId: Int) = filterNot { it.playerId == playerId }

fun Array<Entity>.filterType(entityType: EntityType) = filter { it.entityType == entityType }
fun Array<Entity>.filterNotType(entityType: EntityType) = filterNot { it.entityType == entityType }

fun List<Entity>.filterType(entityType: EntityType) = filter { it.entityType == entityType }