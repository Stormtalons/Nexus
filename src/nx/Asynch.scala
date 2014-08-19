package nx

trait Asynch
{
	import Main._

	private var running = false
	var done = true
	var code: () => Unit
	var callback: () => Unit = null
	def run = Main.run({
		running = true
		done = false
		while (running)
			if (code != null)
				try code() catch{case e=> log(e.getMessage);e.printStackTrace}
		if (callback != null)
			callback()
		done = true
	})
	def stop = running = false
}
