package nx

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.file.{Files, Paths}
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.event.{ActionEvent, EventHandler}
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.layout.{AnchorPane, HBox, VBox}
import javafx.stage.{Stage, Window, WindowEvent}
import javax.imageio.ImageIO

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
	implicit def strToByteArray(_str: String): Array[Byte] = _str.getBytes("UTF-8")
	implicit def byteArrayToStr(_bytes: Array[Byte]): String = new String(_bytes, "UTF-8")
	implicit def strToJSON(_str: String): JSON = JSON.parse(_str)
	implicit def byteArrayToImg(_bytes: Array[Byte]): Image = SwingFXUtils.toFXImage(ImageIO.read(new ByteArrayInputStream(_bytes)), null)
	implicit def imgToByteArray(_img: Image): Array[Byte] =
	{
		val baos = new ByteArrayOutputStream
		ImageIO.write(SwingFXUtils.fromFXImage(_img, null), "jpg", baos)
		baos.toByteArray
	}

	val sep = "<@@>"
	val eom = sep + "EM"
	val bufferSize = 8192
	def eom(_i: Int): Char = eom.charAt(_i)
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
	def setDesktopBackground(_file: String) = desktop.background = new Image(_file)
	
	new Main().launch
	
	def serialize: String = desktop.toJSON.condensed

	def saveState =
	{

	}

	def loadState(_json: JSON) =
	{
		fx({
			desktop = new FolderWidget(_json.get[String]("name"))
			val bg = _json.get[String]("background")
			if (bg != null)
				desktop.background = bg.split(",").map(_.toByte)
			AnchorPane.setLeftAnchor(desktop, 0d)
			AnchorPane.setRightAnchor(desktop, 0d)
			AnchorPane.setTopAnchor(desktop, 0d)
			AnchorPane.setBottomAnchor(desktop, 0d)
			desktop.confirmConstraintsFor(5, 10)
			desktop.expand(false)
			desktop.setPrefSize(1024, 768)
		})
	}

	def log(_str: String) = println("Logger: " + _str)
	def run(code: => Unit) = new Thread(new Runnable {def run = code}).start
	def fx(code: => Unit) = Platform.runLater(new Runnable {def run = code})
}

class Main extends javafx.application.Application
{
	import nx.Main._

	def launch = javafx.application.Application.launch()

	var peerManager: PeerManager = null

	var mainPanel: VBox = null

	def start(stg: Stage)
	{
		window = stg
		mainPanel = new VBox

		desktopPanel = new AnchorPane
		mainPanel.getChildren.add(desktopPanel)

		desktop = new FolderWidget("Desktop")
		setDesktopBackground("/nx/res/background.png")
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
		json.setOnAction(new EventHandler[ActionEvent]{def handle(_evt: ActionEvent) = log(desktop.toJSON + "\n\n")})
		val cnct = new Button("Connect to self")
		cnct.setOnAction(new EventHandler[ActionEvent]{def handle(_evt: ActionEvent) = peerManager.connect("127.0.0.1", 19265)})
		val test = new Button("Test")
		test.setOnAction(new EventHandler[ActionEvent]{def handle(_evt: ActionEvent) = Files.write(Paths.get("test.txt"), "DESKTOP" + sep + serialize + eom)})
		devToolbar.getChildren.addAll(newFolder, json, cnct, test)

		mainPanel.getChildren.add(devToolbar)

		stg.addEventFilter(WindowEvent.WINDOW_HIDING, new EventHandler[WindowEvent]{def handle(_evt: WindowEvent) = peerManager.stop})
		stg.setScene(new Scene(mainPanel))
		stg.show

		peerManager = new PeerManager
		peerManager.run
	}
}