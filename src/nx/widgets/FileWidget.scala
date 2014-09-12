package nx.widgets

import java.io.File
import javafx.scene.control.Tooltip
import javafx.scene.input.MouseEvent

import nx.settings.StringSetting
import nx.util.{JSON, InterfaceShortcuts, Tools}

class FileWidget(_file: File) extends Widget with Content[File] with Tools with InterfaceShortcuts
{
	getStyleClass.add("fileWidget")

	def this(_file: File, _onClick: => Unit) =
	{
		this(_file)
		onClick = _onClick
	}

	def getContent = content: Array[Byte]

	protected val name = new StringSetting("", content.getName, "/nx/res/file.png", scale)
	name.editable = false

	getChildren.add(name)

	val tt = new Tooltip(content.getPath)
	setOnMouseEntered(handle[MouseEvent](tt.show(window)))
	setOnMouseExited(handle[MouseEvent](tt.hide))

	def toJSON: JSON =
	{
		val toReturn = JSON()

		toReturn += JSON("guid", guid: String)
		toReturn += JSON("type", "FileWidget")
		toReturn += JSON("name", name.value)
		toReturn += JSON("file", content.getPath)

		toReturn
	}
}
object FileWidget
{
	def fromJSON(_json: JSON): FileWidget =
	{
		null
	}
}