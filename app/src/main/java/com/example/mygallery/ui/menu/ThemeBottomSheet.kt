package com.example.mygallery.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.mygallery.R
import com.example.mygallery.databinding.BottomSheetThemeBinding
import com.example.mygallery.databinding.ItemThemeSwatchBinding
import com.example.mygallery.utils.ThemePreferences
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ThemeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetThemeBinding? = null
    private val binding get() = _binding!!

    // Keep references to the 8 inflated swatch cards so we can update
    // their border (selected state) without re-inflating everything.
    private val swatchCards =
        mutableMapOf<ThemePreferences.ThemeColorOption, ItemThemeSwatchBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetThemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUiModeRows()
        setupThemeColorSwatches()

        updateUiModeSelection(ThemePreferences.getUiMode(requireContext()))
    }

    // =========================================================
    // UI MODE
    // =========================================================

    private fun setupUiModeRows() {

        binding.layoutSystem.setOnClickListener {
            applyUiMode(ThemePreferences.UiMode.SYSTEM)
        }

        binding.layoutLight.setOnClickListener {
            applyUiMode(ThemePreferences.UiMode.LIGHT)
        }

        binding.layoutDark.setOnClickListener {
            applyUiMode(ThemePreferences.UiMode.DARK)
        }
    }

    private fun applyUiMode(mode: ThemePreferences.UiMode) {

        // setUiMode() internally calls AppCompatDelegate.setDefaultNightMode(),
        // which automatically recreates visible Activities to apply the
        // change — we don't need to call recreate() ourselves for this part.
        ThemePreferences.setUiMode(requireContext(), mode)

        updateUiModeSelection(mode)

        dismiss()
    }

    private fun updateUiModeSelection(mode: ThemePreferences.UiMode) {

        binding.imgSystemCheck.setImageResource(
            if (mode == ThemePreferences.UiMode.SYSTEM)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )

        binding.imgLightCheck.setImageResource(
            if (mode == ThemePreferences.UiMode.LIGHT)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )

        binding.imgDarkCheck.setImageResource(
            if (mode == ThemePreferences.UiMode.DARK)
                R.drawable.ic_check_circle_filled
            else
                R.drawable.ic_plane_circle
        )
    }

    // =========================================================
    // THEME COLOR
    // =========================================================

    private fun setupThemeColorSwatches() {

        val inflater = LayoutInflater.from(requireContext())
        val options = ThemePreferences.ThemeColorOption.entries

        // First 4 swatches in row 1, remaining 4 in row 2 — matches
        // the 4-per-row grid in the design.
        options.forEachIndexed { index, option ->

            val swatchBinding = ItemThemeSwatchBinding.inflate(
                inflater,
                if (index < 4) binding.rowSwatches1 else binding.rowSwatches2,
                false
            )

            swatchBinding.cardSwatch.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), option.swatchColorRes)
            )

            swatchBinding.cardSwatch.setOnClickListener {
                applyThemeColor(option)
            }

            swatchCards[option] = swatchBinding

            if (index < 4) {
                binding.rowSwatches1.addView(swatchBinding.root)
            } else {
                binding.rowSwatches2.addView(swatchBinding.root)
            }
        }

        updateThemeColorSelection(ThemePreferences.getThemeColor(requireContext()))
    }

    private fun applyThemeColor(option: ThemePreferences.ThemeColorOption) {

        ThemePreferences.setThemeColor(requireContext(), option)

        updateThemeColorSelection(option)

        // Unlike UI Mode, changing setTheme() has no automatic
        // recreation mechanism — we trigger it ourselves so the new
        // accent color actually applies immediately instead of only
        // on next app launch.
        dismiss()
        requireActivity().recreate()
    }

    private fun updateThemeColorSelection(selected: ThemePreferences.ThemeColorOption) {

        swatchCards.forEach { (option, swatchBinding) ->
            swatchBinding.cardSwatch.strokeWidth =
                if (option == selected) dpToPx(3) else 0
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}