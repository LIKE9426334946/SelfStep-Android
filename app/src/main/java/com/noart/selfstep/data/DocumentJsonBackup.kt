package com.noart.selfstep.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import com.noart.selfstep.model.SelfStepData

enum class BackupOperation {
    IMPORT,
    EXPORT
}

class BackupStorageException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class DocumentJsonBackup(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun hasDirectoryAccess(): Boolean {
        val treeUri = configuredTreeUri() ?: return false
        return resolver.persistedUriPermissions.any { permission ->
            permission.uri == treeUri && permission.isReadPermission && permission.isWritePermission
        }
    }

    fun configureDirectory(treeUri: Uri) {
        selectedDirectoryKind(treeUri)

        try {
            resolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            preferences.edit().putString(KEY_TREE_URI, treeUri.toString()).apply()
        } catch (error: Exception) {
            throw BackupStorageException("无法获得 Documents 文件夹的访问权限，请重新选择。", error)
        }
    }

    fun export(data: SelfStepData) {
        try {
            val directory = backupDirectory(createIfMissing = true)
                ?: throw BackupStorageException("无法创建 Documents/SelfStep 文件夹。")
            val fileUri = findChild(directory, FILE_NAME)
                ?: DocumentsContract.createDocument(
                    resolver,
                    directory,
                    JSON_MIME_TYPE,
                    FILE_NAME
                )
                ?: throw BackupStorageException("无法创建 $FILE_NAME 文件。")

            resolver.openOutputStream(fileUri, "wt")?.bufferedWriter(Charsets.UTF_8).use { writer ->
                if (writer == null) throw BackupStorageException("无法写入 $FILE_NAME 文件。")
                writer.write(SelfStepJson.encode(data))
                writer.flush()
            }
        } catch (error: BackupStorageException) {
            throw error
        } catch (error: Exception) {
            throw BackupStorageException("导出失败，请检查 Documents 文件夹是否可以正常访问。", error)
        }
    }

    fun import(): SelfStepData {
        try {
            val directory = backupDirectory(createIfMissing = false)
                ?: throw BackupStorageException("没有找到 Documents/SelfStep 文件夹，请先导出一次。")
            val fileUri = findChild(directory, FILE_NAME)
                ?: throw BackupStorageException("没有找到 Documents/SelfStep/$FILE_NAME。")
            val json = resolver.openInputStream(fileUri)?.bufferedReader(Charsets.UTF_8).use { reader ->
                reader?.readText()
                    ?: throw BackupStorageException("无法读取 $FILE_NAME 文件。")
            }
            return SelfStepJson.decodeStrict(json)
        } catch (error: BackupStorageException) {
            throw error
        } catch (error: Exception) {
            throw BackupStorageException("导入失败，备份文件不是有效的 SelfStep JSON。", error)
        }
    }

    private fun backupDirectory(createIfMissing: Boolean): Uri? {
        val treeUri = configuredTreeUri()
            ?: throw BackupStorageException("请先授权访问手机的 Documents 文件夹。")
        if (!hasDirectoryAccess()) {
            throw BackupStorageException("Documents 文件夹授权已失效，请重新选择。")
        }

        val rootDocument = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        if (selectedDirectoryKind(treeUri) == SelectedDirectory.SELF_STEP) return rootDocument

        findChild(rootDocument, BACKUP_FOLDER_NAME)?.let { child ->
            if (documentMimeType(child) != DocumentsContract.Document.MIME_TYPE_DIR) {
                throw BackupStorageException("Documents 中已存在名为 SelfStep 的文件，无法创建备份文件夹。")
            }
            return child
        }

        if (!createIfMissing) return null
        return DocumentsContract.createDocument(
            resolver,
            rootDocument,
            DocumentsContract.Document.MIME_TYPE_DIR,
            BACKUP_FOLDER_NAME
        )
    }

    private fun findChild(parent: Uri, displayName: String): Uri? {
        val parentId = DocumentsContract.getDocumentId(parent)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parent, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )

        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == displayName) {
                    return DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(idIndex))
                }
            }
        }
        return null
    }

    private fun documentMimeType(documentUri: Uri): String? {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE)
        resolver.query(documentUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(
                    cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                )
            }
        }
        return null
    }

    private fun configuredTreeUri(): Uri? = preferences.getString(KEY_TREE_URI, null)?.let(Uri::parse)

    private fun selectedDirectoryKind(treeUri: Uri): SelectedDirectory {
        if (treeUri.authority != EXTERNAL_STORAGE_AUTHORITY || !DocumentsContract.isTreeUri(treeUri)) {
            throw BackupStorageException("请选择手机内部存储中的 Documents 文件夹。")
        }

        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
        val normalizedPath = documentId
            .substringAfter(':', documentId)
            .replace('\\', '/')
            .trimEnd('/')
        return when {
            normalizedPath.endsWith("/${Environment.DIRECTORY_DOCUMENTS}/$BACKUP_FOLDER_NAME") ||
                normalizedPath == "${Environment.DIRECTORY_DOCUMENTS}/$BACKUP_FOLDER_NAME" -> {
                SelectedDirectory.SELF_STEP
            }

            normalizedPath.endsWith("/${Environment.DIRECTORY_DOCUMENTS}") ||
                normalizedPath == Environment.DIRECTORY_DOCUMENTS -> {
                SelectedDirectory.DOCUMENTS
            }

            else -> throw BackupStorageException("请选择手机内部存储中的 Documents 文件夹。")
        }
    }

    private enum class SelectedDirectory {
        DOCUMENTS,
        SELF_STEP
    }

    companion object {
        const val BACKUP_FOLDER_NAME = "SelfStep"
        const val FILE_NAME = "selfstep_data.json"
        const val BACKUP_LOCATION = "Documents/$BACKUP_FOLDER_NAME/$FILE_NAME"

        private const val JSON_MIME_TYPE = "application/json"
        private const val PREFERENCES_NAME = "selfstep_backup"
        private const val KEY_TREE_URI = "documents_tree_uri"
        private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    }
}
