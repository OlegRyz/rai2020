package org.niksi.rai.controller

import model.*
import model.EntityType.*

private val Entity.isBuilding: Boolean
    get() = entityType == BUILDER_BASE ||
            entityType == MELEE_BASE ||
            entityType == RANGED_BASE ||
            entityType == HOUSE ||
            entityType == TURRET ||
            entityType == WALL


class FieldState(
    val entities: Array<Entity>,
    val entityProperties: MutableMap<EntityType, EntityProperties>,
    players: Array<Player>,
    myId: Int,
    val ordersCache: OrdersCache
) {
    fun properties(entity: Entity) = entityProperties[entity.entityType]!!
    fun properties(entityType: EntityType) = entityProperties[entityType]!!
    fun simulate(metaAction: MetaAction) = ImaginaryState(this, metaAction)
    fun recordOrder(entities: List<Entity>, metaAction: MetaAction) = ordersCache.record(entities, metaAction)

    fun recordOrder(entity: Entity, metaAction: MetaAction) = ordersCache.record(entity, metaAction)

    fun canceldOrder(entity: Entity) = ordersCache.remove(entity.id)

    val resources = entities.filterType(RESOURCE)
    val nonResources = entities.filterNotType(RESOURCE)

    val me = players.first { it.id == myId }
    val my = nonResources.filterPlayerId(myId)
    val myBuilders = my.filterType(BUILDER_UNIT)
    val myHouseBuilder: Entity? = myBuilders.firstOrNull { it.id == ordersCache.getId(BUILD_HOUSE).firstOrNull() }
    val myFreeBuilders = myBuilders.free()
    val myMelee = my.filterType(MELEE_UNIT)
    val myRanged = my.filterType(RANGED_UNIT)
    val myInfantry = listOf(myMelee, myRanged).flatten()
    val myFreeInfantry = myInfantry.free()

    val myBuildings = my.filter { it.isBuilding }
    val myBuilderBase = myBuildings.firstOrNull { it.entityType == BUILDER_BASE }
    val myMeleeBase = myBuildings.firstOrNull { it.entityType == MELEE_BASE }
    val myRangedBase = myBuildings.firstOrNull { it.entityType == RANGED_BASE }
    val myTurrets = myBuildings.filterType(TURRET)
    val myUnhealthyBuildings = myBuildings.filter { it.health < entityProperties[it.entityType]!!.maxHealth }

    val myPopulation = my.fold(0) { acc, entity -> acc + properties(entity).populationUse }
    val myPopulationLimit =
        my.fold(0) { acc, entity -> acc + (if (entity.active) properties(entity).populationProvide else 0) }

    val enemies = nonResources.filterNotPlayerId(myId)
    val enemyBuilders = enemies.filterType(BUILDER_UNIT)
    val enemyMelee = enemies.filterType(MELEE_UNIT)
    val enemyRanged = enemies.filterType(RANGED_UNIT)
    val enemyInfantry = listOf(enemyMelee, enemyRanged).flatten()
    val enemyUnits = listOf(enemyMelee, enemyRanged, enemyBuilders).flatten()
    fun List<Entity>.free(): List<Entity> = filterNot { ordersCache.keys.contains(it.id) }
    fun myEntityBy(id: Int?) = if (id == null) null else my.firstOrNull { it.id == id }
    fun myEntityBy(ids: List<Int>) = my.filter { it.id in ids }
    fun canceldOrder(action: MetaAction) {
        val keys = ordersCache.entries.filter { it.value.metaAction.isSame(action) }.map { it.key }
        keys.forEach { ordersCache.remove(it) }
    }
}

private fun List<Entity>.filterPlayerId(playerId: Int) = filter { it.playerId == playerId }
private fun List<Entity>.filterNotPlayerId(playerId: Int) = filterNot { it.playerId == playerId }

fun Array<Entity>.filterType(entityType: EntityType) = filter { it.entityType == entityType }
fun Array<Entity>.filterNotType(entityType: EntityType) = filterNot { it.entityType == entityType }

fun List<Entity>.filterType(entityType: EntityType) = filter { it.entityType == entityType }