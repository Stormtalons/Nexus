package nx.widgets

import java.io.File
import java.nio.file.{Files, Paths}
import javafx.event.EventHandler
import javafx.scene.control.Tooltip
import javafx.scene.input.MouseEvent

import nx.JSON
import nx.settings.StringSetting

class FileWidget(_file: File) extends Widget with Content[File]
{import nx.Main._
	getStyleClass.add("fileWidget")

	def this(_file: File, _onClick: => Unit) =
	{
		this(_file)
		onClick = _onClick
	}

	var content: File = _file
	def getContent: Array[Byte] = if (content == null || !content.exists) Array[Byte]() else Files.readAllBytes(Paths.get(content.getPath))

	protected val fileName = new StringSetting("", content.getName, "", scale)
	fileName.editable = false

	getChildren.add(fileName)

	//Temporary, in lieu of loading thumbnail
	setStyle("-fx-background-color: green")
	val tt = new Tooltip(content.getPath)
	setOnMouseEntered(new EventHandler[MouseEvent]{def handle(_evt: MouseEvent) = tt.show(window)})
	setOnMouseExited(new EventHandler[MouseEvent]{def handle(_evt: MouseEvent) = tt.hide})

	def toJSON: JSON =
	{
		val toReturn = JSON()

		toReturn += JSON("type", "FileWidget")
		toReturn += JSON("name", fileName.value)
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