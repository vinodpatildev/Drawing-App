package com.example.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var ibBrush: ImageButton? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    private var mLinearLayoutPaintColors: LinearLayout? = null
    private var ibGallary: ImageButton? = null
    private var ibUndo:ImageButton? = null
    private var ibRedo:ImageButton? = null
    private var ibSave:ImageButton? = null
    private val activityResultLauncher : ActivityResultLauncher<Array<String>> = registerForActivityResult( ActivityResultContracts.RequestMultiplePermissions() ){
        permissions ->
        permissions.entries.forEach{
            val permissionName = it.key
            val isGranted = it.value

            if(isGranted){
//                Toast.makeText(this,"Permission Granted.", Toast.LENGTH_LONG).show()
                val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGallaryLauncher.launch(pickIntent)

            }else if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                Toast.makeText(this,"${permissionName} Permission Not Granted.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val openGallaryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult( ActivityResultContracts.StartActivityForResult()){
        result ->
        if(result.resultCode == RESULT_OK && result.data != null){
            val imageBackGround : ImageView = findViewById(R.id.iv_background)
            imageBackGround.setImageURI(result.data?.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        ibBrush = findViewById(R.id.ib_brush)
        mLinearLayoutPaintColors = findViewById(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = mLinearLayoutPaintColors!![2] as ImageButton
        ibGallary = findViewById(R.id.ib_gallary)
        ibUndo = findViewById(R.id.ib_undo)
        ibRedo = findViewById(R.id.ib_redo)
        ibSave = findViewById(R.id.ib_save)

        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        drawingView?.setSizeForBrush(10.toFloat())
        ibBrush?.setOnClickListener {
            showBrushSizeChooserDialog()
        }
        ibGallary?.setOnClickListener{
            requestStoragePermission()

        }
        ibUndo?.setOnClickListener{view->
            drawingView?.undo()
        }
        ibRedo?.setOnClickListener{view->
            drawingView?.redo()
        }
        ibSave?.setOnClickListener{view->
            if(isReadStorageAllowed()){
                lifecycleScope.launch{
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    saveBitmapFile( getBitmapFromView( flDrawingView ) )
                }
            }
        }

    }

    private fun isReadStorageAllowed():Boolean{
        val resultRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val resultWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        return resultRead == PackageManager.PERMISSION_GRANTED && resultWrite == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialog("Drawing App","Drawing App "+"Needs to Access Your External Storage to read files.")
        }else if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            showRationaleDialog("Drawing App","Drawing App "+"Needs to Access Your External Storage to save files.")
        }else{
            activityResultLauncher.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setCancelable(false)
        builder.setPositiveButton("OK"){alertDialogbox,_->
            alertDialogbox.dismiss()
        }
        builder.create().show()
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")

        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun changePaintColor(view: View) {
        if (view !== mImageButtonCurrentPaint) {

            val colorTag = (view as ImageButton).tag.toString()
            drawingView?.setColor(colorTag)

            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = view
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )
        }
    }

    private fun getBitmapFromView(view: View) : Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString() + File.separator + "DrawingApp_" + System.currentTimeMillis()/1000 + ".png")

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread{
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,"File saved successfully.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        if(result.isNotEmpty()){
            shareImage(result)
        }
        return result
    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path,uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share this image using...."))
        }
    }
}