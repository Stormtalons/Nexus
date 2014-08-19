package nx.comm

import java.net.{SocketTimeoutException, ServerSocket, Socket}

import nx.Asynch

class PeerListener extends Asynch
{
	val port = 19265
	val socket = new ServerSocket(port)
	socket.setSoTimeout(5000)

	var code: () => Unit = null

	def start(_callback: (Socket) => Unit) =
	{
		code = () => {try _callback(socket.accept()) catch {case ste: SocketTimeoutException =>}}
		run
	}
}