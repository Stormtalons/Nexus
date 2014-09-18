package nx

import nx.comm.ConnectionManager
import nx.comm.sendable.{PartialSendable, Sendable}
import nx.util.{Tools}

import scala.language.postfixOps

object TestingStage extends App with Tools
{
	{
		val host = new ConnectionManager(9998)
		val client = new ConnectionManager(9997)
		client.connect("127.0.0.1", 9998)
		client.send(new Sendable("Line of stringderp."^10))
		Thread.sleep(200)
		println
		client.stop
		host.stop
	}^^^{e => e.printStackTrace;System.exit(0)}
}