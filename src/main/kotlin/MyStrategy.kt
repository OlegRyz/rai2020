import model.*
import org.niksi.rai.controller.Controller
import org.niksi.rai.controller.checkConsistency

class MyStrategy {
    val version = "1.0.3".also {
        println("Version $it")
    }
    var controller: Controller? = null
    fun getAction(playerView: PlayerView, debugInterface: DebugInterface?, debug: Boolean = false) = with(playerView) {
        try {
            controller = controller ?: Controller(myId, mapSize, maxPathfindNodes, maxTickCount, fogOfWar)
            controller?.checkConsistency(currentTick, myId, mapSize, maxPathfindNodes, maxTickCount, fogOfWar)

            controller?.tick(currentTick, entities, entityProperties, players)
            controller!!.bestAction
        } catch (e: Exception) {
            println("Error ${e.message}; ")
            if (debug) {
                throw Exception("Fail caught, but life is hard and debug is ${debug}", e)
            } else {
                Action(mutableMapOf())
            }
        }
    }

    fun debugUpdate(playerView: PlayerView, debugInterface: DebugInterface) {
        debugInterface.send(model.DebugCommand.Clear())
        debugInterface.getState()
    }
}
