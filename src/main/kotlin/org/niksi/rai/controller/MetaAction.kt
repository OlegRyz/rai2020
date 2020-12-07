package org.niksi.rai.controller

import model.Action

val DO_NOTHING = MetaAction {
    Action()
}

class MetaAction(val decoder: () -> Action) {
    fun DecodeToAction() = decoder()
}