package org.niksi.rai.strategies

import org.niksi.rai.controller.*

val Balanced = StrategicDsl {
    BUILD_UNIT_BUILDER.rule("Builders are Limited") {
        (it.myBuilders.count() > 0).isGood()
        (it.myBuilders.count() > 5).isBad()
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
        true.isGood()
    }

    ATTACK_ENEMY.rule("") {
        (it.myInfantry.count() > it.enemyInfantry.count()).isGood()
        (it.myInfantry.count() < it.enemyInfantry.count()).isBad()
    }
}


