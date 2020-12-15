package org.niksi.rai.strategies

import model.*
import org.niksi.rai.controller.MetaAction
import org.niksi.rai.controller.RulesContext
import org.niksi.rai.controller.StrategicDsl
import org.niksi.rai.controller.closest

val F_EARLY_STAGE_COLLECT_RESOURCES = MetaAction() {state ->
    toAction(state.myFreeBuilders.map { builder ->
        state.resources.closest(builder.position)?.run {
            state.recordOrder(builder, this@MetaAction)
            builder.id to builder.attack(this)
        }
    })
}

fun toAction(actions: List<Pair<Int, EntityAction?>?>) = actions
    .filterNotNull()
    .filter {it.second != null}
    .map{it.first to  it.second!!}
    .toMap()
    .toMutableMap()

val FastBuilding = StrategicDsl {
    F_EARLY_STAGE_COLLECT_RESOURCES.rule("F_EARLY_STAGE_COLLECT_RESOURCES") { state ->
        applicableFor(state.myBuilders.count() < 20)
    }
}

private fun RulesContext.applicableFor(condition: Boolean) {
    when (condition) {
        true -> true.isAlwaysNeeded()
        else -> true.isNotAcceptable()
    }
}

private fun Entity.attack(target: Entity) = EntityAction(
    MoveAction(target.position, true, false),
    null,
    AttackAction(null, AutoAttack(0, arrayOf(EntityType.RESOURCE))),
    null
)
