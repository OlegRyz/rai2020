package org.niksi.rai.strategies

import model.Entity
import org.niksi.rai.controller.*

val Balanced = StrategicDsl {
    BUILD_UNIT_BUILDER.rule("Builders are Limited") {
        (it.myBuilders.count() > 0).isGood()
        (it.myBuilders.count() > 5).isBad()
        (it.myBuilders.count() > 5 && it.myInfantry.count() < 5).isNotAcceptable()
    }

    STOP_MAD_PRINTER.rule("Stop mad printer") {
        true.isNotAcceptable()
        (it.myBuilders.count() > 3 && it.myInfantry.count() < 5 && it.ordersCache.none{it.order == STOP_MAD_PRINTER})
            .isAlwaysNeeded()
    }

    UNLEASH_MAD_PRINTER.rule("Unleash mad printer") {
        true.isNotAcceptable()
        (it.ordersCache.any{it.order == STOP_MAD_PRINTER} &&
                (it.myInfantry.count() / it.myBuilders.count()) > 2)
            .isAlwaysNeeded()
        (it.ordersCache.any{it.order == STOP_MAD_PRINTER} &&
                it.myBuilders.count() < 4)
            .isAlwaysNeeded()
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
        (it.enemyInfantry.any { enemy -> it.my.any { distance(enemy.position, it.position) < 13 } }).isAlwaysNeeded()
    }

    GEATHER_ARMY.rule("") {
        true.isGood()
    }

    BUILD_HOUSE.rule("Build a house if food is low") {
        (it.myPopulationLimit - it.myPopulation < 3
                && !it.myUnhealthyBuildings.any()).isGood()
        (it.myPopulationLimit - it.myPopulation > 5).isNotAcceptable()
    }

    REPAIR_BUILDINGS.rule("") {
        (it.myUnhealthyBuildings.any()).isAlwaysNeeded()
        (!it.myUnhealthyBuildings.any() || it.ordersCache.filter { it.order == REPAIR_BUILDINGS }.count() > 2).isNotAcceptable()
    }
}




