package nx.util

import javafx.application.Platform

class Code[T <: Any](code: => T) extends Tools
{
	var active = true

	def % = () => code
	def ^ : Boolean = ^(null)
	def ^(_exHandler: Exception => Unit): Boolean = try {code;true} catch {case e: Exception => if (_exHandler != null){_exHandler(e)};false}
	def * = synchronized{code}
	def ^* : Boolean = ^*(null)
	def ^*(_exHandler: Exception => Unit): Boolean = ^(_exHandler)*
	def x = new Thread(new Runnable{def run = code}).start
	def fx = if (Platform.isFxApplicationThread) code.^ else Platform.runLater(new Runnable {def run = code^})
}