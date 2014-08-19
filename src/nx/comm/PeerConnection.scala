package nx.comm

import java.net.{SocketTimeoutException, InetSocketAddress, Socket}

class PeerConnection
{
	def this(_socket: Socket) =
	{
		this
		receiveConnection(_socket)
	}
	var socket: Socket = null
	val incoming = new StringBuffer
	val outgoing = new StringBuffer
	var recycleable = false

	def makeConnection(_host: String, _port: Int): Boolean =
	{
		socket = new Socket
		try
		{
			socket.connect(new InetSocketAddress(_host, _port), 5000)
			true
		} catch
		{
			case e: SocketTimeoutException =>
				recycleable = true
				false
		}
	}

	def receiveConnection(_socket: Socket) =
	{
		socket = _socket
	}

	def processTraffic =
	{
		try
		{
			if (socket.getInputStream.available > 0)
			{
				val data = new Array[Byte](socket.getInputStream.available)
				socket.getInputStream.read(data)
				incoming.append(new String(data))
			}
			if (outgoing.length > 0)
			{
				val toWrite = new Array[Char](outgoing.length)
				outgoing.getChars(0, toWrite.length - 1, toWrite, 0)
				socket.getOutputStream.write(new String(toWrite).getBytes)
			}
		} catch
		{
			case e: Exception =>
				try socket.close catch{case e =>}
				recycleable = true
		}
	}

	def getNextMessage: String =
	{
		if (incoming.indexOf("|EM") == -1)
			""
		else
		{
			val toReturn = new Array[Char](incoming.indexOf("|EM"))
			incoming.getChars(0, toReturn.length - 1, toReturn, 0)
			new String(toReturn)
		}
	}
}