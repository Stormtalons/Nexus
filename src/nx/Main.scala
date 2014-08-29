package nx

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileWriter}
import java.net.ServerSocket
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

import nx.comm.ConnectionManager
import nx.widgets.{FileWidget, FolderWidget}

import scala.collection.mutable.ArrayBuffer

trait Repeat
{
	case class Repeat(_val: AnyVal)
	{def ^(_i: Int): String = {var s = _val.toString;for (i <- 2 to _i) s+=_val;s}}

	implicit def toRpt(_val: Byte) = Repeat(_val.asInstanceOf[AnyVal])
	implicit def toRpt(_val: Int) = Repeat(_val.asInstanceOf[AnyVal])
	implicit def toRpt(_val: Long) = Repeat(_val.asInstanceOf[AnyVal])
	implicit def toRpt(_val: Float) = Repeat(_val.asInstanceOf[AnyVal])
	implicit def toRpt(_val: Double) = Repeat(_val.asInstanceOf[AnyVal])
	implicit def toRpt(_val: Char) = Repeat(_val.asInstanceOf[AnyVal])
	implicit def toRpt(_val: String) = Repeat(_val.asInstanceOf[AnyVal])
}

object Main extends App with Repeat
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
	var serverPort = 19265

//	Dev tools
	var useConfig = true
	var instance = 1
	try
	{
		val ts = new ServerSocket(serverPort)
		ts.close
		println("Host instance initialized")
	}
	catch
	{
		case e: Exception =>
			useConfig = false
			instance = 2
			println("Host instance initialized")
	}
	val logFile = new File(s"log$instance.log")
	if (!logFile.exists)
		logFile.createNewFile
	val logWriter = new FileWriter(logFile, true)
//	/Dev tools

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
		AnchorPane.setLeftAnchor(desktop_, 0d)
		AnchorPane.setRightAnchor(desktop_, 0d)
		AnchorPane.setTopAnchor(desktop_, 0d)
		AnchorPane.setBottomAnchor(desktop_, 0d)
		desktop_.confirmConstraintsFor(5, 10)
		desktop_.expand(false)
		desktop_.setPrefSize(1024, 768)
	}
	def setDesktopBackground(_file: String) = desktop.background = new Image(_file)
	
	new Main().launch
	
	def serialize: String = desktop.toJSON.condensed
	def saveState = if (useConfig) Files.write(Paths.get("config.ini"), serialize)
	def loadState(_json: JSON) = fx({
		desktop = new FolderWidget(_json.get[String]("name"))
		val bg = _json.get[String]("background")
		if (bg != null)
			desktop.background = _json.get[String]("background").split(",").map(_.toByte)
		def addWidgets(_parent: FolderWidget, _widgets: ArrayBuffer[JSON]): Unit =
			for (_w <- _widgets)
				if (!_w.has[String]("type"))
					println("JSON:\n" + _json)
				else if (_w.get[String]("type").equals("FolderWidget"))
				{
					val fw = new FolderWidget(_w.get[String]("name"))
					_parent.addWidget(fw)
					if (_w.has[ArrayBuffer[JSON]]("widgets"))
						addWidgets(fw, _w.get[ArrayBuffer[JSON]]("widgets"))
				}
				else if (_w.get[String]("type").equals("FileWidget"))
				{
					val fw = new FileWidget(new File(_w.get[String]("file")))
					_parent.addWidget(fw)
				}
		if (_json.has[ArrayBuffer[JSON]]("widgets"))
			addWidgets(desktop, _json.get[ArrayBuffer[JSON]]("widgets"))
	})

	def log(_str: String, _lines: Int = 1) = t(logWriter.write("Instance ${instance} - ${new SimpleDateFormat(\"yyyyMMdd.hh:mm:ss\").format(Calendar.getInstance.getTime)}: ${_str}${'\n'^_lines}"))
	def t(_code: => Unit, _exHandler: (Exception) => Unit = null): Boolean = try {_code;true} catch {case e: Exception => _exHandler(e);false}
	def tg[T <: Any](_code: => T, _dft: T = null): T = try _code catch {case e: Exception => _dft}
	def ex(_code: => Unit) = new Thread(new Runnable {def run = _code}).start
	def fx(_code: => Unit) = if (Platform.isFxApplicationThread) t(_code) else Platform.runLater(new Runnable {def run = t(_code)})
}

class Main extends javafx.application.Application
{import nx.Main._

	def launch = javafx.application.Application.launch()

	var connManager: ConnectionManager = null

	var mainPanel: VBox = null

	def start(stg: Stage)
	{
		log("Application start")
		window = stg
		mainPanel = new VBox

		desktopPanel = new AnchorPane
		mainPanel.getChildren.add(desktopPanel)

		if (Files.exists(Paths.get("config.ini")) && useConfig)
			loadState(Files.readAllBytes(Paths.get("config.ini")): String)
		else
		{
			desktop = new FolderWidget("Desktop")
			setDesktopBackground("/nx/res/background.png")
		}

		val devToolbar = new HBox
		val newFolder = new Button("Add Folder")
		newFolder.setOnAction(new EventHandler[ActionEvent]{def handle(_evt: ActionEvent) = desktop.addWidget(new FolderWidget)})
		val json = new Button("Desktop To JSON")
		json.setOnAction(new EventHandler[ActionEvent]{def handle(_evt: ActionEvent) = log(desktop.toJSON + "\n\n")})
		val cnct = new Button("Connect to self")
		cnct.setOnAction(new EventHandler[ActionEvent]{def handle(_evt: ActionEvent) = connManager.connect("127.0.0.1", serverPort)})
		val test = new Button("Test")
		test.setOnAction(new EventHandler[ActionEvent]{def handle(_evt: ActionEvent) = Files.write(Paths.get("test.txt"), "DESKTOP" + sep + serialize + eom)})
		devToolbar.getChildren.addAll(newFolder, json, cnct, test)

		mainPanel.getChildren.add(devToolbar)

		stg.addEventFilter(WindowEvent.WINDOW_HIDING, new EventHandler[WindowEvent]{def handle(_evt: WindowEvent) =
		{
			log("Stopping connection manager")
			connManager.stop
			
			saveState
		}})
		stg.setScene(new Scene(mainPanel))
		stg.show

		connManager = new ConnectionManager
		connManager.run
	}
}