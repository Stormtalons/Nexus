package nx.util

import scala.collection.mutable.ArrayBuffer

trait Asynch extends Tools
{
	private var running = false
	var done = true
	private val activities = ArrayBuffer[() => Unit]()
	def addActivity(_act: => Unit) = activities += (() => _act)
	def callback: () => Unit = null
	def run =
	{
		running = true
		done = false
		ex({
			for (act <- activities)
				ex(while (running)
				{
					tryDo(act(), _e => log(s"Error occurred: ${_e.getMessage}"))
					Thread.sleep(1)
				})
			while (running)
				Thread.sleep(10)
			if (callback != null)
				callback()
			done = true
		})
	}
	def stop = running = false
	def waitFor = while (!done) Thread.sleep(5)
	def stopAndWait = {stop;waitFor}
}
