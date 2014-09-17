package nx.util

import javafx.application.Platform

import scala.language.postfixOps

class Code[T <: Any](code: => T) extends Tools
{
	var active = true
	def % = () => code
	def ^^ : Boolean = ^^()
	def ^^(_itBroke: => Unit = null): Boolean = try {code;true} catch {case e: Exception => _itBroke;false}
	def ^^^(_itBroke: Exception => Unit): Boolean = try {code;true} catch {case e: Exception => if (_itBroke != null){_itBroke(e)};false}
	def ^* = synchronized{code}
	def ^^* : Boolean = ^^*(null)
	def ^^*(_itBroke: => Unit): Boolean = ^^(_itBroke)^*
	def ^^^*(_itBroke: Exception => Unit): Boolean = ^^^(_itBroke)^*
	def x = new Thread(new Runnable{def run = code}).start
	def fx = if (Platform.isFxApplicationThread) code.^^ else Platform.runLater(new Runnable {def run = code^^})
}