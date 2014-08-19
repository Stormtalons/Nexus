package nx.comm

import java.net.Socket

import nx.Asynch

import scala.collection.mutable.ArrayBuffer

class PeerManager extends Asynch
{
	val peers = new ArrayBuffer[PeerConnection]
	val server = new PeerListener

	server.start(addClient)

	def addClient(_socket: Socket): Unit = peers += new PeerConnection(_socket)

	var code = () =>
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
		peers.foreach(_p => _p.processTraffic)
		Thread.sleep(1)
	}
	callback = () => server.stop

	run
}