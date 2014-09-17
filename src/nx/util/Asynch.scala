package nx.util

import scala.collection.mutable.ArrayBuffer

trait Asynch extends Tools
{
	private var running = false
	def isRunning = running
	private var done = true
	private val activities = new ArrayBuffer[Code[Unit]]
	def addActivity(_act: => Unit) = activities += (_act: Code[Unit])
	private val callbacks = new ArrayBuffer[Code[Unit]]
	def addCallback(_cb: => Unit) = callbacks += (_cb: Code[Unit])

	def run = if (!running)
	{
		running = true
		done = false

		{
			for (act <- activities)
				(while (running && act.active)
				{
					act.^^^(_e =>
					{
						log(s"Error occurred running activity: ${_e}");
						act.active = false
					})
					Thread.sleep(1)
				}).x
			while (running)
				Thread.sleep(50)
			for (cb <- callbacks)
					cb.^^
			done = true
		}.x
	}
	def stop = running = false
	def waitFor = while (!done) Thread.sleep(50)
	def stopAndWait = {stop;waitFor}
}
