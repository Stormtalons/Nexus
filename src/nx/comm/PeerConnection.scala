package nx.comm

import java.io.{File, FileOutputStream}
import java.net.{InetSocketAddress, Socket}

import nx.Main

class PeerConnection
{
	def this(_host: String, _port: Int) =
	{
		this
		Main.run(connect(_host, _port))
	}
	def this(_socket: Socket) =
	{
		this
		receiveConnection(_socket)
	}

	var socket: Socket = null
	var recycleable = false

	private val incoming = new StringBuffer
	private val outgoing = new StringBuffer
	private def appendToIncoming(_str: String) = synchronized{incoming.append(_str)}
	private def appendToOutgoing(_str: String) = synchronized{outgoing.append(_str)}
	def getNextIncomingMsg: String = synchronized{poll(incoming, true)}
	def getNextOutgoingMsg: String = synchronized{poll(outgoing)}
	private def poll(_buf: StringBuffer, _truncateEM: Boolean = false): String =
	{
		if (_buf.indexOf("|EM") == -1)
			""
		else
		{
			val a = new Array[Char](_buf.indexOf("|EM") + 3)
			_buf.getChars(0, a.length, a, 0)
			_buf.delete(0, a.length)
			new String(a).substring(0, if (_truncateEM) a.length - 3 else a.length)
		}
	}

	def connect(_host: String, _port: Int) =
	{
		socket = new Socket
		try
		{
			socket.connect(new InetSocketAddress(_host, _port), 5000)
			send("gimme")
		} catch {case e: Exception => println(e.getMessage);dispose}
	}

	def receiveConnection(_socket: Socket) =
	{
		socket = _socket
	}

	def processTraffic =
	{
		try
		{
			while (socket.getInputStream.available > 0)
			{
				val data = new Array[Byte](socket.getInputStream.available)
				socket.getInputStream.read(data)
				val f = new File("D:\\Code\\Java\\IntelliJ\\Nexus\\folder" + Main.fn + "\\in.txt")
				if (!f.exists)
					f.createNewFile
				val fw = new FileOutputStream(f, true)
				fw.write(data)
				fw.flush
				fw.close
				appendToIncoming(new String(data))
			}

			var outMsg = getNextOutgoingMsg
			while(outMsg.length > 0)
			{
				val f = new File("D:\\Code\\Java\\IntelliJ\\Nexus\\folder" + Main.fn + "\\out.txt")
				if (!f.exists)
					f.createNewFile
				val fw = new FileOutputStream(f, true)
				fw.write(outMsg.getBytes)
				fw.flush
				fw.close
				socket.getOutputStream.write(outMsg.getBytes)
				outMsg = getNextOutgoingMsg
			}
		} catch {case e: Exception => println(e.getMessage);dispose}
	}

	def send(_str: String) = appendToOutgoing(_str + "|EM")

	def dispose =
	{
		recycleable = true
		try socket.close catch{case e: Exception =>println(e.getMessage);}
	}
}