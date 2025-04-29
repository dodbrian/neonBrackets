package tech.zimin.neonbrackets

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer

/**
 * Project listener that registers the highlighting pass factory when a project is opened.
 */
class NeonBracketsInitializer : StartupActivity, DumbAware {
    private var editorFactoryListener: EditorFactoryListener? = null

    override fun runActivity(project: Project) {
        println("[NeonBrackets] Initializing NeonBrackets for project: ${project.name}")

        // Create a dedicated disposable for our plugin's resources
        val pluginDisposable = Disposer.newDisposable("NeonBracketsPluginDisposable")

        // Get a connection from the project's message bus
        val connection = project.messageBus.connect(pluginDisposable)

        // Listen for project closing events to clean up our resources
        connection.subscribe(ProjectManager.TOPIC, object : ProjectManagerListener {
            override fun projectClosing(closingProject: Project) {
                if (project == closingProject) {
                    println("[NeonBrackets] Project closing, disposing resources: ${project.name}")
                    Disposer.dispose(pluginDisposable)
                }
            }
        })

        // Register editor factory listener to catch new editors
        val listener = object : EditorFactoryListener {
            override fun editorCreated(event: EditorFactoryEvent) {
                val editor = event.editor
                if (editor.project == project) {
                    setupEditorListeners(editor)
                    highlightBracketsInEditor(editor)
                }
            }

            override fun editorReleased(event: EditorFactoryEvent) {
                val editor = event.editor
                clearHighlighters(editor)
                removeEditorListeners(editor)
            }
        }

        // Register the listener with EditorFactory using our disposable
        // This is the non-deprecated way to register the listener
        EditorFactory.getInstance().addEditorFactoryListener(listener, pluginDisposable)
        editorFactoryListener = listener

        // Process all open editors immediately
        ApplicationManager.getApplication().invokeLater {
            processAllOpenEditors(project)
        }
    }

    private fun processAllOpenEditors(project: Project) {
        // Get all open editors and apply highlighting
        val editors = EditorFactory.getInstance().allEditors
        println("[NeonBrackets] Processing ${editors.size} existing editors")

        for (editor in editors) {
            if (editor.project == project) {
                setupEditorListeners(editor)
                highlightBracketsInEditor(editor)
            }
        }
    }

    /**
     * Set up document and selection listeners for an editor.
     */
    private fun setupEditorListeners(editor: Editor) {
        // Add document listener if not already added
        if (editor.getUserData(DOCUMENT_LISTENER) == null) {
            val docListener = object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    highlightBracketsInEditor(editor)
                }
            }
            editor.document.addDocumentListener(docListener)
            editor.putUserData(DOCUMENT_LISTENER, docListener)
        }

        // Add selection listener if not already added
        if (editor.getUserData(SELECTION_LISTENER) == null) {
            val selectionListener = object : SelectionListener {
                override fun selectionChanged(event: SelectionEvent) {
                    highlightBracketsInEditor(editor)
                }
            }
            editor.selectionModel.addSelectionListener(selectionListener)
            editor.putUserData(SELECTION_LISTENER, selectionListener)
        }
    }

    /**
     * Remove document and selection listeners from an editor.
     */
    private fun removeEditorListeners(editor: Editor) {
        // Remove document listener
        val documentListener = editor.getUserData(DOCUMENT_LISTENER)
        if (documentListener != null) {
            editor.document.removeDocumentListener(documentListener)
            editor.putUserData(DOCUMENT_LISTENER, null)
        }

        // Remove selection listener
        val selectionListener = editor.getUserData(SELECTION_LISTENER)
        if (selectionListener != null) {
            editor.selectionModel.removeSelectionListener(selectionListener)
            editor.putUserData(SELECTION_LISTENER, null)
        }
    }


}