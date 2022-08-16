package com.nekocwd.fmassstorage

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.nekocwd.fmassstorage.databinding.ItemDirectoryListBinding
import com.nekocwd.fmassstorage.databinding.ItemImageListBinding
import java.io.IOException


class Utils {
    class ImageListAdapter(var onSelected:(Image)->Unit, var onEdit:(Image)->Unit): RecyclerView.Adapter<ImageListAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
            val binding: ItemImageListBinding
            init {
                binding = ItemImageListBinding.bind(view)
            }
        }
        val dataset = ArrayList<Image>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_list, parent, false)
            return ViewHolder(view)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            dataset[position].let { img ->
                holder.binding.apply {
                    imageLabel.text = img.label
                    img.image?.let {
                        holder.binding.image.setImageDrawable(it)
                    }
                    imagePath.text = "${img.directory.label}/${img.path}"
                    top.setOnLongClickListener {
                        onEdit(img)
                        true
                    }
                    top.setOnClickListener{
                        onSelected(img)
                    }
                }
            }
        }

        override fun getItemCount(): Int = dataset.size
    }
    class DirectoryListAdapter(var onDeleteClicked: (Directory)->Unit, var onDirectoryStateChange:(Directory)->Unit): RecyclerView.Adapter<DirectoryListAdapter.ViewHolder>(){
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding: ItemDirectoryListBinding
            init{
                binding = ItemDirectoryListBinding.bind(itemView)
            }
        }

        val dataset = ArrayList<Directory>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_directory_list, parent, false)
            return ViewHolder(view)
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            dataset[position].let { dir ->
                holder.binding.apply {
                    dirLabelText.text = dir.label
                    dirPathText.text = dir.path
                    deleteButton.setOnClickListener{
                        onDeleteClicked(dir)
                        dataset.remove(dir)
                        notifyDataSetChanged()
                    }
                    enabled.isChecked = dir.isEnabled
                    enabled.setOnCheckedChangeListener { _, state ->
                        dir.isEnabled = state
                        onDirectoryStateChange(dir)
                    }
                }
            }
        }

        override fun getItemCount(): Int = dataset.size
    }

    data class Image(var directory: Directory, var path: String, var cdrom: Boolean = false,
                     var readOnly: Boolean = false, var label: String? = null,
                     var image: Drawable? = null){
        companion object{
            const val imageIconPrefs = "image"
            const val imageLabelPrefs = "label"
            const val imageCdromPrefs = "cdrom"
            const val imageReadOnlyPrefs = "readonly"

            fun getAllFromDirectory(directory: Directory, mContext: Context): ArrayList<Image>{
                val process = Runtime.getRuntime().exec(arrayOf("su","-c", "ls", directory.path))
                process.waitFor()
                var fileNames = String(process.inputStream.readBytes()).lines()
                while (fileNames.isNotEmpty() && fileNames.last().isBlank()){
                    fileNames = fileNames.dropLast(1)
                }
                val result = arrayListOf<Image>()
                for(filename in fileNames){
                    val image = Image(directory, filename)
                    image.getIcon(mContext)
                    image.getLabel(mContext)
                    image.getCdrom(mContext)
                    image.getReadOnly(mContext)
                    result.add(image)
                }
                return  result
            }
            fun findNowHosting(mContext: Context): Image?{
                findLunFile(mContext)?.let{ lun->
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat", lun))
                    process.waitFor()
                    val nowHostingPath = String(process.inputStream.readBytes()).split("/")
                    if(nowHostingPath.size < 2)
                        return null
                    val directories = Directory.getAll(mContext)
                    for(directory in directories){
                        if(nowHostingPath.contains(directory.path.split("/").last())){
                            val images = getAllFromDirectory(directory, mContext)
                            for(image in images){
                                if(nowHostingPath.last().trimEnd() == image.path){
                                    return image
                                }
                            }
                        }
                    }
                }
                return null
            }
        }
        fun commit(mContext: Context){
            val sp = PreferenceManager.getDefaultSharedPreferences(mContext)
            sp.edit()
                .putString("$path/$imageLabelPrefs", label)
                .putBoolean("$path/$imageCdromPrefs", cdrom)
                .putBoolean("$path/$imageReadOnlyPrefs", readOnly)
                .apply()
        }
        fun setIcon(mContext: Context, uri: Uri){
            val sp = PreferenceManager.getDefaultSharedPreferences(mContext)
            sp.edit().putString("$path/$imageIconPrefs",uri.toString()).apply()
        }
        private fun getLabel(mContext: Context){
            val sp = PreferenceManager.getDefaultSharedPreferences(mContext)
            sp.getString("$path/$imageLabelPrefs", null)?.let {
                label = it
            }
            if(label == null) {
                label = path
            }
        }
        @SuppressLint("UseCompatLoadingForDrawables")
        private fun getIcon(mContext: Context){
            val sp = PreferenceManager.getDefaultSharedPreferences(mContext)
            sp.getString("$path/$imageIconPrefs", null)?.toUri()?.let { uri->
                image = getDrawableFromUri(uri, mContext)
            }
            if(image == null) {
                val ty = TypedValue()
                mContext.theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, ty, true)
                val color = ty.data
                val drawable = ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_baseline_usb_24, mContext.theme)
                DrawableCompat.setTint(drawable!!, color)
                image = drawable
            }
        }
        private fun getCdrom(mContext: Context){
            val sp = PreferenceManager.getDefaultSharedPreferences(mContext)
            cdrom = sp.getBoolean("$path/$imageCdromPrefs", false)
        }
        private fun getReadOnly(mContext: Context){
            val sp = PreferenceManager.getDefaultSharedPreferences(mContext)
            readOnly = sp.getBoolean("$path/$imageReadOnlyPrefs", false)
        }
        fun move(mDirectory: Directory){
            if(Directory.checkDirectory(directory.path)){
                val fullPath = "${directory.path}/$path"
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "mv", fullPath, mDirectory.path))
                process.waitFor()
            }
        }
        fun host(mContext: Context): Boolean{
            val lun = findLunFile(mContext)!!
            val sp = PreferenceManager.getDefaultSharedPreferences(mContext)
            val debugEnabled = sp.getBoolean("debugEnabled", false)
            eject(mContext)
            val cdromPath = lun.substring(0, lun.length-5).plus("cdrom")
            Runtime.getRuntime().exec(arrayOf("su", "-c", "echo", if(cdrom) "1" else "0", ">", cdromPath)).waitFor()
            val readOnlyPath = lun.substring(0, lun.length-5).plus("ro")
            Runtime.getRuntime().exec(arrayOf("su", "-c", "echo", if(readOnly) "1" else "0", ">", readOnlyPath)).waitFor()
            var process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo", "${directory.path}/$path", ">", lun))
            process.waitFor()
            val exitValue = process.exitValue()
            process = Runtime.getRuntime().exec(arrayOf("su", "-c", "setprop", "sys.usb.config", if(debugEnabled)"mass_storage,adb" else "mass_storage"))
            process.waitFor()
            return  exitValue == 0
        }
    }
    data class Directory(var path: String, var label: String, var isEnabled: Boolean, var dirId: Int?){
        companion object{
            private const val directoryIdsPrefs = "dirIds"
            private const val directoryTitle = "dirTitle"
            private const val directoryPath = "dirPath"
            private const val directoryEnabled = "dirEnabled"
            private const val directoryId = "dirId"

            fun getAll(mContext: Context): ArrayList<Directory>{
                val dirs = arrayListOf<Directory>()
                val ids=mutableSetOf<String>()
                val sp = PreferenceManager.getDefaultSharedPreferences(mContext)
                ids.addAll(sp.getStringSet(directoryIdsPrefs, mutableSetOf())!!)
                for(i in ids){
                    val id = i.substring(directoryIdsPrefs.length-1).toInt()
                    val label = sp.getString(directoryTitle+id, "")
                    val path = sp.getString(directoryPath+id, "")
                    val enabled = sp.getBoolean(directoryEnabled+id, false)
                    dirs.add(Directory(path!!, label!!, enabled, id))
                }
                return dirs
            }
            fun fromDocumentFile(file: DocumentFile, mContext: Context): Directory?{
                try {
                    val dir = Directory("", file.name!!, true, null)
                    dir.commit(mContext)
                    val findMeFile = file.createFile("", "fmassstorageFindMePls.${dir.dirId}")
                    val process = Runtime.getRuntime().exec(arrayOf("su","-c","find","-L","/storage/","-name",findMeFile!!.name))
                    process.waitFor()
                    var path = String(process.inputStream.readBytes())
                    if(path.length>findMeFile.name!!.length){
                        path = path.substring(0, path.length-findMeFile.name!!.length-2)
                        dir.path = path
                        findMeFile.delete()
                        dir.commit(mContext)
                        return dir
                    }
                    else {
                        dir.remove(mContext)
                        findMeFile.delete()
                        Toast.makeText(mContext, R.string.cannot_find_directory_path_add_it_manualy, Toast.LENGTH_LONG).show()
                    }
                } catch (e: IOException){
                    Toast.makeText(mContext, R.string.you_need_install_busybox, Toast.LENGTH_SHORT).show()
                }
                return null

            }
            fun checkDirectory(path: String): Boolean{
                val process = Runtime.getRuntime().exec(arrayOf("su","-c","ls", path))
                process.waitFor()
                return process.exitValue()==0
            }
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            fun choseDirectory(): Intent {
                return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            }

        }
        fun commit(mContext: Context){
            val ids=mutableSetOf<String>()
            val sp = PreferenceManager.getDefaultSharedPreferences(mContext)
            ids.addAll(sp.getStringSet(directoryIdsPrefs, mutableSetOf<String>())!!)
            if(dirId == null) {
                dirId=0
                while (ids.contains(directoryId+dirId)){
                    dirId=dirId!!+1
                }
                ids.add(directoryId+dirId)
                sp.edit().putStringSet(directoryIdsPrefs, ids).apply()
            }
            sp.edit()
                .putString(directoryTitle+dirId, label)
                .putString(directoryPath+dirId, path)
                .putBoolean(directoryEnabled+dirId, isEnabled).apply()
        }
        fun remove(mContext: Context){
            val ids=mutableSetOf<String>()
            val sp = PreferenceManager.getDefaultSharedPreferences(mContext)
            ids.addAll(sp.getStringSet(directoryIdsPrefs, mutableSetOf())!!)
            ids.remove(directoryId+dirId)
            sp.edit().putStringSet(directoryIdsPrefs, ids)
                .remove(directoryTitle+dirId)
                .remove(directoryPath+dirId)
                .remove(directoryEnabled+dirId).apply()
        }
    }

    companion object{
        const val isRootedPrefs = "IsRooted"
        private var previousUsbState = ""
        fun checkForRoot(): Boolean {
            return try{
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "ls"))
                process.waitFor()
                process.exitValue() == 0
            } catch(e: IOException){
                false
            }
        }
        fun selectImage(): Intent{
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "image/*"
            return intent
        }
        fun getDrawableFromUri(uri: Uri, mContext: Context): Drawable{
            val cr = mContext.contentResolver
            var image = Drawable.createFromStream(cr.openInputStream(uri), "image")
            if(image == null){
                val svg = SVG.getFromInputStream(cr.openInputStream(uri))
                image = PictureDrawable(svg.renderToPicture())
            }
            return image
        }
        fun findLunFile(mContext: Context): String?{
            try{
                var process = Runtime.getRuntime().exec(arrayOf("su", "-c", "mount", "|grep", "configfs"))
                process.waitFor()
                val fileNames = String(process.inputStream.readBytes()).split(" ")
                return if(fileNames.size > 3){
                    process = Runtime.getRuntime().exec(arrayOf("su", "-c", "find", fileNames[2], "-type", "f", "-name", "file"))
                    process.waitFor()
                    val path = String(process.inputStream.readBytes())
                    if(path.length > 4)
                        path
                    else null
                } else{
                    process = Runtime.getRuntime().exec(arrayOf("find", "/sys", "-type", "d", "-name", "'f_mass_storage'"))
                    process.waitFor()
                    val path = String(process.inputStream.readBytes()) + "/lun/file"
                    if (path.length > 4)
                        path
                    else null
                }
            } catch (e: IOException){
                Toast.makeText(mContext, R.string.you_need_install_busybox, Toast.LENGTH_SHORT).show()
            }
            return null
        }
        fun eject(mContext: Context){
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "echo", "\"\"", ">", findLunFile(mContext)))
                    .waitFor()
                if (previousUsbState.isNotEmpty()) {
                    Runtime.getRuntime()
                        .exec(arrayOf("su", "-c", "setprop", "sys.usb.config", previousUsbState))
                        .waitFor()
                } else {
                    val process =
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "getprop", "sys.usb.config"))
                    process.waitFor()
                    previousUsbState = String(process.inputStream.readBytes())
                }
            } catch (e: IOException){
                Toast.makeText(mContext, R.string.you_need_install_busybox, Toast.LENGTH_SHORT).show()
            }
        }
    }
}