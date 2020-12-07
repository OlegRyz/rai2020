package org.niksi.rai.controller

import model.*
import kotlin.collections.*

class Controller(
        val myId: Int,
        val mapSize: Int,
        val maxPathfindNodes: Int,
        val maxTickCount: Int,
        val fogOfWar: Boolean) {
    var bestAction = Action()

    fun tick(currentTick: Int, entities: Array<Entity>, entityProperties: MutableMap<EntityType, EntityProperties>, players: Array<Player>) {
        bestAction = thoughtfulActions().takeBest().DecodeToAction()
    }

    private fun thoughtfulActions(): Iterable<MetaAction> {
        return object : Iterable<MetaAction> {
            override fun iterator(): Iterator<MetaAction> = object : Iterator<MetaAction> {
                override fun hasNext(): Boolean {
                    return false
                }

                override fun next(): MetaAction {
                    TODO("not implemented")
                }

            }

        }
    }

}

private fun Iterable<MetaAction>.takeBest() = maxByOrNull { 0 } ?: DO_NOTHING

