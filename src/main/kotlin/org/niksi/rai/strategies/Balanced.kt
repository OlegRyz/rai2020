package org.niksi.rai.strategies

import model.Entity
import model.EntityType
import org.niksi.rai.controller.*

val Balanced = StrategicDsl {
    BUILD_UNIT_BUILDER.rule("Builders are Limited") {
        (it.myBuilders.count() > 0).isAlwaysNeeded()
        (it.myBuilders.count() > 5).isGood()
        (it.myBuilders.count() > 5 && it.myInfantry.count() < 1.5*it.myBuilders.count()).isBad()
    }

    BUILD_UNIT_MELEE.rule("Builders are Limited") {
        (it.myBuilders.count() < 5).isBad()
        (it.myMelee.count() > 0).isBad()
        (it.myBuilders.count() > 4).isBad()
    }

    BUILD_UNIT_RANGED.rule("Builders are Limited") {
        (it.myBuilders.count() < 5).isBad()
        (it.myMelee.count() > 0).isGood()
        (it.myBuilders.count() > 4).isGood()
        (it.myBuilders.count() > 5 && it.myInfantry.count() > 1.5*it.myBuilders.count()).isNotAcceptable()
    }

    BUILD_BASE_RANGED.rule("Builders are Limited") {
        (it.myBuilders.count() < 5).isBad()
        (it.myMelee.count() > 0).isGood()
        (it.myBuilders.count() > 4).isGood()
    }

    COLLECT_RESOURCES.rule("Builders are Limited") {
        (it.myFreeBuilders.any()).isAlwaysNeeded()
    }

    ATTACK_ENEMY.rule("") {
        (it.myInfantry.count() > it.enemyInfantry.count() + 5).isGood()
        (it.myInfantry.count() < it.enemyInfantry.count() + 5)
            .alsoCancelOrder(ATTACK_ENEMY).isNotAcceptable()
        (it.enemyInfantry.any { enemy -> it.my.any { distance(enemy.position, it.position) < 13 } }).isAlwaysNeeded()
    }

    ATTACK_NEIGHBOR.rule("") {
        true.isBad()
        (2*it.ordersCache.getId(ATTACK_NEIGHBOR).count() < it.myInfantry.count()).isGood()
        (it.myInfantry.count() < 9).isBad()

    }

    DEFEND_BUILDINGS.rule("") {
        true.isNotAcceptable()
        (it.enemies.near(it.myBuildings, 20).any()).isAlwaysNeeded()
    }

    GEATHER_ARMY.rule("") {
        true.isGood()
    }

    BUILD_HOUSE.rule("Build a house if food is low") {
        true.isGood()
        (it.myPopulationLimit - it.myPopulation < 3).isAlwaysNeeded()
        (it.myPopulation == 0 || it.myPopulationLimit > 2 * it.myPopulation).isNotAcceptable()
        (it.me.resource < it.properties(EntityType.HOUSE).initialCost + it.myFreeBuilders.count()).isNotAcceptable()
        (it.ordersCache.getId(BUILD_HOUSE).count() > 1).isNotAcceptable()
        if (it.myBuildings.any{!it.active}) {
            it.canceldOrder(BUILD_HOUSE)
        }
    }

    REPAIR_BUILDINGS_ALL.rule("") {
        (it.myUnhealthyBuildings.any()).isAlwaysNeeded()
        (it.myUnhealthyBuildings.isEmpty())
            .alsoCancelOrder(REPAIR_BUILDINGS_ALL).isNotAcceptable()
    }


}




