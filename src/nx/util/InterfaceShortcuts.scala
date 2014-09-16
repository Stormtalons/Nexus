package nx.util

import javafx.event.{Event, EventHandler, EventType}
import javafx.scene.Node
import javafx.stage.Window

import scala.language.postfixOps
import scala.reflect.runtime.universe._

trait InterfaceShortcuts extends Tools
{
	def addFilter[T <: Event: TypeTag](_node: Window, _evt: EventType[T], _code: => Unit) = _node.addEventFilter(_evt, handle[T](_code))
	def addHandler[T <: Event: TypeTag](_node: Window, _evt: EventType[T], _code: => Unit) = _node.addEventHandler(_evt, handle[T](_code))
	def addFilter[T <: Event: TypeTag](_node: Node, _evt: EventType[T], _code: => Unit) = _node.addEventFilter(_evt, handle[T](_code))
	def addHandler[T <: Event: TypeTag](_node: Node, _evt: EventType[T], _code: => Unit) = _node.addEventHandler(_evt, handle[T](_code))
	def handleEvt[T <: Event: TypeTag](_code: T => Unit) = new EventHandler[T]{def handle(_evt: T) = _code(_evt)}
	def handle[T <: Event: TypeTag](_code: => Unit) = new EventHandler[T]{def handle(_evt: T) = _code%}
}
