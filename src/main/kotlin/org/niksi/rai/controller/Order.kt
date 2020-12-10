package org.niksi.rai.controller

import model.Entity
import model.EntityAction

class OrdersCache: MutableMap<Int,OrderItem> by mutableMapOf() {
    fun executeOrders(): MutableMap<Int, EntityAction> {
        return mutableMapOf()
    }

    fun record(entities: List<Entity>, metaAction: MetaAction) = putAll(entities.map{ it.id to OrderItem(metaAction)})
    fun record(entitiy: Entity, metaAction: MetaAction) = put(entitiy.id, OrderItem(metaAction))

    fun getId(metaAction: MetaAction): List<Int> = filter { (_, orderItem) -> orderItem.metaAction == metaAction}.keys.toList()
}

data class OrderItem(val metaAction: MetaAction,
                     val reccurent: (FieldState) -> MutableMap<Int, EntityAction> = { mutableMapOf() },
                     val cancellation: (FieldState) -> Boolean = {false})

class Order {

}