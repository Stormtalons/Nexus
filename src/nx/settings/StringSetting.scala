package nx.settings

import java.lang
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.event.EventHandler
import javafx.scene.control.{Label, TextField}
import javafx.scene.image.Image
import javafx.scene.input.{KeyCode, KeyEvent, MouseEvent}

import nx.Util

class StringSetting(_label: String, _value: String, _icon: Image, _scale: Int) extends Setting[String](_label: String, _value: String, _icon: Image, _scale: Int) with Util
{
	def this(_label: String, _value: String, _icon: String, _scale: Int) = this(_label, _value, nx.Main.tryGet[Image]({new Image(_icon)}), _scale)

	def value_=(_value: String) =
	{
		value_ = _value
		fx({
			valueLabel_.setText(value)
			valueEdit_.setText(value)
		})
	}

	protected val valueLabel_ = new Label(value)
	valueLabel_.setWrapText(true)
	valueLabel_.setStyle(
		"-fx-background-color: darkgray;" +
			"-fx-border-color: #505050;" +
			"-fx-border-style: solid;" +
			"-fx-border-width: 1;" +
			"-fx-font-family: Consolas;" +
			"-fx-font-size: 10pt;")
	valueLabel_.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler[MouseEvent]{def handle(_evt: MouseEvent) =
		if (_evt.getClickCount == 2 && editable)
		{
			beginEdit
			_evt.consume
		}})

	protected val valueEdit_ = new TextField(value)
	valueEdit_.setPrefWidth(0)
	valueEdit_.setVisible(false)
	valueEdit_.focusedProperty.addListener(new ChangeListener[lang.Boolean] {
		def changed(_focused: ObservableValue[_ <: lang.Boolean], _old: lang.Boolean, _new: lang.Boolean) =
			if (!_new && !editing)
				endEdit(true)
	})
	valueEdit_.setOnKeyReleased(new EventHandler[KeyEvent]{def handle(_evt: KeyEvent) =
		if (_evt.getCode.equals(KeyCode.ENTER))
			endEdit(true)
		else if (_evt.getCode.equals(KeyCode.ESCAPE))
			endEdit(false)
	})

	editStack.getChildren.addAll(valueLabel_, valueEdit_)

	def beginEdit = fx({
		valueEdit_.setText(value)
		valueLabel_.setVisible(false)
		valueEdit_.setVisible(true)
		valueEdit_.requestFocus
	})

	def endEdit(_save: Boolean) = fx({
		editing = true
		if (_save && valueEdit_.getText.trim.length > 0)
			value = valueEdit_.getText.trim
		else
			valueEdit_.setText(value)
		valueEdit_.setVisible(false)
		valueLabel_.setVisible(true)
		editing = false
	})
}
