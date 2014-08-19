package nx.comm

import java.net.Socket

import nx.{Main, Asynch}

import scala.collection.mutable.ArrayBuffer

class PeerManager extends Asynch
{
	val peers = new ArrayBuffer[PeerConnection]
	def addPeer(_peer: PeerConnection) = synchronized{peers += _peer}
	def addPeer(_socket: Socket): Unit = addPeer(new PeerConnection(_socket))

	val server = new PeerListener
	server.start(addPeer)

	def connect(_host: String, _port: Int) = addPeer(new PeerConnection(_host, _port))

	var code = () =>
	{
		var i = 0
		while (i < peers.length)
		{
			if (peers(i).recycleable)
			{
				synchronized
				{
					peers(i).dispose
					peers.remove(i)
				}
				i = -1
			}
			i += 1
		}
		peers.foreach(_p =>
		{
			_p.processTraffic
			
			var incMsg = _p.getNextIncomingMsg
			while(incMsg.length > 0)
			{
				println("Rcvd: " + incMsg)
				if (incMsg.equals("gimme"))
					_p.send(Main.serialize)
				incMsg = _p.getNextIncomingMsg
			}
		})
		Thread.sleep(100)
	}
	callback = () => server.stop

	run
}