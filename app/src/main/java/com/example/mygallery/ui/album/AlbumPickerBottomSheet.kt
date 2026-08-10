package com.example.mygallery.ui.album

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mygallery.R
import com.example.mygallery.adapter.AlbumPickerAdapter
import com.example.mygallery.databinding.BottomSheetAlbumPickerBinding
import com.example.mygallery.databinding.DialogCreateAlbumBinding
import com.example.mygallery.repository.GalleryRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * Lets the user pick a destination album for a Move or Copy action.
 * Reused for both — only the title/icon and the caller's follow-up
 * action differ.
 *
 * Usage from a Fragment:
 *   val sheet = AlbumPickerBottomSheet.newInstance(Mode.MOVE, excludedNames)
 *   sheet.onAlbumSelected = { destinationName -> ... }
 *   sheet.show(childFragmentManager, "AlbumPicker")
 */
class AlbumPickerBottomSheet : BottomSheetDialogFragment() {

    enum class Mode { MOVE, COPY }

    companion object {
        private const val ARG_MODE = "arg_mode"
        private const val ARG_EXCLUDED = "arg_excluded"

        fun newInstance(mode: Mode, excludedFolderNames: List<String>): AlbumPickerBottomSheet {
            return AlbumPickerBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode.name)
                    putStringArrayList(ARG_EXCLUDED, ArrayList(excludedFolderNames))
                }
            }
        }
    }

    // Set by the caller right after creating this fragment, before
    // calling show(). Since the sheet is always created and shown by
    // the same parent Fragment (never restored across process death),
    // a plain callback is simpler here than trying to route this
    // through Bundle arguments (which can't hold a lambda anyway).
    var onAlbumSelected: ((String) -> Unit)? = null

    private lateinit var binding: BottomSheetAlbumPickerBinding
    private val repository = GalleryRepository()

    private var mode: Mode = Mode.MOVE
    private var excludedNames: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = Mode.valueOf(arguments?.getString(ARG_MODE) ?: Mode.MOVE.name)
        excludedNames = arguments?.getStringArrayList(ARG_EXCLUDED) ?: emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetAlbumPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSheetTitle.text = if (mode == Mode.MOVE) "Move to Album" else "Copy to Album"
        binding.imgSheetIcon.setImageResource(
            if (mode == Mode.MOVE) R.drawable.ic_move else R.drawable.ic_copy
        )

        binding.recyclerAlbumPicker.layoutManager = GridLayoutManager(requireContext(), 3)

        binding.btnCloseSheet.setOnClickListener {
            dismiss()
        }

        binding.btnAddAlbum.setOnClickListener {
            showCreateAlbumDialog()
        }

        loadAlbums()
    }

    /**
     * Fetches the current album list directly from the Repository
     * (not from GalleryViewModel — this sheet doesn't need selection
     * mode, search, or any of that unrelated state) and excludes
     * whichever folder(s) triggered this Move/Copy, since moving or
     * copying an album into itself doesn't make sense.
     */
    private fun loadAlbums() {
        viewLifecycleOwner.lifecycleScope.launch {
            val realFolders = repository.getAllFolders(requireContext())
            val realNames = realFolders.map { it.folderName }.toSet()

            // Custom albums the user created via "+" that don't have any
            // real photos yet don't exist in MediaStore at all — add a
            // lightweight placeholder so they're immediately selectable
            // as a destination right after creating them.
            val customOnlyNames = com.example.mygallery.utils.CustomAlbumPreferences
                .getCustomAlbumNames(requireContext())
                .filter { it !in realNames }

            val placeholders = customOnlyNames.map { name ->
                com.example.mygallery.model.GalleryFolder(
                    folderName = name,
                    coverImage = android.net.Uri.EMPTY,
                    imageList = arrayListOf()
                )
            }

            val allFolders = realFolders + placeholders
            val filtered = allFolders.filter { it.folderName !in excludedNames }

            binding.recyclerAlbumPicker.adapter = AlbumPickerAdapter(filtered) { folder ->
                onAlbumSelected?.invoke(folder.folderName)
                dismiss()
            }
        }
    }

    private fun showCreateAlbumDialog() {

        val dialogBinding = DialogCreateAlbumBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnCreate.setOnClickListener {

            val albumName = dialogBinding.etAlbumName.text.toString().trim()
            val result = repository.createAlbum(requireContext(), albumName)

            if (result.isSuccess) {
                Toast.makeText(requireContext(), result.getOrNull(), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadAlbums() // refresh so the new album shows up immediately
            } else {
                dialogBinding.tilAlbumName.error = result.exceptionOrNull()?.message
            }
        }
    }
}