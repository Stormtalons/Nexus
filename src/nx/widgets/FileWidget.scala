package nx.widgets

import java.io.File
import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.{MenuItem, ContextMenu, Tooltip}
import javafx.scene.input.{ContextMenuEvent, MouseEvent}

import nx.comm.sendable.{RemoteFile, Sendable}
import nx.settings.StringSetting
import nx.util.{JSON, InterfaceShortcuts, Tools}

class FileWidget(_fileName: String) extends Widget with Content[RemoteFile] with Tools with InterfaceShortcuts
{
	getStyleClass.add("fileWidget")

	def this(_file: File) =
	{
		this(_file.getName)
		content.obj.fileData = _file: Array[Byte]
	}

	var fileName = _fileName
	
	var content = new Sendable[RemoteFile](new RemoteFile(fileName))

	protected val name = new StringSetting("", fileName, "/nx/res/file.png", scale)
	name.editable = false

	getChildren.add(name)

	protected val rightClickMenu = new ContextMenu
	protected val download = new MenuItem("Download")
	download.setOnAction(
	{
		if (!content.obj.hasFileData)
		{
			//TODO: Figure out async file transfer mechanism.
		}
	})
	rightClickMenu.getItems.addAll(download)
	setOnContextMenuRequested(handleEvt[ContextMenuEvent](_evt =>
		if (_evt.getSource.isInstanceOf[Node])
		{
			rightClickMenu.show(_evt.getSource.asInstanceOf[Node], _evt.getScreenX, _evt.getScreenY)
			_evt.consume
		}))

	def toSendable =
	{
		val toReturn = JSON()

		toReturn += JSON("guid", guid: String)
		toReturn += JSON("type", "FileWidget")
		toReturn += JSON("name", name.value)
		toReturn += JSON("file", content.guid: String)

		(toReturn, Array(content))
	}
}
object FileWidget
{
	def fromJSON(_json: JSON): FileWidget =
	{
		null
	}
}