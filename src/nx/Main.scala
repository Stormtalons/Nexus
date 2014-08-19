package nx

import javafx.application.Platform
import javafx.event.{ActionEvent, EventHandler}
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.image.{Image, PixelFormat, WritableImage}
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
//	var ar1 = Files.readAllBytes(Paths.get("test.txt"))
//	ar1 = ar1.drop(5)
//	val a1 = new Array[Byte](1024 * 10)
//	var ar2 = Files.readAllBytes(Paths.get("test2.txt"))
//	ar2 = ar2.drop(8)
//	val a2 = new Array[Byte](1024 * 10)
//	for (i <- 0 to a1.length - 1)
//	{
//		a1(i) = ar1(i)
//		a2(i) = ar2(i)
//	}
//	println(ar1.length)
//	println(ar2.length)
//	var mis = 0
//	for (i <- 0 to ar2.length - 1)
//		if (ar1(i) != ar2(i))
//			mis += 1
//	println(mis + " mismatched data points")
//
//	Files.write(Paths.get("out1.txt"), a1)
//	Files.write(Paths.get("out2.txt"), a2)
//
//	System.exit(0)
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
			var bytes = Array[Byte]()
			for (b <- data.split(","))
				bytes = bytes :+ b.toByte
			val bgimg = new WritableImage(w, h)
			println("writing image")
			bgimg.getPixelWriter.setPixels(0, 0, w, h, PixelFormat.getByteBgraInstance, bytes, 0, 0)
			println("written image")
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
		devToolbar.getChildren.addAll(newFolder, json, cnct)

		mainPanel.getChildren.add(devToolbar)

		stg.addEventFilter(WindowEvent.WINDOW_HIDING, new EventHandler[WindowEvent]{def handle(_evt: WindowEvent) = peerManager.stop})
		stg.setScene(new Scene(mainPanel))
		stg.show

		peerManager = new PeerManager
		peerManager.run
	}
}