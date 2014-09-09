package nx

import java.awt.event.ActionListener
import java.awt.{MenuItem, PopupMenu, SystemTray, TrayIcon}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileWriter}
import java.net._
import java.nio.file.{Files, Path, Paths}
import java.text.SimpleDateFormat
import java.util.regex.Pattern
import java.util.{Calendar, EventListener, UUID}
import javafx.application.{Application, Platform}
import javafx.embed.swing.SwingFXUtils
import javafx.event.{ActionEvent, Event, EventHandler, EventType}
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.layout.{AnchorPane, HBox, VBox}
import javafx.scene.{Node, Scene}
import javafx.stage.{Stage, Window, WindowEvent}
import javax.imageio.ImageIO

import nx.comm.{ConnectionManager, SendableOld}
import nx.widgets.{FileWidget, FolderWidget}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._
//import scala.reflect.runtime.{universe => ru}

trait InterfaceShortcuts
{
	def addFilter[T <: Event](_node: Window, _evt: EventType[T], _code: => Unit) = _node.addEventFilter(_evt, handle[T](_code))
	def addHandler[T <: Event](_node: Window, _evt: EventType[T], _code: => Unit) = _node.addEventHandler(_evt, handle[T](_code))
	def addFilter[T <: Event](_node: Node, _evt: EventType[T], _code: => Unit) = _node.addEventFilter(_evt, handle[T](_code))
	def addHandler[T <: Event](_node: Node, _evt: EventType[T], _code: => Unit) = _node.addEventHandler(_evt, handle[T](_code))
	def handleEvt[T <: Event](_code: T => Unit): EventHandler[T] = new EventHandler[T]{def handle(_evt: T) = _code(_evt)}
	def handle[T <: Event](_code: => Unit): EventHandler[T] = new EventHandler[T]{def handle(_evt: T) = _code}

	def addListener[T <: EventListener](_code: () => Unit)(implicit tag: TypeTag[T]): T =
	{
		tag match
		{
			case ae: java.awt.event.ActionListener => new ActionListener {def actionPerformed(e: java.awt.event.ActionEvent) = _code()}
			case _ => null
		}
	}.asInstanceOf[T]
}

trait Util
{import scala.language.implicitConversions

	case class Repeat(_val: Any){def ^(_i: Int): String = {var s = "";for (i <- 0 until _i) s += _val.toString;s}}
	implicit def toRpt(_val: Byte) = Repeat(_val.asInstanceOf[Any])
	implicit def toRpt(_val: Int) = Repeat(_val.asInstanceOf[Any])
	implicit def toRpt(_val: Long) = Repeat(_val.asInstanceOf[Any])
	implicit def toRpt(_val: Float) = Repeat(_val.asInstanceOf[Any])
	implicit def toRpt(_val: Double) = Repeat(_val.asInstanceOf[Any])
	implicit def toRpt(_val: Char) = Repeat(_val.asInstanceOf[Any])
	implicit def toRpt(_val: String) = Repeat(_val.asInstanceOf[Any])

	case class Screen[T <: Any](_p: T)
	{
		def ^# : T = ^#(1)
		def ^#(_crlf: Int) : T = {print(_p + ("\n"^_crlf));_p}
		def ^@ : T = {print(_p);_p}
	}
	implicit def anyToScreen[T <: Any](_p: T): Screen[T] = Screen(_p)

	def serverPort = Main.serverPort_
	def serverPort_=(_port: Int) = Main.serverPort_ = _port
	def bufferSize = Main.bufferSize_
	def bufferSize_=(_size: Int) = Main.bufferSize_ = _size
	val sep = "<@@>"
	val eom = sep + "EM"
	def eom(_i: Int): Char = eom.charAt(_i)

//	<Dev tools>
	def useConfig = Main.useConfig_
	def useConfig_=(_config: Boolean) = Main.useConfig_ = _config
	def instance = Main.instance_
	def instance_=(_instance: Int) = Main.instance_ = _instance
	def logWriter = Main.logWriter_
	//TODO: LOG NEEDS HELP!! No line breaks.
	def log(_str: String, _lines: Int = 1) = tryDo(synchronized{logWriter.write(s"Instance ${instance} - ${new SimpleDateFormat("yyyyMMdd.hh:mm:ss").format(Calendar.getInstance.getTime)}: ${_str}${'\n'^_lines}")})
//	</Dev tools>
	
	def window = Main.window_
	def window_=(_window: Window) = Main.window_ = _window
	def desktop = Main.desktop_
	def desktop_=(_desktop: FolderWidget) =
	{
		if (desktop != null)
			desktopPanel.getChildren.remove(desktop)
		Main.desktop_ = _desktop
		desktopPanel.getChildren.add(0, desktop)
		AnchorPane.setLeftAnchor(desktop, 0d)
		AnchorPane.setRightAnchor(desktop, 0d)
		AnchorPane.setTopAnchor(desktop, 0d)
		AnchorPane.setBottomAnchor(desktop, 0d)
		desktop.confirmConstraintsFor(5, 10)
		desktop.expand(false)
		desktop.setPrefSize(1024, 768)
	}
	def desktopPanel = Main.desktopPanel_
	def desktopPanel_=(_desktopPanel: AnchorPane) = Main.desktopPanel_ = _desktopPanel

	implicit def bytesToStr(_bytes: Array[Byte]): String = new String(_bytes, "UTF-8")
	implicit def bytesToImg(_bytes: Array[Byte]): Image = SwingFXUtils.toFXImage(ImageIO.read(new ByteArrayInputStream(_bytes)), null)

	implicit def strToBytes(_str: String): Array[Byte] = _str.getBytes("UTF-8")
	implicit def strToPath(_str: String): Path = Paths.get(_str)
//	implicit def strToJSON(_str: String): JSON = JSON.parse(_str)
	implicit def strToUUID(_str: String): UUID = UUID.fromString(_str)
	implicit def strToImg(_str: String): Image = SwingFXUtils.toFXImage(ImageIO.read(getClass.getResource(_str)), null).asInstanceOf[Image]

	implicit def pathToBytes(_path: Path): Array[Byte] = if (fileExists(_path)) Files.readAllBytes(_path) else Array[Byte]()
	implicit def pathToStr(_path: Path): String = _path: Array[Byte]

	implicit def UUIDToBytes(_uuid: UUID): Array[Byte] = _uuid.toString: Array[Byte]
	implicit def UUIDToStr(_uuid: UUID): String = _uuid.toString

	implicit def fileToBytes(_file: File): Array[Byte] = if (fileExists(_file)) _file: Path else Array[Byte]()
	implicit def fileToStr(_file: File): String = if (fileExists(_file)) _file: Array[Byte] else ""
	implicit def fileToPath(_file: File): Path = if (fileExists(_file)) _file.getPath: Path else null

	implicit def imgToBytes(_img: Image): Array[Byte] =
	{
		val baos = new ByteArrayOutputStream
		ImageIO.write(SwingFXUtils.fromFXImage(_img, null), "jpg", baos)
		baos.toByteArray
	}

	def fileExists(_file: File): Boolean = _file != null && _file.exists
	def fileExists(_path: Path): Boolean = Files.exists(_path)
	def fileExists(_filePath: String): Boolean = fileExists(_filePath: Path)
	def fileText(_filePath: String): String = (_filePath: Path): Array[Byte]
	def toFile(_filePath: String, _bytes: Array[Byte]): Unit = Files.write(_filePath: Path, _bytes)
	def toFile(_filePath: String, _str: String): Unit = toFile(_filePath, _str: Array[Byte])
	def tryDo(_code: => Unit, _exHandler: Exception => Unit = null): Boolean = try {_code;true} catch {case e: Exception => if (_exHandler != null){_exHandler(e)};false}
	def tryGet[T <: Any](_code: => T, _dft: => T = null): T = try _code catch {case e: Exception => _dft}
	def ex(_code: => Unit) = new Thread(new Runnable {def run = _code}).start
	def fx(_code: => Unit) = if (Platform.isFxApplicationThread) tryDo(_code) else Platform.runLater(new Runnable {def run = tryDo(_code)})
	def regex[T <: Any](_regex: String, _input: T) =
	{
		val m = Pattern.compile(_regex).matcher(_input.toString)
		if (m.find) m.group else ""
	}
}

class SOMaker[T <: Any]()(implicit manifest: Manifest[SendableOld[T]])
{
	def make: SendableOld[T] = manifest.erasure.newInstance.asInstanceOf[SendableOld[T]]
}
object Main extends App with Util with InterfaceShortcuts
{
//	val runtimeMirror = ru.runtimeMirror(classOf[SendableObject[_]].getClassLoader)
//	val classMirror = runtimeMirror.reflectClass(runtimeMirror.classSymbol(runtimeMirror.runtimeClass(ru.typeOf[SendableObject[_]])))
//	val constructor = ru.typeOf[SendableObject[_]].member(ru.stringToTermName("<init>")).asTerm.alternatives(1).asMethod
//	val sendableObject = classMirror.reflectConstructor(constructor)("Hello, World!", (_str: String) => _str: Array[Byte]).asInstanceOf[SendableObject[_]]
	val asdf = SendableOld.construct(SendableOld.STRING, "Hello, World!")
	println(asdf)
//	val typeDict = HashMap(
//		"STRING" -> (typeTag[Sendable[String]],		(_str: String) 			 => _str: Array[Byte]),
//		"IMAGE"  -> (typeTag[Sendable[Image]],		(_img: Image) 			 => _img: Array[Byte]),
//		"FILE"   -> (typeTag[Sendable[Array[Byte]]],	(_fileData: Array[Byte]) => _fileData)
//	)
//	def metadataForType(_type: String) = (typeDict(_type)._1.tpe, typeDict(_type)._1.mirror, typeDict(_type)._2)
//
//	val (objType, mirror, objToBytes) = metadataForType("STRING")
//	mirror.reflectClass(mirror.classSymbol(mirror.runtimeClass(objType))).reflectConstructor(objType.member(stringToTermName("<init>")).asTerm.alternatives(0).asMethod)("Hello, World!", null)
//		match
//		{
//			case sendableWithString: Sendable[String] =>
//				println("Success!!")
//				println(sendableWithString.obj)
//			case lostSendable: Sendable[_] =>
//				println("Well... sorta.")
//				println(lostSendable.obj)
//			case _ => println("Failure. =[")
//		}

	System.exit(0)

//	<Dev tools>
	var useConfig_ = true
	var instance_ = 1

	val logFile = new File(s"log${instance}.log")
	if (!logFile.exists)
		logFile.createNewFile
	val logWriter_ = new FileWriter(logFile, true)
//	</Dev tools>

	tryDo(
	{
		new ServerSocket(serverPort).close
		log("Host instance initialized")
	}, _e =>
	{
		useConfig = false
		instance = 2
		log("Client instance initialized")
	})
	
	var serverPort_ = 19265
	var bufferSize_ = 8192
	var window_ : Window = null
	var desktopPanel_ : AnchorPane = null
	var desktop_ : FolderWidget = null
	var connManager: ConnectionManager = null

	val trayIcon = new TrayIcon(ImageIO.read(getClass.getResource("/nx/res/Stormworks_16.png")))
	val trayMenu = new PopupMenu
	val exitItem = new MenuItem("Exit")
	//TODO: Fix listener creation function
	exitItem.addActionListener(new ActionListener {def actionPerformed(e: java.awt.event.ActionEvent) = quit})
	trayMenu.add(exitItem)
	trayIcon.setPopupMenu(trayMenu)
	SystemTray.getSystemTray.add(trayIcon)
	
	new Main().launch
	
	def serialize = desktop.toJSON.condensed
	def saveState = if (useConfig) toFile("config.ini", serialize)
	def loadState(_json: JSON) = fx({
		log("Initializing application state")
		desktop = new FolderWidget(_json.get[String]("name"))
		val bg = _json.get[String]("background")
		if (bg != null)
			desktop.background = _json.get[String]("background").split(",").map(_.toByte): Image
		def addWidgets(_parent: FolderWidget, _widgets: ArrayBuffer[JSON]): Unit =
			for (i <- 0 until _widgets.length)
			{
				val widget = _widgets(i)
				if (!widget.has[String]("type"))
					log(s"type value not found in widget $i")
				else if (widget.get[String]("type").equals("FolderWidget"))
				{
					val fw = new FolderWidget(widget.get[String]("name"))
					_parent.addWidget(fw)
					if (widget.has[ArrayBuffer[JSON]]("widgets"))
						addWidgets(fw, widget.get[ArrayBuffer[JSON]]("widgets"))
				}
				else if (widget.get[String]("type").equals("FileWidget"))
				{
					val fw = new FileWidget(new File(widget.get[String]("file")))
					_parent.addWidget(fw)
				}
			}
		if (_json.has[ArrayBuffer[JSON]]("widgets"))
			addWidgets(desktop, _json.get[ArrayBuffer[JSON]]("widgets"))
	})

	def quit =
	{
		fx(window.hide)
		log("Saving application state")
		//TODO: Find string parse error in serialize
//		ex(Main.saveState)
		log("Application state saved")
		log("Stopping connection manager")
		connManager.stopAndWait
		log("Connection manager stopped")
		log("Exiting application", 2)
		logWriter.close
		SystemTray.getSystemTray.remove(Main.trayIcon)
	}
}

class Main extends Application with Util with InterfaceShortcuts
{
	def launch = javafx.application.Application.launch()

	var mainPanel: VBox = null

	def start(stg: Stage)
	{
		log("Application start")
		window = stg
		mainPanel = new VBox

		desktopPanel = new AnchorPane
		mainPanel.getChildren.add(desktopPanel)

		if (fileExists("config.ini") && useConfig)
			Main.loadState(JSON.parse(fileText("config.ini")))
		else
		{
			desktop = new FolderWidget("Desktop")
			desktop.background = "/nx/res/background.png"
		}

		val devToolbar = new HBox
		val newFolder = new Button("Add Folder")
		newFolder.setOnAction(handle[ActionEvent](desktop.addWidget(new FolderWidget)))
		val json = new Button("Desktop To JSON")
		json.setOnAction(handle[ActionEvent](log(desktop.toJSON + "\n\n")))
		val cnct = new Button("Connect to self")
		cnct.setOnAction(handle[ActionEvent](Main.connManager.connect("127.0.0.1", serverPort)))
		val test = new Button("Test")
		test.setOnAction(handle[ActionEvent](toFile("test.txt", "DESKTOP" + sep + Main.serialize + eom)))
		devToolbar.getChildren.addAll(newFolder, json, cnct, test)

		mainPanel.getChildren.add(devToolbar)

		addFilter[WindowEvent](stg, WindowEvent.WINDOW_HIDING, Main.quit)
		stg.setScene(new Scene(mainPanel))
		stg.show

		Main.connManager = new ConnectionManager
		Main.connManager.run
	}
}