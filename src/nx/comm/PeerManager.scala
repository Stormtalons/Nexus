package nx.comm

import java.net.Socket

import nx.Asynch

import scala.collection.mutable.ArrayBuffer

class PeerManager extends Asynch
{
	import nx.Main._

	val peers = new ArrayBuffer[PeerConnection]
	def addPeer(_peer: PeerConnection) = synchronized{peers += _peer}
	def addPeer(_socket: Socket): Unit = addPeer(new PeerConnection(_socket))

	val server = new PeerListener
	server.start(addPeer)

	def connect(_host: String, _port: Int) = addPeer(new PeerConnection(_host, _port))

	var code = () =>
	{
		synchronized
		{
			var i = 0
			while (i < peers.length)
			{
				if (peers(i).recycleable)
				{
					peers(i).dispose
					peers.remove(i)
					i = -1
				}
				i += 1
			}

			peers.foreach(_p =>
			{
				_p.processTraffic
				while (_p.hasMsg)
				{
					val msgParts = _p.getMsgStr.split("<@@>")
					if (msgParts(0).equals("gimme"))
						_p.send("DESKTOP" + (sep: String) + serialize)
					else if (msgParts(0).equals("DESKTOP"))
						loadState(msgParts(1))
				}
			})
		}
		Thread.sleep(100)
	}
	callback = () =>
	{
		server.stop
		peers.foreach(_p => _p.dispose)
	}

	run
}