package org.niksi.rai.controller

import model.*

val DO_NOTHING = MetaAction {
    Action()
}

val COLLECT_RESOURCES = MetaAction {
    it.myBuilders.collect()
}

fun List<Entity>.act(action: (Entity) -> EntityAction) = Action(associateBy({ it.id }, action).toMutableMap())

private fun List<Entity>.collect() = act {
    EntityAction(
            null,
            null,
            AttackAction(null, AutoAttack(10, arrayOf(EntityType.RESOURCE))),
            null)
}

class MetaAction(val decoder: (FieldState) -> Action) {
    fun DecodeToAction(state: FieldState) = decoder(state)
}