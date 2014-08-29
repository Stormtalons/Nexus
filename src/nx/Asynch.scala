package nx

trait Asynch
{import Main._
	private var running = false
	var done = true
	var code: () => Unit
	def callback: () => Unit = null
	def run = ex({
		running = true
		done = false
		while (running)
			if (code != null)
				try {code();Thread.sleep(1)} catch{case e=> log(e.getMessage);e.printStackTrace}
		if (callback != null)
			callback()
		done = true
	})
	def stop = running = false
	def waitFor = while (!done) Thread.sleep(5)
	def stopAndWait = {stop;waitFor}
}
