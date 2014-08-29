package nx.comm

import nx.Asynch

import scala.collection.mutable.ArrayBuffer

class ConnectionManager extends Asynch
{import nx.Main._
	
	val outgoingClient = new ClientConnection
	val clients = new ArrayBuffer[ClientConnection]

	val server = new ConnectionListener(_socket => synchronized{clients += new ClientConnection(_socket)})

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
						_client.sendMsg("DESKTOP" + sep + serialize)
					else if (msgParts(0).equals("DESKTOP"))
						loadState(msgParts(1))
				}
			})
		}
		Thread.sleep(100)
	}
	override def callback = () =>
	{
		server.stop
		outgoingClient.dispose
		clients.foreach(_client => _client.dispose)
		clients.clear
	}

	def connect(_host: String, _port: Int) = outgoingClient.connect(_host, _port)

	run
}