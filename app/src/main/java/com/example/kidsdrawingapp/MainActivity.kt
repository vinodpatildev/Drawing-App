package com.example.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var ibBrush: ImageButton? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    private var mLinearLayoutPaintColors: LinearLayout? = null
    private var ibGallary: ImageButton? = null
    private var ibUndo:ImageButton? = null
    private var ibRedo:ImageButton? = null
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
//            Toast.makeText(this,"changePaintColor clicked",Toast.LENGTH_LONG).show()

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
}