package nx

import java.awt.event.ActionListener
import java.awt.{MenuItem, PopupMenu, SystemTray, TrayIcon}
import java.io.{File, FileWriter}
import java.net._
import java.nio.channels.spi.SelectorProvider
import javafx.application.Application
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

		addFilter[WindowEvent](stg, WindowEvent.WINDOW_HIDING, Main.quit)
		stg.setScene(new Scene(mainPanel))
		stg.show

		Main.connManager = new ConnectionManager
		Main.connManager.run
	}
}