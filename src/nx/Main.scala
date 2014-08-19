package nx

import java.nio.file.{Files, Paths}
import javafx.application.Platform
import javafx.event.{ActionEvent, EventHandler}
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.image.{WritableImage, Image}
import javafx.scene.layout.{AnchorPane, HBox, VBox}
import javafx.stage.{Stage, Window, WindowEvent}

import nx.comm.PeerManager
import nx.widgets.FolderWidget

object Repeat
{
	implicit def toRpt(_val: Byte) = Repeat(_val.asInstanceOf[AnyVal])
	implicit def toRpt(_val: Int) = Repeat(_val.asInstanceOf[AnyVal])
	implicit def toRpt(_val: Long) = Repeat(_val.asInstanceOf[AnyVal])
	implicit def toRpt(_val: Float) = Repeat(_val.asInstanceOf[AnyVal])
	implicit def toRpt(_val: Double) = Repeat(_val.asInstanceOf[AnyVal])
	implicit def toRpt(_val: Char) = Repeat(_val.asInstanceOf[AnyVal])
	implicit def toRpt(_val: String) = Repeat(_val.asInstanceOf[AnyVal])
}
case class Repeat(_val: AnyVal)
{def ^(_i: Int): String = {var s = _val.toString;for (i <- 2 to _i) s+=_val;s}}

object Main extends App
{
	val fn = 2
	var window: Window = null
	var desktopPanel: AnchorPane = null
	var desktop_ : FolderWidget = null
	def desktop = desktop_
	def desktop_=(_fw: FolderWidget) =
	{
		if (desktop_ != null)
			desktopPanel.getChildren.remove(desktop_)
		desktop_ = _fw
		desktopPanel.getChildren.add(0, desktop_)
	}
//	def setDesktopBackground(_file: String) = desktop.setStyle("-fx-background-image: url(\"" + _file + "\")")
	def setDesktopBackground(_file: String) = {println(_file);desktop.background = new Image(_file)}
	
	new Main().launch
	
	def serialize: String = desktop.toJSON.condensed

	def saveState =
	{

	}

	def loadState(_json: JSON) =
	{
		fx({
			println("loading state")
			desktop = new FolderWidget(_json.get[String]("name"))
			val bg = _json.get[JSON]("background")
			val w = bg.get[String]("width").toDouble.toInt
			val h = bg.get[String]("height").toDouble.toInt
			val data = bg.get[String]("data")
			Files.write(Paths.get("temp.txt"), data.getBytes)
			var bytes = data.split(",")
			val bgimg = new WritableImage(w, h)
			var counter = 0
			for (i <- 0 to h - 1)
				for (j <- 0 to w - 1)
				{
					bgimg.getPixelWriter.setArgb(j, i, bytes(counter).toInt)
					counter += 1
				}
			desktop.background = bgimg
			AnchorPane.setLeftAnchor(desktop, 0d)
			AnchorPane.setRightAnchor(desktop, 0d)
			AnchorPane.setTopAnchor(desktop, 0d)
			AnchorPane.setBottomAnchor(desktop, 0d)
			desktop.confirmConstraintsFor(5, 10)
			desktop.expand(false)
			desktop.setPrefSize(1024, 768)
		})
	}

	def run(code: => Unit) = new Thread(new Runnable {def run = code}).start
	def fx(code: => Unit) = Platform.runLater(new Runnable {def run = code})
}

class Main extends javafx.application.Application
{
	def launch = javafx.application.Application.launch()
	def desktop = Main.desktop
	def desktop_=(_fw: FolderWidget) = Main.desktop = _fw

	var peerManager: PeerManager = null

	var mainPanel: VBox = null

	def start(stg: Stage)
	{
		Main.window = stg
		mainPanel = new VBox

		Main.desktopPanel = new AnchorPane
		mainPanel.getChildren.add(Main.desktopPanel)

		desktop = new FolderWidget("Desktop")
		Main.setDesktopBackground("/nx/res/background.png")
		AnchorPane.setLeftAnchor(desktop, 0d)
		AnchorPane.setRightAnchor(desktop, 0d)
		AnchorPane.setTopAnchor(desktop, 0d)
		AnchorPane.setBottomAnchor(desktop, 0d)
		desktop.confirmConstraintsFor(5, 10)
		desktop.expand(false)
		desktop.setPrefSize(1024, 768)

		val devToolbar = new HBox
		val newFolder = new Button("Add Folder")
		newFolder.setOnAction(new EventHandler[ActionEvent]{def handle(_evt: ActionEvent) = desktop.addWidget(new FolderWidget)})
		val json = new Button("Desktop To JSON")
		json.setOnAction(new EventHandler[ActionEvent]{def handle(_evt: ActionEvent) = println(desktop.toJSON + "\n\n")})
		val cnct = new Button("Connect to self")
		cnct.setOnAction(new EventHandler[ActionEvent]{def handle(_evt: ActionEvent) = peerManager.connect("127.0.0.1", 19265)})
		val test = new Button("Test")
		test.setOnAction(new EventHandler[ActionEvent]{def handle(_evt: ActionEvent) = Files.write(Paths.get("test.txt"), ("DESKTOP|" + Main.serialize + "|EM").getBytes)})
		devToolbar.getChildren.addAll(newFolder, json, cnct, test)

		mainPanel.getChildren.add(devToolbar)

		stg.addEventFilter(WindowEvent.WINDOW_HIDING, new EventHandler[WindowEvent]{def handle(_evt: WindowEvent) = peerManager.stop})
		stg.setScene(new Scene(mainPanel))
		stg.show

		peerManager = new PeerManager
		peerManager.run
	}
}