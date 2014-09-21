package nx.util

import javafx.event.{ActionEvent, Event, EventHandler, EventType}
import javafx.scene.Node
import javafx.scene.input.{DragEvent, MouseDragEvent, MouseEvent}
import javafx.stage.{WindowEvent, Window}

import scala.language.{postfixOps, implicitConversions}
import scala.reflect.runtime.universe._

trait InterfaceShortcuts extends Tools
{
	implicit def codeToActionHandler(_code: => Unit): EventHandler[ActionEvent] = handle[ActionEvent](_code%)
	implicit def codeToMouseEvent(_code: => Unit) = new EventHandler[MouseEvent]{def handle(_evt: MouseEvent) = _code}
	implicit def codeToMouseDragEvent(_code: => Unit) = new EventHandler[MouseDragEvent]{def handle(_evt: MouseDragEvent) = _code}
	implicit def codeToDragEvent(_code: => Unit) = new EventHandler[DragEvent]{def handle(_evt: DragEvent) = _code}
	implicit def codeToWindowEvent(_code: => Unit) = new EventHandler[WindowEvent]{def handle(_evt: WindowEvent) = _code}

	def addFilter[T <: Event: TypeTag](_node: Window, _evt: EventType[T], _code: => Unit) = _node.addEventFilter(_evt, handle[T](_code%))
	def addHandler[T <: Event: TypeTag](_node: Window, _evt: EventType[T], _code: => Unit) = _node.addEventHandler(_evt, handle[T](_code%))
	def addFilter[T <: Event: TypeTag](_node: Node, _evt: EventType[T], _code: => Unit) = _node.addEventFilter(_evt, handle[T](_code%))
	def addHandler[T <: Event: TypeTag](_node: Node, _evt: EventType[T], _code: => Unit) = _node.addEventHandler(_evt, handle[T](_code%))
	def handleEvt[T <: Event: TypeTag](_code: T => Unit) = new EventHandler[T]{def handle(_evt: T) = _code(_evt)}
	def handle[T <: Event: TypeTag](_code: () => Unit) = new EventHandler[T]{def handle(_evt: T) = _code()}
}
