package com.anga9.seller.MVVM.ui.products.wizard

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.anga9.seller.R
import com.anga9.seller.MVVM.ui.products.AddProductWizardViewModel
import com.anga9.seller.MVVM.ui.products.WizardStep

import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import android.widget.Button

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ImageView
import coil.load

class Step1MediaFragment : Fragment(R.layout.fragment_wizard_step1_media), WizardStep {

    private lateinit var tvImageCount: TextView
    private lateinit var tvVideoCount: TextView
    private lateinit var rvImages: RecyclerView
    private lateinit var rvVideos: RecyclerView
    
    private val selectedImages = mutableListOf<Uri>()
    private val selectedVideos = mutableListOf<Uri>()
    
    private lateinit var imagesAdapter: MediaAdapter
    private lateinit var videosAdapter: MediaAdapter

    private val pickMultipleImages = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages.clear()
            selectedImages.addAll(uris)
            updateImageCount()
            imagesAdapter.notifyDataSetChanged()
        }
    }

    private val pickVideo = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(2)) { uris ->
        if (uris.isNotEmpty()) {
            selectedVideos.clear()
            selectedVideos.addAll(uris)
            updateVideoCount()
            videosAdapter.notifyDataSetChanged()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvImageCount = view.findViewById(R.id.tvImageCount)
        tvVideoCount = view.findViewById(R.id.tvVideoCount)
        rvImages = view.findViewById(R.id.rvImages)
        rvVideos = view.findViewById(R.id.rvVideos)
        
        imagesAdapter = MediaAdapter(selectedImages) { position ->
            selectedImages.removeAt(position)
            imagesAdapter.notifyItemRemoved(position)
            updateImageCount()
        }
        rvImages.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvImages.adapter = imagesAdapter

        videosAdapter = MediaAdapter(selectedVideos) { position ->
            selectedVideos.removeAt(position)
            videosAdapter.notifyItemRemoved(position)
            updateVideoCount()
        }
        rvVideos.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvVideos.adapter = videosAdapter

        view.findViewById<Button>(R.id.btnAddImages).setOnClickListener {
            pickMultipleImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        view.findViewById<Button>(R.id.btnAddVideos).setOnClickListener {
            pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }
        
        val tipText = view.findViewById<TextView>(R.id.tvTipText)
        tipText.text = "Add at least 3 photos to improve approval chances. Listings with 1 photo are rejected more often."
        
        updateImageCount()
        updateVideoCount()
    }

    private fun updateImageCount() {
        tvImageCount.text = "${selectedImages.size}/10 selected"
    }

    private fun updateVideoCount() {
        tvVideoCount.text = "${selectedVideos.size}/2 selected"
    }

    private inner class MediaAdapter(
        private val mediaList: List<Uri>,
        private val onRemoveClick: (Int) -> Unit
    ) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

        inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivPreview: ImageView = view.findViewById(R.id.ivPreview)
            val btnRemoveImage: ImageView = view.findViewById(R.id.btnRemoveImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_preview, parent, false)
            return MediaViewHolder(view)
        }

        override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
            holder.ivPreview.load(mediaList[position])
            holder.btnRemoveImage.setOnClickListener {
                onRemoveClick(holder.adapterPosition)
            }
        }

        override fun getItemCount(): Int = mediaList.size
    }

    override fun validate(): Boolean {
        return true
    }

    override fun saveDataToViewModel(viewModel: AddProductWizardViewModel) {
        viewModel.selectedImageUris = selectedImages
    }
}
