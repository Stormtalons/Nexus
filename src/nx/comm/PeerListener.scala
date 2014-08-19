package nx.comm

import java.net.{SocketTimeoutException, ServerSocket, Socket}

import nx.Asynch

class PeerListener extends Asynch
{
	val port = 19265
	var socket: ServerSocket = null
	try
	{
		socket = new ServerSocket(port)
		socket.setSoTimeout(5000)
	}
	catch{case e: Exception => println(e.getMessage)}

	var code: () => Unit = null

	def start(_callback: (Socket) => Unit) =
	{
		code = () => {try _callback(socket.accept()) catch {case ste: SocketTimeoutException => case npe: NullPointerException => stop case e: Exception =>println(e.getMessage);}}
		run
	}
}