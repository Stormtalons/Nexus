package nx

import java.awt.event.ActionListener
import java.awt.{MenuItem, PopupMenu, SystemTray, TrayIcon}
import java.io.{File, FileWriter}
import java.net._
import java.nio.channels.{SelectionKey, Selector}
import javafx.application.{Platform, Application}
import javafx.event.ActionEvent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.layout.{AnchorPane, HBox, VBox}
import javafx.stage.{Stage, Window, WindowEvent}
import javax.imageio.ImageIO

import nx.comm.ConnectionManager
import nx.util.{JSON, InterfaceShortcuts, Tools}
import nx.widgets.{FileWidget, FolderWidget}

import scala.collection.mutable.ArrayBuffer

object Main extends App with Tools with InterfaceShortcuts
{
	var serverPort_ = 19265
	var bufferSize_ = 8192

//	<Dev tools>
	var useConfig_ = true
	var instance_ = 0

	var logFile: File = null
	var logWriter_ : FileWriter = null
	def setLogFile(_filePath: String) =
	{
		logFile = new File(_filePath)
		if (!logFile.exists)
			logFile.createNewFile
		logWriter_.close.^
		logWriter_ = new FileWriter(logFile, true)
	}

	{
		createServerChannel.bind(new InetSocketAddress("0.0.0.0", serverPort))
		instance = 1
		log("Host instance initialized")
	}.^(_e =>
	{
		useConfig = false
		instance = 2
		log("Client instance initialized")
	})

//	</Dev tools>

	var connManager: ConnectionManager = new ConnectionManager
	var window_ : Window = null
	var desktopPanel_ : AnchorPane = null
	var desktop_ : FolderWidget = null

	val trayIcon = new TrayIcon(ImageIO.read(getClass.getResource("/nx/res/Stormworks_16.png")))
	val trayMenu = new PopupMenu
	val exitItem = new MenuItem("Exit")
	exitItem.addActionListener(new ActionListener {def actionPerformed(e: java.awt.event.ActionEvent) = quit})
	trayMenu.add(exitItem)
	trayIcon.setPopupMenu(trayMenu)
	SystemTray.getSystemTray.add(trayIcon)

	new Main().launch

	def serialize = desktop.toJSON.condensed
	def saveState = if (useConfig) toFile("config.ini", serialize)
	def loadState(_json: JSON) =
	{
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
	}.fx

	def quit =
	{
		window.hide.fx
		SystemTray.getSystemTray.remove(trayIcon)
		//TODO: Find string parse error in serialize
//		ex(Main.saveState)
		log("Application state saved")
		connManager.stopAndWait
		log("Connection manager stopped")
		log("Exiting application", 2)
		logWriter.close
	}
}

class Main extends Application with Tools with InterfaceShortcuts
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

		addFilter(stg, WindowEvent.WINDOW_HIDING, Main.quit)
		stg.setScene(new Scene(mainPanel))
		stg.setTitle("Instance - " + instance)
		stg.show
	}
}