package org.niksi.rai.strategies

import model.EntityType
import org.niksi.rai.controller.*

val Balanced = StrategicDsl {
    DEFENSIVE_WALL_RIGHT.rule("debug rule") {
        true.isNotAcceptable()
        it.enemyInfantry.inZone(25, 120, 0, 50).any().isAlwaysNeeded()
    }
    SNAKE.rule("debug rule") {
        true.isAlwaysNeeded()
    }
    SNAKE_MOVE.rule("") {
        true.isNotAcceptable()
        (it.ordersCache.getId(SNAKE).count() >= 5).isAlwaysNeeded()
        (it.ordersCache.getId(SNAKE_MOVE).count() >= 5).isAlwaysNeeded()
    }
    BUILD_UNIT_BUILDER.rule("Builders are Limited") {
        (it.myBuilders.count() > 0).isAlwaysNeeded()
        (it.myBuilders.count() > 5).isGood()
    }

    BUILD_UNIT_MELEE.rule("Builders are Limited") {
        (it.myBuilders.count() < 5).isBad()
        (it.myMelee.count() > 0).isBad()
        (it.myBuilders.count() > 4).isGood()
    }

    (BUILD_UNIT_BUILDER to BUILD_UNIT_RANGED).pairedRule("") {
        true.isNotAcceptable()
        (it.myBuilders.count() > 8).isAlwaysNeeded()
        val choice = (it.myInfantry.count() > 1.5 * it.myBuilders.count())
        choice
    }

    BUILD_UNIT_RANGED.rule("Builders are Limited") {
        (it.myBuilders.count() < 5).isBad()
        (it.myMelee.count() > 0).isGood()
        (it.myBuilders.count() > 4).isGood()
    }

    BUILD_BASE_RANGED.rule("Builders are Limited") {
        true.isNotAcceptable()
        (it.myRangedBase == null && it.me.resource + it.myBuilders.count() * 3 > 500).isAlwaysNeeded()
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
        (2*(it.ordersCache.getId(ATTACK_NEIGHBOR).count() + it.ordersCache.getId(ATTACK_DIAGONAL).count())
                < it.myInfantry.count()).isGood()
        (it.myInfantry.count() < 9).isBad()

    }

    ATTACK_DIAGONAL.rule("") {
        true.isNotAcceptable()
        (2*it.ordersCache.getId(ATTACK_DIAGONAL).count() < it.myInfantry.count()).isGood()
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




