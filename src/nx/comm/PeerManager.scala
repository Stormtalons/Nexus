package nx.comm

import nx.Asynch

import scala.collection.mutable.ArrayBuffer

class PeerManager extends Asynch
{
	import nx.Main._
	
	val outgoingPeer = new PeerConnection
	val clients = new ArrayBuffer[PeerConnection]

	val server = new PeerListener(_socket => synchronized{clients += new PeerConnection(_socket)})

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
		outgoingPeer.dispose
		clients.foreach(_client => _client.dispose)
		clients.clear
	}

	def connect(_host: String, _port: Int) = outgoingPeer.connect(_host, _port)

	run
}