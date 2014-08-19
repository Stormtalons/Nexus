package nx

import javafx.application.Platform
import javafx.event.{ActionEvent, EventHandler}
import javafx.scene.Scene
import javafx.scene.control.Button
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
	var window: Window = null
	var desktopPanel: AnchorPane = null
	var desktop: FolderWidget = null
	
	new Main().launch
	
	def serialize: String = desktop.toJSON.condensed

	def saveState =
	{

	}

	def loadState =
	{

	}

	def run(code: => Unit) = new Thread(new Runnable {def run = code}).start
	def fx(code: => Unit) = Platform.runLater(new Runnable {def run = code})
}

class Main extends javafx.application.Application
{
	def launch = javafx.application.Application.launch()
	def desktop = Main.desktop
	def desktop_=(_fw: FolderWidget) =
	{
		if (Main.desktop != null)
			Main.desktopPanel.getChildren.remove(Main.desktop)
		Main.desktop = _fw
		Main.desktopPanel.getChildren.add(0, Main.desktop)
	}

	var peerManager: PeerManager = null

	var mainPanel: VBox = null

	def start(stg: Stage)
	{
		Main.window = stg
		mainPanel = new VBox

		Main.desktopPanel = new AnchorPane
		mainPanel.getChildren.add(Main.desktopPanel)

		desktop = new FolderWidget("Desktop")
		AnchorPane.setLeftAnchor(desktop, 0d)
		AnchorPane.setRightAnchor(desktop, 0d)
		AnchorPane.setTopAnchor(desktop, 0d)
		AnchorPane.setBottomAnchor(desktop, 0d)
		desktop.confirmConstraintsFor(5, 10)
		desktop.expand(false)
		desktop.setStyle("-fx-background-image: url(\"/nx/res/background.png\")")
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