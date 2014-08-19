package nx

trait Asynch
{
	private var running = false
	var done = true
	var code: () => Unit
	var callback: () => Unit = null
	def run = Main.run({
		running = true
		done = false
		while (running)
			try code() catch{case e=>println(e.getMessage);}
		if (callback != null)
			callback()
		done = true
	})
	def stop = running = false
}
