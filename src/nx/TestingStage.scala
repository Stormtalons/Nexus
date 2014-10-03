package nx

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.io.Tcp._
import nx.comm.sendable.Sendable
import nx.comm.{O, Client}
import nx.util.Tools

import scala.collection.mutable

object TestingStage extends App with Tools
{
	implicit val system = ActorSystem("CS")
	serverPort = 2772
	receivedItems = new mutable.HashMap[UUID, Sendable[_]]

	val cli1 = system.actorOf(Props(new Client), name = "cli1")
	cli1 ! Bind
	sleep(100)

	val cli2 = system.actorOf(Props(new Client), name = "cli2")
	cli2 ! O("127.0.0.1", serverPort)
	sleep(100)

	val ht = Sendable("Hello, World!")
	cli2 ! ht
	sleep(200)

	println(receivedItems.get(ht.guid).get)

	cli1 ! Close
	cli2 ! Close

	system.shutdown
}