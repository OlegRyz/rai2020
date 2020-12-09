package org.niksi.rai.strategies

import model.Entity
import org.niksi.rai.controller.*

val Balanced = StrategicDsl {
    BUILD_UNIT_BUILDER.rule("Builders are Limited") {
        (it.myBuilders.count() > 0).isGood()
        (it.myBuilders.count() > 5).isBad()
        (it.myBuilders.count() > 5 && it.myInfantry.count() < 5).isNotAcceptable()
    }

    BUILD_UNIT_MELEE.rule("Builders are Limited") {
        (it.myBuilders.count() < 5).isBad()
        (it.myMelee.count() > 0).isGood()
        (it.myBuilders.count() > 4).isGood()
    }

    BUILD_UNIT_RANGED.rule("Builders are Limited") {
        (it.myBuilders.count() < 5).isBad()
        (it.myMelee.count() > 0).isGood()
        (it.myBuilders.count() > 4).isGood()
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
        (it.myInfantry.count() < it.enemyInfantry.count()).isNotAcceptable()
    }

    GEATHER_ARMY.rule("") {
        (it.myInfantry.count() < 10).isGood()
        (it.myInfantry.count() > 9).isBad()
    }

    BUILD_HOUSE.rule("Build a house if food is low") {
        (it.myPopulationLimit - it.myPopulation < 3
                && !it.myUnhealthyBuildings.any()).isAlwaysNeeded()
        (it.myPopulationLimit - it.myPopulation > 5).isNotAcceptable()
    }

    REPAIR_BUILDINGS.rule("") {
        (it.myUnhealthyBuildings.any()).isAlwaysNeeded()
        (!it.myUnhealthyBuildings.any()).isNotAcceptable()
    }
}




