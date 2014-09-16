package nx.settings

import javafx.scene.control.Label
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.{GridPane, StackPane}

import nx.Main
import nx.util.{Code, Tools}

object Setting
{
	final val HORIZONTAL = 0
	final val VERTICAL = 1
}

abstract class Setting[T >: Null](_label: String, _value: T, _icon: Image, _scale: Int) extends GridPane with Tools
{
	def this(_label: String, _value: T, _icon: String, _scale: Int) = this(_label, _value, Main.tryGet(new Image(_icon)), _scale)
	def this(_label: String, _icon: Image, _scale: Int) = this(_label, null, _icon, _scale)
	def this(_label: String, _icon: String, _scale: Int) = this(_label, null, Main.tryGet(new Image(_icon)), _scale)
	def this(_label: String) = this(_label, null, "", 0)
	def this(_label: String, _value: T) = this(_label, _value, "", 0)
	def this(_value: T, _icon: Image, _scale: Int) = this("", _value, _icon, _scale)
	def this(_value: T, _icon: String, _scale: Int) = this("", _value, _icon, _scale)
	def this(_icon: Image, _scale: Int) = this("", null, _icon, _scale)
	def this() = this("", null, "", 0)
	
	protected var orientation_ = Setting.HORIZONTAL
	def orientation = orientation_
	def orientation_=(_newVal: Int) =
		if (_newVal == Setting.HORIZONTAL || _newVal == Setting.VERTICAL)
		{
			orientation_ = _newVal
			doLayout
		}
	
	protected var editable_ = true
	def editable = editable_
	def editable_=(_editable: Boolean) =
	{
		editable_ = _editable
		if (!editable_)
			endEdit(false)
	}

	protected var editing = false

	private val icon_ : ImageView = new ImageView(_icon)
	def icon = icon_.getImage
	def icon_=(_filePath: String): Unit = icon = tryGet(new Image(_filePath))
	def icon_=(_image: Image): Unit =
	{
		icon_.setImage(_image)
		doLayout
	}.fx
	def setIconScale(_scale: Int) =
	{
		icon_.setFitHeight(_scale)
		icon_.setFitWidth(_scale)
	}
	setIconScale(_scale)

	protected val label_ = new Label(_label)
	label_.setWrapText(true)
	def label = label_.getText
	def label_=(_label: String) = label_.setText(_label).fx

	protected var value_ : T = _value
	def value: String = if (value_ == null) "" else value_.toString
	def value_=(_value: T)
//	def value_=(_value: String)
	
	protected val editStack = new StackPane

	def beginEdit
	
	def endEdit(_save: Boolean)

	def doLayout =
	{
		var (c, r) = (0, 0)
		getChildren.clear
		if (icon != null)
		{
			add(icon_, c, r)
			r += 1
		}
		if (label.length > 0)
		{
			add(label_, c, r)
			if (orientation == Setting.HORIZONTAL)
			{
				c += 1
				GridPane.setColumnSpan(icon_, 2)
				GridPane.setRowSpan(icon_, 1)
			}
			else
				r += 1
		}
		add(editStack, c, r)
	}.fx

	doLayout
}