package de.jlus.seriesdb.view

import com.sun.javafx.collections.ObservableListWrapper
import de.jlus.seriesdb.app.*
import de.jlus.seriesdb.viewmodel.DapiViewModel
import de.jlus.seriesdb.viewmodel.ProjectViewModel
import de.jlus.seriesdb.viewmodel.TmViewModel
import javafx.beans.property.*
import javafx.geometry.Side
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.paint.Paint
import javafx.stage.FileChooser
import javafx.stage.Modality
import tornadofx.*
import tornadofx.controlsfx.*
import java.io.File


/**
 * Generates the main window
 */
class MainView : View("Preliminary HERMESS SPU Interface software") {
    private val projectVm = ProjectViewModel()
    private val dapiVm = DapiViewModel()
    private val tmVm = TmViewModel()

    private val statusText = SimpleStringProperty("Not connected")
    private val progressValue = SimpleDoubleProperty(0.0)
    private val tabPane = TabPane()


    override val root = borderpane {
        top = menubar {
            menu("Project") {
                item("Open project").action(::openProject)
                item("Close project") {
                    action(projectVm::closeProject)
                }
                item("Reload project") {
                    enableWhen(projectVm.isOpened)
                    action(projectVm::reloadProject)
                }
                separator()
                item("Save project").action(::saveProject)
                item("Save As project") {
                    enableWhen(projectVm.isOpened)
                    action(::saveProjectAs)
                }
                separator()
                item("Application settings")
                separator()
                item("Exit application").action {
                    currentStage?.close()
                }
            }

            menu("SPU Interfacing") {
                menu("DAPI: Connect") {
                    enableWhen(dapiVm.itemProperty.isNull)
                    item("Reload ports")
                    separator()
                }
                item("DAPI: Disconnect") {
                    disableWhen(dapiVm.itemProperty.isNull)
                }
                item("DAPI: Configure") {
                    disableWhen(dapiVm.itemProperty.isNull)
                }
                item("DAPI: Readout") {
                    disableWhen(dapiVm.itemProperty.isNull)
                }
                separator()
                menu("TM: Connect") {
                    enableWhen(tmVm.itemProperty.isNull)
                    item("Reload ports")
                    separator()
                }
                item("TM: Disconnect") {
                    disableWhen(tmVm.itemProperty.isNull)
                }
                item("TM: Life view") {
                    disableWhen(tmVm.itemProperty.isNull)
                }
            }

            menu("About") {
                item("About this application") {
                    action { tabPane.mainTab(::WelcomeTab) }
                }
                separator()
                item("HERMESS Website") {
                    action { hostServices.showDocument("https://www.project-hermess.com") }
                }
                item("GitHub Repositories") {
                    action { hostServices.showDocument("https://github.com/HERMESSSignalProcessingSoftware") }
                }
                item("DAPI protocol specification") {
                    // TODO link to DAPI spec
                }
                item("TM protocol specification") {
                    // TODO link to TM spec
                }
                separator()
                item("V. $thisVersion") {
                    disableProperty().set(true)
                }
            }
        }

        center = splitpane {
            // after window was initialized, but the separator in a nice position
            subscribe<WindowFirstShow> {
                this@splitpane.setDividerPosition(0, 0.2)
            }

            // the left menu
            drawer {
                minWidth = 200.0
                dockingSide = Side.LEFT
                multiselect = true
                item("Project config", ImageView("imgs/icon-config-20.png"), true) {
                    form {
                        fieldset("Project configuration") {
                            field("Name:") {
                                label(projectVm.name)
                            }
                            field("Location:") {
                                val locationButton = button(graphic=ImageView(imgDirectory20)) {
                                    enableWhen(projectVm.isOpened)
                                }
                                val locationLabel = label("UNSAVED PROJECT") {
                                    hgrow = Priority.ALWAYS
                                }
                                projectVm.file.onChange {
                                    val loc = it?.parent
                                    locationLabel.text = loc ?: "UNSAVED PROJECT"
                                    if (loc == null)
                                        return@onChange
                                    locationButton.action {
                                        hostServices.showDocument(loc)
                                    }
                                }
                            }
                        }
                        buttonbar {
                            button("More")
                            button("Edit")
                        }
                    }
                }
                item("SPU configs", ImageView("imgs/icon-spu-20.png"), true) {
                    listview(tree.children) {
                        cellFormat {
                            textProperty().bind(it.descriptor)
                        }
                    }
                }
                item("Measurements", ImageView("imgs/icon-measurement-20.png"), true) {
                    listview(tree.children) {
                        cellFormat {
                            textProperty().bind(it.descriptor)
                        }
                    }
                }
            }

            // the main space with the TabPane
            add(tabPane.apply {
                minWidth = 500.0
                tabClosingPolicy = TabPane.TabClosingPolicy.ALL_TABS
                tabDragPolicy = TabPane.TabDragPolicy.REORDER
                mainTab(::WelcomeTab)
            })

        }

        bottom = statusbar(statusText, progressValue) {}
    }


    init {
        shortcut("Ctrl+O", ::openProject)
        shortcut("Ctrl+S", ::saveProject)
        shortcut("Ctrl+Shift+S", ::saveProjectAs)
    }


    /**
     * open a dialog for requesting the open project
     */
    private fun openProject () {
        val selection = chooseFile(
                "Select the project file to open",
                arrayOf(FileChooser.ExtensionFilter("HERMESS project file", "*.herpro")),
                File(System.getProperty("user.home")),
                FileChooserMode.Single,
                currentWindow
        )
        if (selection.size == 1)
            projectVm.openProject(selection[0])
    }


    /**
     * Save the project either in bg or request for a new path
     */
    private fun saveProject () {
        if (projectVm.isOpened.value)
            projectVm.saveProject()
        else
            saveProjectAs()
    }


    /**
     * Opens a dialog for a new project
     */
    private fun saveProjectAs () {
        dialog("New Project", Modality.APPLICATION_MODAL) {
            setPrefSize(500.0, 150.0)
            val name = SimpleStringProperty()
            val location = SimpleObjectProperty(File(System.getProperty("user.home")))
            gridpane {
                hgap = 12.0
                vgap = 12.0
                row {
                    label("A new directory containing the project files will be generated.") {
                        gridpaneConstraints {
                            columnSpan = 3
                        }
                    }
                }
                row {
                    label("New Project name: ")
                    textfield(name) {
                        gridpaneConstraints {
                            columnSpan = 2
                        }
                    }
                }
                row {
                    label("Location: ")
                    label(location.value.toString()) {
                        location.onChange {
                            text = it.toString()
                        }
                        gridpaneConstraints {
                            hGrow = Priority.ALWAYS
                        }
                    }
                    button(graphic = ImageView(imgDirectory20)).action {
                        location.value = chooseDirectory(
                                "Select parent directory for new project",
                                location.value
                        )
                    }
                }
                row {
                    button("Create project") {
                        isDefaultButton = true
                        action {
                            val finalName = name.value ?: ""
                            val finalLocation = location.value
                            if (!finalName.matches(regexProjectName))
                                error("Name must match $regexProjectName")
                            else if (finalLocation == null || !finalLocation.isDirectory || !finalLocation.exists())
                                error("Location must be set to a directory")
                            else {
                                projectVm.name.set(finalName)
                                projectVm.saveProjectAs(finalLocation.resolve("$finalName/$finalName.herpro"))
                                close()
                            }
                        }
                    }
                    button("Cancel").action(::close)
                }
            }
        }
    }
}


/**
 * Represents a single item in the TreeView on the left side
 * TODO remove all this shit
 */
class ProjectOverviewItem(descriptor: String) {
    val descriptor = SimpleStringProperty(descriptor)
    val children = ObservableListWrapper(mutableListOf<ProjectOverviewItem>())
    val fillColor = SimpleObjectProperty(Paint.valueOf("#000000"))

    /**
     * create ProjectOverviewItem and add to this children
     */
    fun item(descriptor: String, f: ProjectOverviewItem.() -> Unit = {}): ProjectOverviewItem {
        val item = ProjectOverviewItem(descriptor)
        item.f()
        children.add(item)
        return item
    }
}

val tree = ProjectOverviewItem("Project overview").apply {
    item("Project configuration") {
        item("Test")
        fillColor.set(Paint.valueOf("#006000"))
    }
    item("SPU configuration files")
    item("Measurement readout files")
}