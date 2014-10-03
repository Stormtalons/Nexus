package nx.widgets

import java.io.File
import javafx.event.ActionEvent
import javafx.geometry.{HPos, Pos, VPos}
import javafx.scene.Node
import javafx.scene.control.{ContextMenu, MenuItem}
import javafx.scene.image.Image
import javafx.scene.input._
import javafx.scene.layout._
import javafx.stage.FileChooser

import nx.comm.sendable.Sendable
import nx.settings.StringSetting
import nx.util.{JSON, Tools}
import sw.common.ui.UIShortcuts

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

class FolderWidget extends Widget with Tools with UIShortcuts
{
	getStyleClass.add("folderWidget")

	def this(_name: String) =
	{
		this
		name = _name
	}
	def this(_file: File) =
	{
		this(_file.getName)
		if (_file.isDirectory)
			_file.listFiles.foreach(_f => addWidget(new FileWidget(_f)))
	}

	private var background_ : Image = null
	def background = background_
	def background_=(_img: Image): Unit =
	{
		background_ = _img
		widgets.setBackground(new Background(new BackgroundImage(background_,
			BackgroundRepeat.REPEAT,
			BackgroundRepeat.REPEAT,
			BackgroundPosition.DEFAULT,
			BackgroundSize.DEFAULT)))
	}
	def background_=(_str: String): Unit = background = new Image(_str)

	private val name_ = new StringSetting("", "New Folder", "/nx/res/folder.png", scale)
	def name = name_.value
	def name_=(_name: String) = name_.value = _name

	protected val contentPane = new VBox
	
	protected val header = new HBox
	header.getChildren.add(name_)
	header.setOnMouseClicked(handleEvt[MouseEvent](_evt =>
		if (_evt.getClickCount == 2) {if (!isExpanded) expand else collapse}
		else {requestFocus;_evt.consume}))
	header.setAlignment(Pos.CENTER_LEFT)
	contentPane.getChildren.add(header)
	getChildren.add(contentPane)

	protected val headerMenu = new ContextMenu
	protected val remove = new MenuItem("Delete")
	remove.setOnAction(handle[ActionEvent](() => desktop.removeWidget(FolderWidget.this)))

	headerMenu.getItems.addAll(remove)
	header.setOnContextMenuRequested(handleEvt[ContextMenuEvent](_evt =>
		if (_evt.getSource.isInstanceOf[Node])
		{
			headerMenu.show(_evt.getSource.asInstanceOf[Node], _evt.getScreenX, _evt.getScreenY)
			_evt.consume
		}))

	protected var (rowMax, colMax) = (0, 0)
	protected val widgets: GridPane = new GridPane
	VBox.setVgrow(widgets, Priority.ALWAYS)
	widgets.setOnDragOver(handleEvt[DragEvent](_evt => if (_evt.getDragboard.hasContent(DataFormat.FILES)) _evt.acceptTransferModes(TransferMode.MOVE)))
	widgets.setOnDragDropped(handleEvt[DragEvent](_evt =>
	{
		val row = math.floor(_evt.getY / scale).toInt
		val col = math.floor(_evt.getX / scale).toInt

		val list = _evt.getDragboard.getContent(DataFormat.FILES).asInstanceOf[java.util.List[File]]
		for (i <- 0 to list.size - 1)
		{
			val toAdd: Widget =
				if (list.get(i).isFile)
					new FileWidget(list.get(i))
				else
					new FolderWidget(list.get(i))
			if (!addWidget(toAdd, row, col))
				addWidget(toAdd)
		}
		_evt.consume
	}))
	def getWidgets[T <: Widget:ClassTag]: ArrayBuffer[T] =
	{
		var toReturn = new ArrayBuffer[T]
		for (i <- 0 to widgets.getChildren.size - 1)
			widgets.getChildren.get(i) match {
				case _t: T => toReturn += _t
				case _ =>
			}
		toReturn
	}
	def getWidgetBounds(_w: Widget): (Int, Int, Int, Int) =
	{
		var (left, right, upper, lower) = (-1, -1, -1, -1)
		if (widgets.getChildren.contains(_w))
		{
			left = GridPane.getColumnIndex(_w)
			right = tryGet[Int]({GridPane.getColumnSpan(_w) + left - 1}, left)
			upper = GridPane.getRowIndex(_w)
			lower = tryGet[Int]({GridPane.getRowSpan(_w) + upper - 1}, upper)
		}
		(left, right, upper, lower)
	}
	def getWidgetAt(_row: Int, _col: Int): Widget =
	{
		getWidgets[Widget].foreach(_w =>
		{
			val (left, right, upper, lower) = getWidgetBounds(_w)
			if (_col >= left && _col <= right && _row >= upper && _row <= lower)
				return _w
		})
		null
	}
	def removeWidget(_w: Widget): Boolean =
		if (widgets.getChildren.contains(_w))
		{
			widgets.getChildren.remove(_w).fx
			true
		}
		else
		{
			getWidgets[FolderWidget].foreach(_w => _w match {case _fw: FolderWidget => if (_fw.removeWidget(_w)) return true; case _ =>})
			false
		}
	def addWidget(_w: Widget): Unit =
	{
		for (i <- 0 to rowMax)
			for (j <- 0 to colMax)
				if (getWidgetAt(i, j) == null)
				{
					widgets.add(_w, j, i)
					confirmConstraints
					return
				}
		val (newCol, newRow) = if (colMax >= 15) (0, rowMax + 1) else (colMax + 1, rowMax)
		widgets.add(_w, newCol, newRow)
		confirmConstraints
	}
	def addWidget(_w: Widget, _row: Int, _col: Int): Boolean =
		if (getWidgetAt(_row, _col) == null)
		{
			widgets.add(_w, _col, _row)
			confirmConstraints
			true
		}
		else
			false

	def confirmConstraints: Unit = confirmConstraintsFor(0, 0)
	def confirmConstraintsFor(_rowMax: Int, _colMax: Int): Unit =
	{
		rowMax = _rowMax
		colMax = _colMax
		getWidgets[Widget].foreach(_w =>
		{
			colMax = math.max(colMax, GridPane.getColumnIndex(_w) + tryGet(GridPane.getColumnSpan(_w) - 1, 0))
			rowMax = math.max(rowMax, GridPane.getRowIndex(_w) + tryGet(GridPane.getRowSpan(_w) - 1, 0))
		})

		val rc = new RowConstraints
		rc.setMaxHeight(scale)
		rc.setMinHeight(scale)
		rc.setValignment(VPos.TOP)
		for (i <- widgets.getRowConstraints.size to rowMax)
			widgets.getRowConstraints.add(rc)

		val cc = new ColumnConstraints
		cc.setMaxWidth(scale)
		cc.setMinWidth(scale)
		cc.setHalignment(HPos.LEFT)
		for (i <- widgets.getColumnConstraints.size to colMax)
			widgets.getColumnConstraints.add(cc)
	}
	confirmConstraintsFor(5, 5)

	def expand: Unit = expand(true)
	def expand(_keepThumbnail: Boolean): Unit =
	{
		if (!_keepThumbnail)
			contentPane.getChildren.remove(header)
		contentPane.getChildren.add(widgets)
		GridPane.setColumnSpan(this, 5)
		GridPane.setRowSpan(this, 5)
		if (!name.equals("Desktop"))
		{
			setStyle("-fx-background-color: rgb(100, 100, 100, 0.5)")
			header.setStyle("-fx-background-color: rgb(0, 0, 0, 0.3)")
		}
	}.fx

	def collapse =
	{
		if (!contentPane.getChildren.contains(header))
			contentPane.getChildren.add(header)
		contentPane.getChildren.remove(widgets)
		GridPane.setColumnSpan(this, 1)
		GridPane.setRowSpan(this, 1)
		if (!name.equals("Desktop"))
		{
			setStyle("-fx-background-color: transparent")
			header.setStyle("-fx-background-color: transparent")
		}
	}.fx

	def isExpanded: Boolean = contentPane.getChildren.contains(widgets)

	protected val widgetMenu = new ContextMenu
	protected val addFolder = new MenuItem("Add Folder")
	addFolder.setOnAction(addWidget(new FolderWidget("New Folder")))
	protected val setBackgroundItem = new MenuItem("Choose Background")
	setBackgroundItem.setOnAction(
	{
		val bg = new FileChooser().showOpenDialog(window)
		if (bg != null)
			background = new Image(bg.toURI.toURL.toString)
	})
	widgetMenu.getItems.addAll(addFolder, setBackgroundItem)
	widgets.setOnContextMenuRequested(handleEvt[ContextMenuEvent](_evt =>
		if (_evt.getSource.isInstanceOf[Node])
		{
			widgetMenu.show(_evt.getSource.asInstanceOf[Node], _evt.getScreenX, _evt.getScreenY)
			_evt.consume
		}))

	def toSendable =
	{
		val items = new ArrayBuffer[Sendable[_]]
		val toReturn = JSON()

		toReturn += JSON("guid", guid: String)
		toReturn += JSON("type", "FolderWidget")
		toReturn += JSON("name", name)
		toReturn += JSON("expanded", isExpanded.toString)
		if (background != null)
		{
			val si = new Sendable(background, Sendable.typeToGUID[Image])
			items += si
			toReturn += JSON("background", si.guid: String)
		}

		val widgets = getWidgets[Widget]
		if (widgets.length > 0)
		{
			val childWidgets = new ArrayBuffer[JSON]
			widgets.foreach(_w =>
			{
				val (json, widgetItems) = _w.toSendable
				childWidgets += json
				items ++= widgetItems
			})
			toReturn += JSON("widgets", childWidgets)
		}

		(toReturn, items.toArray)
	}
}
object FolderWidget
{
	def fromJSON(_json: JSON): FolderWidget =
	{
		null
	}
}