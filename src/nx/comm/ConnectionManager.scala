package nx.comm

import java.net.Socket

import nx.{JSON, Main, Util, Asynch}

import scala.collection.mutable.ArrayBuffer

class ConnectionManager extends Asynch with Util
{
	val outgoingClient = new ClientConnection
	val clients = new ArrayBuffer[ClientConnection]
	var clientLimit = 5

	val server = new ConnectionListener(_socket => if (clients.length < clientLimit) accept(_socket) else reject(_socket))
	def accept(_socket: Socket) =
	{
		val newClient = new ClientConnection(_socket)
		synchronized{clients += newClient}
		log(s"Connection from ${newClient.getHost}/${newClient.getIP} accepted")
	}
	def reject(_socket: Socket) =
	{
		log(s"Connection from ${_socket.getLocalAddress.getHostName}/${_socket.getLocalAddress.getHostAddress} rejected - at client limit")
		_socket.close
	}

	var code = () =>
	{
		synchronized
		{
			var i = 0
			while (i < clients.length)
			{
				if (clients(i).recycleable)
				{
					clients.remove(i)
					i = -1
				}
				i += 1
			}

			clients.foreach(_client =>
			{
				_client.processTraffic
				while (_client.hasMsg)
				{
					val msgParts = _client.getMsg.split(sep)
					if (msgParts(0).equals("gimme"))
						_client.sendMsg(s"DESKTOP + $sep + ${Main.serialize}")
					else if (msgParts(0).equals("DESKTOP"))
						Main.loadState(JSON.parse(msgParts(1)))
				}
			})
		}
		Thread.sleep(100)
	}
	override def callback = () =>
	{
		log("Stopping listening server")
		server.stop
		log(s"Disposing of ${clients.length} clients")
		outgoingClient.dispose
		clients.foreach(_client => _client.dispose)
		clients.clear
		log("Client list cleared")
		server.waitFor
		log("Listening server stopped")
	}

	def connect(_host: String, _port: Int) = log(s"Connection to ${_host}:${_port} ${if (outgoingClient.connect(_host, _port)) "" else "un"} successful")

	run
}