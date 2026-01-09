package com.gosash.winampbooster

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import androidx.fragment.app.Fragment

class ColorSettingsFragment : Fragment(R.layout.fragment_colors) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ThemePrefs.applyToView(view)

        fun bindRGB(rId:Int, gId:Int, bId:Int, initial:Int): Triple<SeekBar,SeekBar,SeekBar> {
            val r = view.findViewById<SeekBar>(rId)
            val g = view.findViewById<SeekBar>(gId)
            val b = view.findViewById<SeekBar>(bId)
            r.progress = Color.red(initial)
            g.progress = Color.green(initial)
            b.progress = Color.blue(initial)
            return Triple(r,g,b)
        }

        val bg = bindRGB(R.id.bgR, R.id.bgG, R.id.bgB, ThemePrefs.bg(requireContext()))
        val ac = bindRGB(R.id.acR, R.id.acG, R.id.acB, ThemePrefs.accent(requireContext()))
        val tx = bindRGB(R.id.txR, R.id.txG, R.id.txB, ThemePrefs.text(requireContext()))

        view.findViewById<Button>(R.id.btnApply).setOnClickListener {
            val bgC = Color.rgb(bg.first.progress, bg.second.progress, bg.third.progress)
            val acC = Color.rgb(ac.first.progress, ac.second.progress, ac.third.progress)
            val txC = Color.rgb(tx.first.progress, tx.second.progress, tx.third.progress)
            ThemePrefs.save(requireContext(), bgC, acC, txC)
            requireActivity().recreate()
        }
    }
}
