package com.example.application_gestion_rdv
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ImageViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        val imageUrl = intent.getStringExtra("image_url")
        val imageView = findViewById<ImageView>(R.id.photoView)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)

        // Charger l'image
        Glide.with(this)
            .load(imageUrl)
            .into(imageView)

        // Bouton fermer
        btnClose.setOnClickListener {
            finish()
        }

        // Fermer en cliquant sur l'image
        imageView.setOnClickListener {
            finish()
        }
    }
}