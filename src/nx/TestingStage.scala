package nx

import java.util.Scanner

import nx.util.{Asynch, Tools}

object TestingStage extends App with Tools
{
	val as = new Asynch{}

	as.addActivity({
		println("starting count")
		(1 to 3).foreach(i => print(i + ", "))
		println("\nwaiting.\n")
		Thread.sleep(3000)
	})

	as.addActivity({
		println("faster activity")
		Thread.sleep(500)
	})

	as.addCallback({
		println("done running.")
	})

	as.run

	new Scanner(System.in).nextLine

	println("stopping and waiting")
	as.stopAndWait

	println("exiting program.")
}