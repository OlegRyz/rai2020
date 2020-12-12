package org.niksi.rai.langpack

import model.Entity
import model.EntityAction

fun Entity?.stopProduction() = when(this) {
    null -> mutableMapOf()
    else ->mutableMapOf(
        id to EntityAction(
            null,
            null,
            null,
            null
        )
    )
}