import kotlinx.coroutines.experimental.CompletableDeferred

sealed class Message

class MoveResult(val x: Int, val y: Int, val alive: Boolean = true, val health: Int = 0)

class MoveMessage(val x: Int, val y: Int, val xt: Int, val yt: Int, val result: CompletableDeferred<MoveResult>) : Message()

class GetMessage(val x: Int, val y: Int, val result: CompletableDeferred<Int>) : Message()

class InteractMessage(val x: Int, val y: Int, val result: CompletableDeferred<Int>) : Message()

class DieMessage(val x: Int, val y: Int) : Message()

sealed class Command

class DrawDetails(val x: Int, val y: Int, val health: Int, val direction: Int)

class DrawCommand(val details: CompletableDeferred<DrawDetails>) : Command()