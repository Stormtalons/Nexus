package nx.comm

import java.net.{SocketTimeoutException, ServerSocket, Socket}

import nx.{Main, Asynch}

class PeerListener extends Asynch
{
	import Main._

	var code: () => Unit = null

	val port = 19265
	var socket: ServerSocket = null
	try
	{
		socket = new ServerSocket(port)
		socket.setSoTimeout(5000)
	}
	catch{case e: Exception => log(e.getMessage)}

	def start(_callback: (Socket) => Unit) =
	{
		code = () =>
		{
			if (socket != null)
			{
				try _callback(socket.accept)
				catch
					{
						case ste: SocketTimeoutException =>
						case npe: NullPointerException =>
							log("null pointer on server callback")
							stop
						case e: Exception => log(e.getMessage)
					}
			}
			Thread.sleep(100)
		}
		run
	}
}