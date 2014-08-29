package nx.comm

import java.net.{SocketTimeoutException, ServerSocket, Socket}

import nx.{Main, Asynch}

class ConnectionListener(_callback: (Socket) => Unit) extends Asynch
{import Main._
	var socket: ServerSocket = null
	var code: () => Unit = () => t(try _callback(socket.accept) catch {case ste: SocketTimeoutException =>}, _e => {log(_e.getMessage);stop})
	override def callback = () => t(socket.close)

	def reset: Boolean =
	{
		stopAndWait
		if (socket != null)
			socket.close
		t({
			socket = new ServerSocket(serverPort)
			socket.setSoTimeout(5000)
			run
		}, _e => log(_e.getMessage))
	}

	reset
}