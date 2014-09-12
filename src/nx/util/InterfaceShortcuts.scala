package nx.util

import java.awt.event.ActionListener
import java.util.EventListener
import javafx.event.{Event, EventHandler, EventType}
import javafx.scene.Node
import javafx.stage.Window

import scala.reflect.runtime.universe._

trait InterfaceShortcuts
{
	def addFilter[T <: Event](_node: Window, _evt: EventType[T], _code: => Unit) = _node.addEventFilter(_evt, handle[T](_code))
	def addHandler[T <: Event](_node: Window, _evt: EventType[T], _code: => Unit) = _node.addEventHandler(_evt, handle[T](_code))
	def addFilter[T <: Event](_node: Node, _evt: EventType[T], _code: => Unit) = _node.addEventFilter(_evt, handle[T](_code))
	def addHandler[T <: Event](_node: Node, _evt: EventType[T], _code: => Unit) = _node.addEventHandler(_evt, handle[T](_code))
	def handleEvt[T <: Event](_code: T => Unit): EventHandler[T] = new EventHandler[T]{def handle(_evt: T) = _code(_evt)}
	def handle[T <: Event](_code: => Unit): EventHandler[T] = new EventHandler[T]{def handle(_evt: T) = _code}

	def addListener[T <: EventListener](_code: () => Unit)(implicit tag: TypeTag[T]): T =
	{
		tag match
		{
			case ae: java.awt.event.ActionListener => new ActionListener {def actionPerformed(e: java.awt.event.ActionEvent) = _code()}
			case _ => null
		}
	}.asInstanceOf[T]
}
