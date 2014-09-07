package nx.comm

import java.net.{ServerSocket, Socket, SocketTimeoutException}

import nx.Asynch

class ConnectionListener(_callback: (Socket) => Unit) extends Asynch
{
	var socket: ServerSocket = null
	var code: () => Unit = () => tryDo(try _callback(socket.accept) catch {case ste: SocketTimeoutException =>}, _e => {log(_e.getMessage);stop})
	override def callback = () => tryDo(socket.close)

	def reset: Boolean =
	{
		stopAndWait
		if (socket != null)
			socket.close
		tryDo({
			socket = new ServerSocket(serverPort)
			socket.setSoTimeout(5000)
			run
		}, _e => log(_e.getMessage))
	}

	reset
}