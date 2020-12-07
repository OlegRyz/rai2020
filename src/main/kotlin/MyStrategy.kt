import model.*
import org.niksi.rai.controller.Controller
import org.niksi.rai.controller.checkConsistency

class MyStrategy {
    var controller: Controller? = null
    fun getAction(playerView: PlayerView, debugInterface: DebugInterface?) = with(playerView) {

        controller = controller ?: Controller(myId, mapSize, maxPathfindNodes, maxTickCount, fogOfWar)
        controller?.checkConsistency(currentTick, myId, mapSize, maxPathfindNodes, maxTickCount, fogOfWar)

        controller?.tick(currentTick, entities, entityProperties, players)
        controller!!.bestAction
    }

    fun debugUpdate(playerView: PlayerView, debugInterface: DebugInterface) {
        debugInterface.send(model.DebugCommand.Clear())
        debugInterface.getState()
    }
}
