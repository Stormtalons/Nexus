package nx.widgets

import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import javafx.scene.layout._

import nx.JSON

abstract class Widget extends StackPane
{
	implicit def stringToOption(s: String) = Some(s)
	implicit def anyrefToOption(ar: AnyRef) = Some(ar)

	GridPane.setFillHeight(this, true)
	GridPane.setFillWidth(this, true)
	GridPane.setVgrow(this, Priority.ALWAYS)
	GridPane.setHgrow(this, Priority.ALWAYS)

	getStyleClass.add("widget")
	getStylesheets.add("/nx/res/dft.css")

	protected var scale_ : Int = 100
	def scale: Int = scale_
	def scale_=(_scale: Int) = scale_ = _scale
	
	protected var onClick_ : EventHandler[MouseEvent] = null
	def onClick = onClick_
	def onClick_=(_onClick: => Unit) =
	{
		if (onClick != null)
			removeEventFilter(MouseEvent.MOUSE_CLICKED, onClick)
		onClick_ = new EventHandler[MouseEvent] {def handle(event: MouseEvent) = _onClick}
		addEventFilter(MouseEvent.MOUSE_CLICKED, onClick_)
	}

	def toJSON: JSON
}